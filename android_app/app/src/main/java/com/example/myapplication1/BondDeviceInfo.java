package com.example.myapplication1;

public class BondDeviceInfo {

    private String name;
    private String addr;
    private boolean isConnected;

    public void setName(String name) {
        this.name = name;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public String getName() {
        return name;
    }

    public String getAddr() {
        return  addr;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
