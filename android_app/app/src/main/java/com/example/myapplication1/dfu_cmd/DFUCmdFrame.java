package com.example.myapplication1.dfu_cmd;

/**
 * Created by yuanmingwu on 17-11-28.
 */

 class DFUCmdFrame {

    private int cmdType;
    private int cmdLen;
    private byte[] cmdData;
    private int cmdCheckSum;

    public void setCmdType(int cmdType) {
        this.cmdType = cmdType;
    }

    public void setCmdLen(int cmdLen) {
        this.cmdLen = cmdLen;
    }

    public void setCmdData(byte[] data) {
        this.cmdData = data;
    }

    public void setCmdCheckSum(int checkSum) {
        this.cmdCheckSum = checkSum;
    }


    public int getCmdType(){
        return this.cmdType;
    }

    public int getCmdLen(){
        return this.cmdLen;
    }

    public byte[] getCmdData() {
        return this.cmdData;
    }

    public int getCmdCheckSum() {
        return this.cmdCheckSum;
    }
}
