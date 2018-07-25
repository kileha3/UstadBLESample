package com.furahitechstudio.ustadsample.manager;

import com.furahitechstudio.ustadsample.models.NetworkNode;
import com.furahitechstudio.ustadsample.utils.BleAndroidUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import static com.furahitechstudio.ustadsample.utils.BleAndroidUtils.compress;

public abstract class BluetoothManagerShared {

  public static String SERVICE_STRING = "7D2EA28A-F7BD-485A-BD9D-92AD6ECFE93E";
  public static UUID SERVICE_UUID = UUID.fromString(SERVICE_STRING);

  public static String CLIENT_CONFIGURATION_DESCRIPTOR_STRING = "00002902-0000-1000-8000-00805f9b34fb";
  public static UUID CLIENT_CONFIGURATION_DESCRIPTOR_UUID = UUID.fromString(CLIENT_CONFIGURATION_DESCRIPTOR_STRING);

  public static final String CLIENT_CONFIGURATION_DESCRIPTOR_SHORT_ID = "2902";

  public static final byte ENTRY_STATUS_REQUEST = (byte) 111;

  public static final byte ENTRY_STATUS_RESPONSE = (byte) 112;

  private Vector<NetworkNode> connectedNetworkNodes = new Vector<>();

  private Vector<NetworkNode> knownNetworkNodes = new Vector<>();

  private Map<String, byte[]> mNetworkNodeConfigurations = new HashMap<>();

  private String dataToTransfer = "";

  public abstract boolean isBleCapable();

  public abstract boolean isBluetoothEnabled();

  public boolean isDeviceConnected(){
    return connectedNetworkNodes.size() > 0;
  }


  public abstract boolean canDeviceAdvertise();

  public abstract void startAdvertising();

  public abstract void stopAdvertising();

  public abstract void startScanning();

  public abstract void stopScanning();

  public void setDataToTransfer(String dataToTransfer){
    this.dataToTransfer = dataToTransfer;
  }

 public byte [][] getPayload(byte requestStatus){
   return BleAndroidUtils.packetizePayload(requestStatus,compress(dataToTransfer),20);
 }

  public void addNodeConfiguration(NetworkNode networkNode, byte[] value){
    mNetworkNodeConfigurations.put(networkNode.getBluetoothAddress(),value);
  }

  public void addNetworkNode(NetworkNode networkNode){
    connectedNetworkNodes.add(networkNode);
  }

  public void removeNetworkNode(NetworkNode networkNode){
    connectedNetworkNodes.remove(networkNode);
  }

  public abstract void connectToServerDevice(NetworkNode networkNode);

  public abstract void requestCourseStatuses(String coursesIds);

  public abstract void sendCourseStatuses(NetworkNode networkNode,String courseResults);

  public abstract void acknowledgeRequest(NetworkNode networkNode, int requestId, int status,
      int offset, byte[] value);

  public abstract void processPackets(NetworkNode networkNode,byte [] value);

  public abstract void disconnectServer();

  public abstract void stopService();

  public List<NetworkNode> getConnectedNodes(){
    return connectedNetworkNodes;
  }

  public List<NetworkNode> getAvailableNodes(){
    return knownNetworkNodes;
  }

  public Map<String, byte[]> getNodeConfigurations(){
    return mNetworkNodeConfigurations;
  }

  public abstract void destroy();
}
