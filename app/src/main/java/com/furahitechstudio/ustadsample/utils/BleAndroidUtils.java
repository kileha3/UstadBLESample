package com.furahitechstudio.ustadsample.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.Nullable;
import com.furahitechstudio.ustadsample.models.NetworkNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.furahitechstudio.ustadsample.manager.BluetoothManagerShared.CLIENT_CONFIGURATION_DESCRIPTOR_SHORT_ID;
import static com.furahitechstudio.ustadsample.manager.BluetoothManagerShared.SERVICE_STRING;

public class BleAndroidUtils {


  public static byte[] compress(String string){
    ByteArrayOutputStream os = new ByteArrayOutputStream(string.length());
    byte[] compressed = new byte[]{};
    try{
      GZIPOutputStream gos = new GZIPOutputStream(os);
      gos.write(string.getBytes());
      gos.close();
      compressed = os.toByteArray();
      os.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return compressed;
  }



  public static String decompress(byte[] compressed){
    final int BUFFER_SIZE = 32;
    StringBuilder string = new StringBuilder();
    ByteArrayInputStream is = new ByteArrayInputStream(compressed);
    try{
      GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
      byte[] data = new byte[BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = gis.read(data)) != -1) {
        string.append(new String(data, 0, bytesRead));
      }
      gis.close();
      is.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return string.toString();
  }

  public static byte[][] packetizePayload(byte requestType, byte[] payload, int mtu){
    int packetSize = (int) Math.ceil(payload.length / (double) mtu);
    ByteBuffer headerBuffer = ByteBuffer.allocate(5);
    byte[] header = headerBuffer.put(requestType).putInt(payload.length).array();
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      outputStream.write(header);
      outputStream.write(payload);
    } catch (IOException e) {
      e.printStackTrace();
      return new byte[][]{};
    }
    byte [] totalPayLoad = outputStream.toByteArray();
    byte[][] packets = new byte[packetSize][mtu];
    int start = 0;
    for(int position = 0; position < packets.length; position++) {
      int end = start + mtu;
      if(end > totalPayLoad.length){end = totalPayLoad.length;}
      packets[position] = Arrays.copyOfRange(totalPayLoad,start, end);
      start += mtu;
    }

    return packets;
  }

  public static byte[] depacketizePayload(byte[][] packets){
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
    for(byte [] payLoad :packets){
      try {
        outputStream.write(payLoad);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return outputStream.toByteArray();
  }

  public static byte getRequestType(byte [] payLoad){
    return ByteBuffer.wrap(Arrays.copyOfRange(payLoad, 0, 1)).get();
  }

  public static int getContentLength(byte [] payLoad){
    return ByteBuffer.wrap(Arrays.copyOfRange(payLoad, 1, 5)).getInt();
  }

  public static byte [] getActualPayLoad(byte [] payload){
    if(payload != null){
      return ByteBuffer.wrap(Arrays.copyOfRange(payload, 6, payload.length)).array();
    }

    return new byte[]{};
  }


  @Nullable
  public static String stringFromBytes(byte[] bytes) {
    String byteString = null;
    try {
      byteString = new String(bytes, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      LogWrapper.log(true,"Unable to convert message bytes to string");
    }
    return byteString;
  }


  public static NetworkNode getNetworkNode(BluetoothDevice device){
    NetworkNode networkNode = new NetworkNode();
    networkNode.setNodeName(device.getName());
    networkNode.setBluetoothAddress(device.getAddress());
    return networkNode;
  }


  public static List<BluetoothGattCharacteristic> findCharacteristics(BluetoothGatt bluetoothGatt) {
    List<BluetoothGattCharacteristic> matchingCharacteristics = new ArrayList<>();

    List<BluetoothGattService> serviceList = bluetoothGatt.getServices();
    BluetoothGattService service = BleAndroidUtils.findService(serviceList);
    if (service == null) {
      return matchingCharacteristics;
    }

    List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
    for (BluetoothGattCharacteristic characteristic : characteristicList) {
      if (isMatchingCharacteristic(characteristic)) {
        matchingCharacteristics.add(characteristic);
      }
    }

    return matchingCharacteristics;
  }

  @Nullable
  public static BluetoothGattCharacteristic findCourseServiceCharacteristics(BluetoothGatt bluetoothGatt) {
    return findCharacteristic(bluetoothGatt, SERVICE_STRING);
  }


  @Nullable
  private static BluetoothGattCharacteristic findCharacteristic(BluetoothGatt bluetoothGatt, String uuidString) {
    List<BluetoothGattService> serviceList = bluetoothGatt.getServices();
    BluetoothGattService service = BleAndroidUtils.findService(serviceList);
    if (service == null) {
      return null;
    }

    List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
    for (BluetoothGattCharacteristic characteristic : characteristicList) {
      if (characteristicMatches(characteristic, uuidString)) {
        return characteristic;
      }
    }

    return null;
  }

  public static boolean isCourseCheckCharacteristics(BluetoothGattCharacteristic characteristic) {
    return characteristicMatches(characteristic, SERVICE_STRING);
  }


  private static boolean characteristicMatches(BluetoothGattCharacteristic characteristic, String uuidString) {
    if (characteristic == null) {
      return false;
    }
    UUID uuid = characteristic.getUuid();
    return uuidMatches(uuid.toString(), uuidString);
  }

  private static boolean isMatchingCharacteristic(BluetoothGattCharacteristic characteristic) {
    if (characteristic == null) {
      return false;
    }
    UUID uuid = characteristic.getUuid();
    return matchesCharacteristicUuidString(uuid.toString());
  }

  private static boolean matchesCharacteristicUuidString(String characteristicIdString) {
    return uuidMatches(characteristicIdString, SERVICE_STRING);
  }

  public static boolean requiresResponse(BluetoothGattCharacteristic characteristic) {
    return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)
        != BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
  }

  public static boolean requiresConfirmation(BluetoothGattCharacteristic characteristic) {
    return (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE)
        == BluetoothGattCharacteristic.PROPERTY_INDICATE;
  }


  @Nullable
  public static BluetoothGattDescriptor findClientConfigurationDescriptor(List<BluetoothGattDescriptor> descriptorList) {
    for(BluetoothGattDescriptor descriptor : descriptorList) {
      if (isClientConfigurationDescriptor(descriptor)) {
        return descriptor;
      }
    }

    return null;
  }

  private static boolean isClientConfigurationDescriptor(BluetoothGattDescriptor descriptor) {
    if (descriptor == null) {
      return false;
    }
    UUID uuid = descriptor.getUuid();
    String uuidSubstring = uuid.toString().substring(4, 8);
    return uuidMatches(uuidSubstring, CLIENT_CONFIGURATION_DESCRIPTOR_SHORT_ID);
  }


  private static boolean matchesServiceUuidString(String serviceIdString) {
    return uuidMatches(serviceIdString, SERVICE_STRING);
  }

  @Nullable
  private static BluetoothGattService findService(List<BluetoothGattService> serviceList) {
    for (BluetoothGattService service : serviceList) {
      String serviceIdString = service.getUuid()
          .toString();
      if (matchesServiceUuidString(serviceIdString)) {
        return service;
      }
    }
    return null;
  }

  private static boolean uuidMatches(String uuidString, String... matches) {
    for (String match : matches) {
      if (uuidString.equalsIgnoreCase(match)) {
        return true;
      }
    }

    return false;
  }
}
