package com.furahitechstudio.ustadsample.callbacks;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothProfile;
import com.furahitechstudio.ustadsample.manager.BluetoothManagerShared;
import com.furahitechstudio.ustadsample.utils.BleAndroidUtils;
import com.furahitechstudio.ustadsample.utils.LogWrapper;
import java.io.ByteArrayOutputStream;

import static com.furahitechstudio.ustadsample.manager.BluetoothManagerShared.CLIENT_CONFIGURATION_DESCRIPTOR_UUID;
import static com.furahitechstudio.ustadsample.manager.BluetoothManagerShared.ENTRY_STATUS_REQUEST;
import static com.furahitechstudio.ustadsample.manager.BluetoothManagerShared.SERVICE_UUID;

public class GattServerCallback extends BluetoothGattServerCallback {


  private BluetoothManagerShared bluetoothManager;

  private boolean isWaitingForExtraPackets = false;

  private int contentLength = 0;

  private ByteArrayOutputStream outputStream = null;

  public GattServerCallback(BluetoothManagerShared bluetoothManager) {
    this.bluetoothManager = bluetoothManager;
  }

  @Override
  public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
    super.onConnectionStateChange(device, status, newState);

    LogWrapper.log(false, "onConnectionStateChange status:"+status+" state "+newState);
    if (newState == BluetoothProfile.STATE_CONNECTED) {
      bluetoothManager.addNetworkNode(BleAndroidUtils.getNetworkNode(device));
    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
      bluetoothManager.removeNetworkNode(BleAndroidUtils.getNetworkNode(device));
    }
  }

  @Override
  public void onCharacteristicReadRequest(BluetoothDevice device,
      int requestId,
      int offset,
      BluetoothGattCharacteristic characteristic) {
    super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

    LogWrapper.log(false,"onCharacteristicReadRequest for " + characteristic.getUuid().toString());

    if (BleAndroidUtils.requiresResponse(characteristic)) {
      bluetoothManager.acknowledgeRequest(BleAndroidUtils.getNetworkNode(device), requestId, BluetoothGatt.GATT_FAILURE, 0, null);
    }
  }

  @Override public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
      BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
      int offset, byte[] value) {
    super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
        responseNeeded, offset, value);

    LogWrapper.log(false, "onCharacteristicWriteRequest for "+characteristic.getUuid().toString());
    if (SERVICE_UUID.equals(characteristic.getUuid())) {
      bluetoothManager.acknowledgeRequest(BleAndroidUtils.getNetworkNode(device), requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
      processPackets(value);
    }
  }


  private void processPackets(byte [] value){
    byte clientRequest = BleAndroidUtils.getRequestType(value);
    byte [] actualPayLoad = null;
    if(ENTRY_STATUS_REQUEST == clientRequest){
      LogWrapper.log(false, "Requesting course status");
      outputStream = new ByteArrayOutputStream();
      isWaitingForExtraPackets = true;
      contentLength = BleAndroidUtils.getContentLength(value);
    }


    if(isWaitingForExtraPackets && outputStream!=null){
      outputStream.write(value, 0, value.length);
      actualPayLoad = BleAndroidUtils.getActualPayLoad(outputStream.toByteArray());
    }

    if(outputStream !=null){
      LogWrapper.log(false, "Waiting "+ isWaitingForExtraPackets +" Request "+clientRequest
          +" Length "+contentLength+" Payload: "+actualPayLoad.length);

      if(contentLength == actualPayLoad.length){
        LogWrapper.log(false,"Transfer completed\n"
            +BleAndroidUtils.decompress(actualPayLoad));
      }
    }


  }


  @Override
  public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
      int offset, BluetoothGattDescriptor descriptor) {
    super.onDescriptorReadRequest(device, requestId, offset, descriptor);
    LogWrapper.log(false,"onDescriptorReadRequest for " + descriptor.getUuid().toString());
  }

  @Override public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
      BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset,
      byte[] value) {
    super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
        offset, value);
    LogWrapper.log(false, "onDescriptorWriteRequest for "+descriptor.getUuid().toString());

    if (CLIENT_CONFIGURATION_DESCRIPTOR_UUID.equals(descriptor.getUuid())) {
      bluetoothManager.addNodeConfiguration(BleAndroidUtils.getNetworkNode(device), value);
      bluetoothManager.acknowledgeRequest(BleAndroidUtils.getNetworkNode(device), requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
    }
  }

  @Override
  public void onNotificationSent(BluetoothDevice device, int status) {
    super.onNotificationSent(device, status);
    LogWrapper.log(false,"onNotificationSent : Response sent");
  }
}
