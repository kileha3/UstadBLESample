package com.furahitechstudio.ustadsample.manager;

import com.furahitechstudio.ustadsample.models.NetworkNode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

public abstract class BluetoothManagerShared {

  public static String SERVICE_STRING = "7D2EA28A-F7BD-485A-BD9D-92AD6ECFE93E";
  public static UUID SERVICE_UUID = UUID.fromString(SERVICE_STRING);

  public static String CLIENT_CONFIGURATION_DESCRIPTOR_STRING = "00002902-0000-1000-8000-00805f9b34fb";
  public static UUID CLIENT_CONFIGURATION_DESCRIPTOR_UUID = UUID.fromString(CLIENT_CONFIGURATION_DESCRIPTOR_STRING);

  public static final String CLIENT_CONFIGURATION_DESCRIPTOR_SHORT_ID = "2902";

  public static final int MAXIMUM_DATA_CHUNK_SIZE = 20;

  public static final String DATA_SEGMENT_START="start";

  public static final String DATA_SEGMENT_END="end";

  public byte[][] BYTES_TO_BE_TRANSFERRED = null;

  private Vector<NetworkNode> connectedNetworkNodes = new Vector<>();

  private Vector<NetworkNode> knownNetworkNodes = new Vector<>();

  private Map<String, byte[]> mNetworkNodeConfigurations = new HashMap<>();

  private StringBuilder stringBuilder = new StringBuilder();

  private String dataToBeTransferred = "";

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

  public Integer getPacketSize(){
    return (int) Math.ceil( dataToBeTransferred.getBytes().length / (double)MAXIMUM_DATA_CHUNK_SIZE);
  }


  public void setStringDataToBeTransferred(String dataToBeTransferred){
    this.dataToBeTransferred = dataToBeTransferred;
    byte [] data = dataToBeTransferred.getBytes();
    byte[][] packets = new byte[getPacketSize()][MAXIMUM_DATA_CHUNK_SIZE];
    Integer start = 0;
    for(int i = 0; i < packets.length; i++) {
      int end = start+MAXIMUM_DATA_CHUNK_SIZE;
      if(end > data.length){end = data.length;}
      packets[i] = Arrays.copyOfRange(data,start, end);
      start += MAXIMUM_DATA_CHUNK_SIZE;
    }
    BYTES_TO_BE_TRANSFERRED = packets;
  }
  public byte[][] getBytesToSend(){
    return BYTES_TO_BE_TRANSFERRED;
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

  public abstract void sendCourseStatuses(String courseResults);

  public abstract void acknowledgeRequest(NetworkNode networkNode, int requestId, int status,
      int offset, byte[] value);

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

  public void storeReceivedSegment(String dataSegment){
    stringBuilder.append(dataSegment);
  }

  public String getCombinedReceivedSegments(){
    return stringBuilder
        .toString()
        .replace(DATA_SEGMENT_START,"")
        .replace(DATA_SEGMENT_END,"");
  }

  public abstract void destroy();
}
