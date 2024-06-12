package com.example.myapplication1.dfu_cmd;


import com.example.myapplication1.DfuLog;
import com.example.myapplication1.HexUtil;

public class DFUCmdSend {
    private static final String Tag = "DfuCmdSend";
    public static byte[] resetCmd(byte[] bootInfo){
        DfuLog.i(Tag, "resetCmd-->"+ HexUtil.encodeHexStr(bootInfo));
        if(bootInfo != null) {
            return isp_send_frame(bootInfo,DFUCmd.SYSTEM_RESET_CMD);
        }else {
            return isp_send_frame(null,DFUCmd.SYSTEM_RESET_CMD);
        }
    }

    public static byte[] readSystemConfig(int addr, int readLen) {
        DfuLog.i(Tag, "readSystemConfig-->"+"addr = "+addr+",readLen = "+readLen);
        byte[] sendContent = new byte[7];
        sendContent[0] =  DFUCmd.READ_SYSTEM_CONFIG;
        sendContent[1] = (byte)(addr & 0xff);
        sendContent[2] = (byte)((addr>>8) & 0xff);
        sendContent[3] = (byte)((addr>>16) & 0xff);
        sendContent[4] = (byte)((addr>>24) & 0xff);

        sendContent[5] = (byte) (readLen & 0xff);
        sendContent[6] = (byte) ((readLen>> 8) & 0xff);
        return isp_send_frame(sendContent,DFUCmd.OPERATE_SYSTEM_CONFIG);
    }

    public static byte[] updateSystemConfig(int addr, byte[] data) {
        DfuLog.i(Tag, "updateSystemConfig-->"+"addr = "+addr +" "+ HexUtil.encodeHexStr(data));
        byte[] sendContent = new byte[7+data.length];
        sendContent[0] = DFUCmd.UPDATE_SYSTEM_CONFIG;
        sendContent[1] = (byte)(addr & 0xff);
        sendContent[2] = (byte)((addr>>8) & 0xff);
        sendContent[3] = (byte)((addr>>16) & 0xff);
        sendContent[4] = (byte)((addr>>24) & 0xff);

        sendContent[5] = (byte) (data.length & 0xff);
        sendContent[6] = (byte) ((data.length>> 8) & 0xff);
        for(int i=0; i<data.length; i++) {
            sendContent[7+i] = data[i];
        }
        return isp_send_frame(sendContent,DFUCmd.OPERATE_SYSTEM_CONFIG);
    }


    public static byte[] programStartCmd(byte[] data) {
        return isp_send_frame(data,DFUCmd.PROGRAM_START_CMD);
    }


    public static byte[] programFlash(int addr,byte[] data,int program_type, int flash_type) {
        DfuLog.i(Tag, "programFlash-->"+ "addr = "+addr+" ");
        byte type = (byte) ((flash_type << 4) | program_type);

        byte[] sendContent = new byte[7+data.length];
        sendContent[0] = type;

        sendContent[1] = (byte)(addr & 0xff);
        sendContent[2] = (byte)((addr>>8) & 0xff);
        sendContent[3] = (byte)((addr>>16) & 0xff);
        sendContent[4] = (byte)((addr>>24) & 0xff);

        sendContent[5] = (byte) (data.length & 0xff);
        sendContent[6] = (byte) ((data.length>> 8) & 0xff);

        for(int i=0; i<data.length; i++) {
            sendContent[7+i] = data[i];
        }
        return isp_send_frame(sendContent, DFUCmd.PROGRAME_FLASH_CMD);
    }


    public static byte[] programEndCmd(int file_check_sum, int mode) {
        DfuLog.i(Tag, "programEndCmd-->"+file_check_sum+" "+mode);
        byte[] sendBytes = new byte[5];
        sendBytes[0] = (byte) mode;
        sendBytes[1] = (byte) (file_check_sum & 0xff);
        sendBytes[2] = (byte) ((file_check_sum >> 8) & 0xff);
        sendBytes[3] = (byte) ((file_check_sum >> 16) & 0xff);
        sendBytes[4] = (byte) ((file_check_sum >> 24) & 0xff);
        return isp_send_frame(sendBytes,DFUCmd.PROGRAME_END_CMD);
    }

    public static byte[] configSpiFlash(int csPin, int csMux, int spiGroup) {
        DfuLog.i(Tag, "configSpiFlash-->"+csPin+" "+csMux+" "+spiGroup);
        byte[] sendBytes = new byte[3];
        sendBytes[0] = (byte) csPin;
        sendBytes[1] = (byte) csMux;
        sendBytes[2] = (byte) spiGroup;
        return isp_send_frame(sendBytes,DFUCmd.CONFIG_SPI_FLASH);
    }

    public static byte[] getSpiFlashId() {
        DfuLog.i(Tag, "getSpiFlashId");
        return isp_send_frame(null,DFUCmd.GET_SPI_FLASH_ID);
    }


    private static byte[] isp_send_frame(byte[] data,byte cmd) {
        int checkSum = 0;
        int len = 0;
        if(data != null) {
            len = data.length;
        }
        byte[] sendData = new byte[8+len];

        checkSum += cmd & 0xff;
        checkSum += (cmd >> 8)&0xff;
        checkSum += len& 0xff;
        checkSum += (len >> 8)&0xff;

        sendData[0] = DFUCmd.FRAME_HEADER_L;
        sendData[1] = DFUCmd.FRAME_HEADER_H;

        sendData[2] = cmd;
        sendData[3] = 0x00;

        sendData[4] = (byte) (len & 0xff);
        sendData[5] = (byte) ((len >> 8) & 0xff);

       for(int i=0; i<len; i++) {
           sendData[6+i] = data[i];
           checkSum += (data[i] & 0xff);
       }
        sendData[6+len] = (byte) (checkSum & 0xff);
        sendData[7+len] = (byte) ((checkSum>> 8) & 0xff);

        return sendData;
    }




}
