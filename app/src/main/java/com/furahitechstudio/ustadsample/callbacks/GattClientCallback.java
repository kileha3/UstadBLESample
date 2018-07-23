package com.furahitechstudio.ustadsample.callbacks;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import com.furahitechstudio.ustadsample.manager.BluetoothManagerShared;
import com.furahitechstudio.ustadsample.utils.BleAndroidUtils;
import com.furahitechstudio.ustadsample.utils.LogWrapper;
import java.util.List;

import static com.furahitechstudio.ustadsample.manager.BluetoothManagerShared.DATA_SEGMENT_END;
import static com.furahitechstudio.ustadsample.manager.BluetoothManagerShared.DATA_SEGMENT_START;

public class GattClientCallback extends BluetoothGattCallback {

  private BluetoothManagerShared bluetoothManager;

  private int packetIteration = 0;

  private boolean isWaitingForPackets = false;

  @Override
  public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
    super.onMtuChanged(gatt, mtu, status);
  }

  public GattClientCallback(BluetoothManagerShared bluetoothManager) {
    this.bluetoothManager = bluetoothManager;
  }

  @Override
  public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
    super.onConnectionStateChange(gatt, status, newState);
    LogWrapper.log(false,"onConnectionStateChange state: " + newState);

    if (status == BluetoothGatt.GATT_FAILURE) {
      LogWrapper.log(true,"Failed with status " + status);
      bluetoothManager.disconnectServer();
      return;
    } else if (status != BluetoothGatt.GATT_SUCCESS) {
      // handle anything not SUCCESS as failure
      LogWrapper.log(true,"Connection failed with status " + status);
      bluetoothManager.disconnectServer();
      return;
    }
    if (newState == BluetoothProfile.STATE_CONNECTED) {
      LogWrapper.log(false,"Connected to device " + gatt.getDevice().getAddress());
      bluetoothManager.addNetworkNode(BleAndroidUtils.getNetworkNode(gatt.getDevice()));
      gatt.discoverServices();
    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
      bluetoothManager.removeNetworkNode(BleAndroidUtils.getNetworkNode(gatt.getDevice()));
      LogWrapper.log(false,"Disconnected from server device");
      bluetoothManager.disconnectServer();
    }
  }

  @Override
  public void onServicesDiscovered(BluetoothGatt gatt, int status) {
    super.onServicesDiscovered(gatt, status);

    if (status != BluetoothGatt.GATT_SUCCESS) {
      LogWrapper.log(false,"Device service discovered " + status);
      return;
    }

    List<BluetoothGattCharacteristic> matchingCharacteristics = BleAndroidUtils.findCharacteristics(gatt);
    if (matchingCharacteristics.isEmpty()) {
      LogWrapper.log(true,"Unable to find characteristics.");
      return;
    }

    LogWrapper.log(false,"Initializing characteristics write");
    for (BluetoothGattCharacteristic characteristic : matchingCharacteristics) {
      characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
      enableAcknowledgment(gatt, characteristic);
    }
  }

  @Override
  public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    super.onCharacteristicWrite(gatt, characteristic, status);
    if (status == BluetoothGatt.GATT_SUCCESS) {

      if(packetIteration < bluetoothManager.getPacketSize()){
        characteristic.setValue(bluetoothManager.getBytesToSend()[packetIteration]);
        gatt.writeCharacteristic(characteristic);
        packetIteration++;
      }
      LogWrapper.log(false,"Characteristic written successfully");
    } else {
      LogWrapper.log(true,"Characteristic write not written, status: " + status);
      bluetoothManager.disconnectServer();
    }
  }

  @Override
  public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
    super.onCharacteristicRead(gatt, characteristic, status);
    if (status == BluetoothGatt.GATT_SUCCESS) {
      LogWrapper.log(true,"Characteristic read successfully");
      readCharacteristic(characteristic);
    } else {
      LogWrapper.log(true,"Characteristic read unsuccessful, status: " + status);
    }
  }

  @Override
  public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    super.onCharacteristicChanged(gatt, characteristic);
    LogWrapper.log(false,"Characteristic modified, " + characteristic.getUuid().toString());
    readCharacteristic(characteristic);
  }


  @Override
  public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
    if (status == BluetoothGatt.GATT_SUCCESS) {
      LogWrapper.log(false,"Descriptor written : " + descriptor.getUuid().toString());
    } else {
      LogWrapper.log(true,"Descriptor writing failed : " + descriptor.getUuid().toString());
    }
  }


  private void enableAcknowledgment(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    boolean isSetSuccessfully = gatt.setCharacteristicNotification(characteristic, true);
    if (isSetSuccessfully) {
      LogWrapper.log(false,"Signaling set successfully for " + characteristic.getUuid().toString());
      if (BleAndroidUtils.isCourseCheckCharacteristics(characteristic)) {
        LogWrapper.log(false, "Device is ready for communication");
      }
    } else {
      LogWrapper.log(true,"Failed to set response for " + characteristic.getUuid().toString());
    }
  }


  private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
    byte[] receivedSegmentBytes = characteristic.getValue();
    String receivedSegment = BleAndroidUtils.stringFromBytes(receivedSegmentBytes);
    if (receivedSegment == null) {
      LogWrapper.log (true,"Unable to read bytes from string");
      return;
    }

    if(receivedSegment.contains(DATA_SEGMENT_START)){
      isWaitingForPackets = true;
    }

    if(isWaitingForPackets){
      bluetoothManager.storeReceivedSegment(receivedSegment);
    }else{
      LogWrapper.log (false,"Received data: "+receivedSegment);
    }

    if(receivedSegment.contains(DATA_SEGMENT_END) && isWaitingForPackets){
      bluetoothManager.sendCourseStatuses(bluetoothManager.getCombinedReceivedSegments());
      LogWrapper.log(false, "Received Response:"+bluetoothManager.getCombinedReceivedSegments());
      isWaitingForPackets = false;
    }
  }
}
