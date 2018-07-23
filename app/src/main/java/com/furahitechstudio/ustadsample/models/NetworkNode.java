package com.furahitechstudio.ustadsample.models;

public class NetworkNode {

  private String nodeName, bluetoothAddress;

  public String getNodeName() {
    return nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }

  public String getBluetoothAddress() {
    return bluetoothAddress;
  }

  public void setBluetoothAddress(String bluetoothAddress) {
    this.bluetoothAddress = bluetoothAddress;
  }


  @Override
  public boolean equals(Object device) {
    return device !=null && device instanceof NetworkNode &&
        this.bluetoothAddress.equals(((NetworkNode)device).bluetoothAddress);
  }
}
