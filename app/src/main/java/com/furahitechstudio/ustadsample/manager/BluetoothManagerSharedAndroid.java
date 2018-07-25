package com.furahitechstudio.ustadsample.manager;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import com.furahitechstudio.ustadsample.callbacks.GattClientCallback;
import com.furahitechstudio.ustadsample.callbacks.GattServerCallback;
import com.furahitechstudio.ustadsample.models.NetworkNode;
import com.furahitechstudio.ustadsample.utils.BleAndroidUtils;
import com.furahitechstudio.ustadsample.utils.LogWrapper;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import static android.content.Context.BLUETOOTH_SERVICE;
public class BluetoothManagerSharedAndroid extends BluetoothManagerShared implements BluetoothAdapter.LeScanCallback{


  private Activity mActivity;


  private BluetoothManager mBluetoothManager;

  private BluetoothAdapter mBluetoothAdapter;

  private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

  private BluetoothGattServer mGattServer;

  private BluetoothGatt mGatt;

  private GattServerCallback mGattServerCallback;

  private GattClientCallback mGattClientCallback;

  private boolean isWaitingForExtraPackets = false;

  private int contentLength = 0;

  private byte currentRequestType;

  private ByteArrayOutputStream outputStream = null;


  public BluetoothManagerSharedAndroid(Activity mActivity){
    this.mActivity = mActivity;
    this.mBluetoothManager = (BluetoothManager) mActivity.getSystemService(BLUETOOTH_SERVICE);
    this.mBluetoothAdapter = mBluetoothManager.getAdapter();
    this.mGattServerCallback = new GattServerCallback(this);
    this.mGattClientCallback = new GattClientCallback(this);
  }


  @Override public boolean isBleCapable() {
    return mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
  }

  @Override public boolean isBluetoothEnabled() {
    return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
  }


  @Override public boolean canDeviceAdvertise() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
        && mBluetoothAdapter.isMultipleAdvertisementSupported();
  }


  @Override public void startAdvertising() {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
      mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
      mGattServer = mBluetoothManager.openGattServer(mActivity, mGattServerCallback);

      BluetoothGattService service = new BluetoothGattService(SERVICE_UUID,
          BluetoothGattService.SERVICE_TYPE_PRIMARY);
      BluetoothGattCharacteristic writeCharacteristic = new BluetoothGattCharacteristic(
          SERVICE_UUID,
          BluetoothGattCharacteristic.PROPERTY_WRITE,
          BluetoothGattCharacteristic.PERMISSION_WRITE);


      BluetoothGattDescriptor clientConfigurationDescriptor = new BluetoothGattDescriptor(
          CLIENT_CONFIGURATION_DESCRIPTOR_UUID,
          BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
      clientConfigurationDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

      service.addCharacteristic(writeCharacteristic);

      mGattServer.addService(service);

      if (mBluetoothLeAdvertiser == null) {
        return;
      }

      AdvertiseSettings settings = new AdvertiseSettings.Builder()
          .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
          .setConnectable(true)
          .setTimeout(0)
          .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
          .build();

      ParcelUuid parcelUuid = new ParcelUuid(SERVICE_UUID);
      AdvertiseData data = new AdvertiseData.Builder()
          .addServiceUuid(parcelUuid)
          .build();

      mBluetoothLeAdvertiser.startAdvertising(settings, data, new AdvertiseCallback() {
        @Override public void onStartSuccess(AdvertiseSettings settingsInEffect) {
          super.onStartSuccess(settingsInEffect);
          LogWrapper.log(false, "Service advertised successfully");
        }

        @Override public void onStartFailure(int errorCode) {
          super.onStartFailure(errorCode);
          LogWrapper.log(true,"Service advertisement failed with errorCode:"+errorCode);
        }
      });
    }else{
      LogWrapper.log(true,"Device can't advertise it's service");
    }

  }

  @Override public void startScanning() {
    mBluetoothAdapter.startLeScan(new UUID[] { SERVICE_UUID},this);
  }

  @Override public void connectToServerDevice(NetworkNode networkNode) {
    LogWrapper.log(false,"Connecting to " + networkNode.getBluetoothAddress());
    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(networkNode.getBluetoothAddress());
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
      mGatt = device.connectGatt(mActivity, false, mGattClientCallback,BluetoothDevice.TRANSPORT_LE);
    }else{
      mGatt = device.connectGatt(mActivity, false, mGattClientCallback);
    }
  }

  @Override public void requestCourseStatuses(String stringToSend) {
    setDataToTransfer(stringToSend);
    BluetoothGattCharacteristic characteristic = BleAndroidUtils.findCourseServiceCharacteristics(mGatt);
    if (characteristic == null) {
      LogWrapper.log(true, "Failed to find characteristics");
      return;
    }

    if (stringToSend.length() == 0) {
      LogWrapper.log(true,"Failed to read bytes");
      return;
    }

    Integer payloadSize = BleAndroidUtils.depacketizePayload(getPayload(ENTRY_STATUS_REQUEST)).length;
    characteristic.setValue(payloadSize.toString().getBytes());
    boolean success = mGatt.writeCharacteristic(characteristic);
    boolean execute = mGatt.executeReliableWrite();
    if (success && execute) {
      LogWrapper.log(false, "Course requested successfully");
    } else {
      LogWrapper.log(true, "Failed to request course status");
    }
  }

  @Override
  public void sendCourseStatuses(NetworkNode networkNode,String courseResult) {
    setDataToTransfer(courseResult);
    BluetoothGattService service = mGattServer.getService(SERVICE_UUID);
    BluetoothGattCharacteristic characteristic = service.getCharacteristic(SERVICE_UUID);
    int payloadLength = getPayload(ENTRY_STATUS_RESPONSE).length;
    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(networkNode.getBluetoothAddress());
    boolean isConfirmationRequired = BleAndroidUtils.requiresConfirmation(characteristic);

    for(int packetIteration = 0; packetIteration < payloadLength; packetIteration++){
      characteristic.setValue(getPayload(ENTRY_STATUS_RESPONSE)[packetIteration]);
      if (isResponseEnabled(device, characteristic)) {
        mGattServer.notifyCharacteristicChanged(device, characteristic, isConfirmationRequired);
      }
    }

  }



  @Override
  public void acknowledgeRequest(NetworkNode networkNode, int requestId, int status, int offset,
      byte[] value) {
    mGattServer.sendResponse(mBluetoothAdapter.getRemoteDevice(networkNode.getBluetoothAddress()),
        requestId, status, 0, null);
  }

  @Override public void processPackets(NetworkNode networkNode, byte[] value) {
    byte clientRequest = BleAndroidUtils.getRequestType(value);
    byte [] actualPayLoad = null;
    if(ENTRY_STATUS_REQUEST == clientRequest || ENTRY_STATUS_RESPONSE == clientRequest){
      LogWrapper.log(false, "Requesting received");
      outputStream = new ByteArrayOutputStream();
      isWaitingForExtraPackets = true;
      currentRequestType = clientRequest;
      contentLength = BleAndroidUtils.getContentLength(value);
    }


    if(isWaitingForExtraPackets && outputStream!=null){
      outputStream.write(value, 0, value.length);
      actualPayLoad = BleAndroidUtils.getActualPayLoad(outputStream.toByteArray());
      LogWrapper.log(false, "Content-Length: "+contentLength+" Payload-Length: "+actualPayLoad.length);
    }


    if(outputStream !=null && actualPayLoad != null){

      if(contentLength == actualPayLoad.length){
        switch (currentRequestType){
          case ENTRY_STATUS_REQUEST:
            LogWrapper.log(false,"Entry status request completed:\n"+BleAndroidUtils.decompress(actualPayLoad));
            String [] entryIds = BleAndroidUtils.decompress(actualPayLoad).split(",");
            StringBuilder stringBuilder = new StringBuilder();
            for (String entryId : entryIds) {
              stringBuilder.append("T");
            }
            sendCourseStatuses(networkNode,stringBuilder.toString());
            break;

          case ENTRY_STATUS_RESPONSE:
            LogWrapper.log(false,"Entry status response received\n"+BleAndroidUtils.decompress(actualPayLoad));
            break;
        }

      }
    }

  }

  @Override public void disconnectServer() {

  }

  private boolean isResponseEnabled(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
    List<BluetoothGattDescriptor> descriptorList = characteristic.getDescriptors();
    BluetoothGattDescriptor descriptor = BleAndroidUtils.findClientConfigurationDescriptor(descriptorList);
    if (descriptor == null) {
      return true;
    }
    String deviceAddress = device.getAddress();
    byte[] clientConfiguration = getNodeConfigurations().get(deviceAddress);
    if (clientConfiguration == null) {
      return false;
    }

    byte[] responseEnabled = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
    return clientConfiguration.length == responseEnabled.length
        && (clientConfiguration[0] & responseEnabled[0]) == responseEnabled[0]
        && (clientConfiguration[1] & responseEnabled[1]) == responseEnabled[1];
  }

  @Override public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
    NetworkNode networkNode = BleAndroidUtils.getNetworkNode(device);
    if(!getAvailableNodes().contains(networkNode)){
      LogWrapper.log(false, "Device found: "+networkNode.getBluetoothAddress());
      getAvailableNodes().add(networkNode);
    }

  }

  @Override public void stopScanning() {
    if(mBluetoothAdapter == null) return;
    mBluetoothAdapter.stopLeScan(this);
    if(mGatt == null) return;
    mGatt.disconnect();
    mGatt.close();
  }


  @Override public void stopAdvertising() {

    if(mBluetoothLeAdvertiser == null) return;
    if (mGattServer != null) {
      mGattServer.clearServices();
      mGattServer.close();
    }
  }


  @Override public void stopService() {
    stopAdvertising();
    stopScanning();
  }

  @Override public void destroy() {
    if(mGattServer != null || mGatt != null){
      mGattServer = null;
      mGatt = null;
      mGattClientCallback = null;
      mGattClientCallback = null;
      mBluetoothManager = null;
      getConnectedNodes().clear();
      getAvailableNodes().clear();

    }
  }


}
