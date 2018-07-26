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
import com.furahitechstudio.ustadsample.callbacks.GattClientCallback;
import com.furahitechstudio.ustadsample.callbacks.GattServerCallback;
import com.furahitechstudio.ustadsample.models.NetworkNode;
import com.furahitechstudio.ustadsample.utils.BleAndroidUtils;
import com.furahitechstudio.ustadsample.utils.LogWrapper;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static android.content.Context.BLUETOOTH_SERVICE;
import static com.furahitechstudio.ustadsample.utils.BleAndroidUtils.decompress;
import static com.furahitechstudio.ustadsample.utils.BleAndroidUtils.isValidRequest;

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

  private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();


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

  @Override public void sendRequestToSuperNode(String requestContent, byte requestType) {
    setDataToTransfer(requestContent);
    setCurrentRequestType(requestType);
    BluetoothGattCharacteristic characteristic = BleAndroidUtils.findCourseServiceCharacteristics(mGatt);
    if (characteristic == null) {
      LogWrapper.log(true, "Failed to find characteristics");
      return;
    }

    if (requestContent.length() == 0) {
      LogWrapper.log(true,"Failed to read bytes");
      return;
    }

    Integer payloadSize = BleAndroidUtils.depacketizePayload(getPayload(requestType)).length;
    characteristic.setValue(payloadSize.toString().getBytes());
    boolean success = mGatt.writeCharacteristic(characteristic);
    boolean execute = mGatt.executeReliableWrite();
    if (success && execute) {
      LogWrapper.log(false, "Request sent successfully");
    } else {
      LogWrapper.log(true, "Request failed");
    }
  }

  @Override
  public void sendResponseToClient(NetworkNode networkNode,String responseContent,byte requestType) {
    setDataToTransfer(responseContent);
    setCurrentRequestType(requestType);
    BluetoothGattService service = mGattServer.getService(SERVICE_UUID);
    BluetoothGattCharacteristic characteristic = service.getCharacteristic(SERVICE_UUID);
    int payloadLength = getPayload(requestType).length;
    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(networkNode.getBluetoothAddress());
    boolean isConfirmationRequired = BleAndroidUtils.requiresConfirmation(characteristic);

    for(int packetIteration = 0; packetIteration < payloadLength; packetIteration++){
      characteristic.setValue(getPayload(requestType)[packetIteration]);
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

    if(isValidRequest(clientRequest) && outputStream.size() <= 0){
      LogWrapper.log(false, "New Request received");
      isWaitingForExtraPackets = true;
      currentRequestType = clientRequest;
      contentLength = BleAndroidUtils.getContentLength(value);
    }


    if(isWaitingForExtraPackets && outputStream!=null){
      outputStream.write(value, 0, value.length);
      actualPayLoad = BleAndroidUtils.getActualPayLoad(outputStream.toByteArray());
    }


    if(outputStream !=null && actualPayLoad != null){

      if(contentLength == actualPayLoad.length){
        switch (currentRequestType){
          case ENTRY_STATUS_REQUEST:
            //Request received on server device from client requesting entry status
            String [] entryIds = BleAndroidUtils.decompress(actualPayLoad).split(",");
            StringBuilder stringBuilder = new StringBuilder();
            for (String entryId : entryIds) {
              stringBuilder.append("T");
            }
            isWaitingForExtraPackets = false;
            LogWrapper.log(false,"Sending entry statuses: "+stringBuilder.toString());
            sendResponseToClient(networkNode,stringBuilder.toString(),ENTRY_STATUS_RESPONSE);
            break;

          case ENTRY_STATUS_RESPONSE:
            //Reply from the server device on entry status request
            LogWrapper.log(false,"Entry statues response received: "+decompress(actualPayLoad));
            String acquireCommand = "AcquireAllIhave";
            isWaitingForExtraPackets = false;
            LogWrapper.log(false,"Sending entry acquisition request: "+acquireCommand);
            sendRequestToSuperNode(acquireCommand, ENTRY_ACQUISITION_REQUEST);
            break;

          case ENTRY_ACQUISITION_REQUEST:
            //Request from client device to get WiFi groupID and passphrase
            LogWrapper.log(false,"Entry acquisition request received: "+decompress(actualPayLoad));
            String groupID = "UstadWIFiName";
            String groupPassphrase = "87@-09379?Password";
            isWaitingForExtraPackets = false;
            String response = groupID+","+groupPassphrase;
            LogWrapper.log(false, "Sending network credentials: "+response);
            sendResponseToClient(networkNode,response,ENTRY_ACQUISITION_RESPONSE);
            break;

          case ENTRY_ACQUISITION_RESPONSE:
            //Reply from server device on acquisition request
            LogWrapper.log(false,"Network credential received: "+decompress(actualPayLoad));
            isWaitingForExtraPackets = false;
            disconnectServer();
            break;
        }
        outputStream.reset();

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
