package com.example.myapplication1.dfu_cmd;


import com.example.myapplication1.DfuLog;
import com.example.myapplication1.HexUtil;

public class DFUCmdParse {
    private static final String Tag = "DfuCmdParse";
    private  final int RECEIVE_MAX_LEN = 2048;
    private  int state = 0;

    private  int cmdType = 0;
    private  int cmdLen = 0;
    private  byte[] cmdData;
    private  int cmdCheckSum = 0;
    private  int receiveDataCount  = 0;

    public void receiveParse(byte[] data) {
        for(int i=0; i<data.length; i++)
        {
            switch(state)
            {
                //check frame_header 0x44
                case 0:
                {
                    if(data[i] == DFUCmd.FRAME_HEADER_L)
                    {
                        state=1;
                    }
                }
                break;

                //check frame_header 0x47
                case 1:
                {
                    if(data[i] == DFUCmd.FRAME_HEADER_H)
                    {
                        state=2;
                    } else if(data[i] == DFUCmd.FRAME_HEADER_L) {
                        state = 1;
                    } else {
                        state = 0;
                    }
                }
                break;

                //receive cmd type
                case 2:
                {
                    cmdType = data[i]&0xff;
                    state = 3;
                }
                break;

                //receive cmd type
                case 3:
                {
                    cmdType |= (data[i] << 8)&0xffff;
                    state = 4;
                }
                break;

                //receive content length
                case 4:
                {
                    cmdLen = data[i]&0xff;
                    state = 5;
                }
                break;

                //receive content length
                case 5:
                {
                    cmdLen |= (data[i] << 8)&0xffff;
                    if(cmdLen == 0)
                    {
                        state = 7;
                    }
                    else if(cmdLen >= RECEIVE_MAX_LEN)
                    {
                        state = 0;
                    }
                    else
                    {
                        receiveDataCount = 0;
                        cmdData = new byte[cmdLen];
                        state = 6;
                    }
                }
                break;

                //receive content data
                case 6:
                {
                    cmdData[receiveDataCount] = data[i];
                    if(++receiveDataCount == cmdLen)
                    {
                        state = 7;
                    }
                }
                break;

                //receive check sum
                case 7:
                {
                    cmdCheckSum = (data[i] & 0xff);
                    state = 8;
                }
                break;

                //receive check sum
                case 8:
                {
                    cmdCheckSum |= ((data[i] << 8) & 0xffff);
                    state = 0;
                    cmdProcess();
                    return;
                }

                default:{state=0;}break;
            }
        }
    }

    private void cmdProcess() {
        int checkSum = 0;
        checkSum += cmdType & 0xff;
        checkSum += (cmdType >> 8)&0xff;
        checkSum += cmdLen& 0xff;
        checkSum += (cmdLen >> 8)&0xff;
        for(int i=0; i<cmdData.length; i++) {
            checkSum += (cmdData[i] & 0xff);
            checkSum = (checkSum & 0xffff);
        }
        if((checkSum & 0xffff) == cmdCheckSum) {
            cmdParse();
        } else {
            DfuLog.e(Tag, "checkSum error");
        }
    }


    private void cmdParse() {
        DfuLog.i(Tag, "cmdType = "+cmdType+",cmdStatus = "+cmdData[0]+"CmdData = "+ HexUtil.encodeHexStr(cmdData));
    }



}
