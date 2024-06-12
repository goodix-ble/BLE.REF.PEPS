package com.example.myapplication1.dfu_cmd;

/**
 * Created by yuanmingwu on 17-11-28.
 */

public class DFUCmd {

    public static final int SYSTEM_INFO_START_ADDRESS = 0x1000000;//img info开始地址
    public static final int SYSTEM_INFO_LENGTH = 0x1D0;


    public static final byte ACK_SUCCESS = 0x01;
    public static final byte ACK_ERROR = 0x02;

    public static final byte FRAME_HEADER_L = 0x44;
    public static final byte FRAME_HEADER_H = 0x47;

    public static final byte SYSTEM_RESET_CMD = 0x02;
    public static final byte PROGRAM_START_CMD = 0x23;
    public static final byte PROGRAME_FLASH_CMD = 0x24;
    public static final byte PROGRAME_END_CMD = 0x25;
    public static final byte OPERATE_SYSTEM_CONFIG = 0x27;
    public static final byte OPERATE_NVDS = 0x28;
    public static final byte OPERATE_EFUSE = 0x29;
    public static final byte CONFIG_SPI_FLASH = 0x2A;
    public static final byte GET_SPI_FLASH_ID = 0x2B;


    public static final byte READ_SYSTEM_CONFIG = 0x00;
    public static final byte UPDATE_SYSTEM_CONFIG = 0x01;
}
