package com.goodix.ble.libcomx.task;

import com.goodix.ble.libcomx.event.IEventListener;
import com.goodix.ble.libcomx.util.HexStringBuilder;
import com.goodix.ble.libcomx.util.HexStringParser;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TaskTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void setParameterTest() {
        Integer val = 123456;
        Float flaotVal = 1.23f;
        SimpleTask task = new SimpleTask();
        task.setParameter(Integer.class, val);
        task.setParameter(Float.class, flaotVal);
        task.evtFinished().register(new IEventListener<ITaskResult>() {
            @Override
            public void onEvent(Object src, int type, ITaskResult param) {
                System.out.println("+++++++++");
                Assert.assertNotEquals(null, param.getError());
                Assert.assertEquals("Parameter floatParamNull is null.", param.getError().getMessage());
                Assert.assertEquals(ITaskResult.CODE_ERROR, param.getCode());
            }
        });
        task.start(null, null);
        Assert.assertEquals(val, task.intParam);

        System.out.println("---------------------");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void checksum() {
        String abc = "24 00 \n" +
                "07 04 (len=1031)\n" +
                "01 (type)\n" +
                "00 34 06 01 (addr)\n" +
                "00 04 (len=1024)\n" +
                "4C EE 00 30 30 B5 05 46 00 20 0B 23 0E 4C 54 F8 23 20 AA 42 02 D9 5B 1E 00 2B F8 DA 0B 22 29 44 \n" +
                "54 F8 22 50 A9 42 02 D8 52 1E 00 2A F8 DA D9 B2 03 25 05 E0 4C 00 05 FA 04 F3 18 43 49 1C C9 B2 \n" +
                "91 42 F7 DD 30 BD 00 00 CC 44 00 30 01 48 07 C8 18 F0 75 BE 20 41 00 30 06 46 0F 46 15 46 72 B6 \n" +
                "38 46 FD F7 9B FA 40 28 01 D9 40 20 02 E0 38 46 FD F7 94 FA 80 46 30 46 FD F7 90 FA 40 28 01 D9 \n" +
                "40 20 02 E0 30 46 FD F7 89 FA 04 46 9C 21 10 48 FD F7 44 FB 42 46 39 46 0D 48 FD F7 9E FA 0C 48 \n" +
                "22 46 31 46 5C 30 FD F7 98 FA 09 48 4F F0 AA 31 01 64 45 64 4F F0 BB 31 81 64 4F F0 CC 31 01 65 \n" +
                "4F F0 DD 31 81 65 00 20 00 F0 04 F8 FE E7 00 00 28 73 00 30 30 B5 AD F2 0C 4D 04 46 4F F4 80 61 \n" +
                "03 A8 FD F7 1B FB 18 4D 20 20 85 F8 3F 00 85 F8 9B 00 3C B1 28 46 01 2C 43 6D C2 6C 09 D0 02 2C \n" +
                "0C D0 0F E0 10 4A 11 A1 5C 32 03 A8 FD F7 30 F8 08 E0 11 A1 03 A8 FD F7 2B F8 03 E0 16 A1 03 A8 \n" +
                "FD F7 26 F8 03 A9 0B A2 68 6C CD E9 01 21 00 90 18 A3 05 4A 18 A1 00 20 00 F0 7C FA 00 F0 57 FA \n" +
                "0D F2 0C 4D 30 BD 00 00 28 73 00 30 5B 45 52 52 4F 52 5D 20 25 73 00 00 5B 57 41 52 4E 49 4E 47 \n" +
                "5D 20 50 61 72 61 6D 30 3A 25 64 2C 50 61 72 61 6D 31 3A 25 64 00 00 00 5B 50 41 52 41 4D 5D 20 \n" +
                "50 61 72 61 6D 30 3A 25 64 2C 50 61 72 61 6D 31 3A 25 64 00 00 00 00 00 61 70 70 5F 61 73 73 65 \n" +
                "72 74 2E 63 00 00 00 00 06 46 0F 46 90 46 1D 46 72 B6 40 46 FD F7 F2 F9 40 28 01 D9 40 20 02 E0 \n" +
                "40 46 FD F7 EB F9 04 46 9C 21 0D 48 FD F7 A6 FA 22 46 41 46 0A 48 FD F7 00 FA 09 48 4F F0 55 31 \n" +
                "01 64 45 64 4F F0 66 31 81 64 C6 64 4F F0 77 31 01 65 47 65 C9 43 81 65 02 20 FF F7 6B FF FE E7 \n" +
                "28 73 00 30 2D E9 F0 41 06 46 0F 46 90 46 1D 46 40 46 FD F7 C3 F9 40 28 01 D9 40 20 02 E0 40 46 \n" +
                "FD F7 BC F9 04 46 9C 21 0D 48 FD F7 77 FA 22 46 41 46 0B 48 FD F7 D1 F9 09 48 4F F0 11 31 01 64 \n" +
                "45 64 49 00 81 64 C6 64 4F F0 33 31 01 65 47 65 4F F0 44 31 81 65 BD E8 F0 41 01 20 FF F7 3A BF \n" +
                "28 73 00 30 70 47 00 00 10 B5 86 B0 09 49 00 20 01 F0 30 FB 04 46 00 2C 09 D0 14 22 06 49 01 A8 \n" +
                "FD F7 F0 F9 AD F8 08 40 01 A8 00 F0 07 F8 06 B0 10 BD 00 00 C8 41 00 30 30 E3 02 30 0E B5 05 46 \n" +
                "4F F4 80 61 1A 48 FD F7 17 FA 28 78 10 B1 01 28 1A D0 1E E0 00 24 17 4E AA 88 36 F8 34 00 82 42 \n" +
                "07 D1 06 EB C4 00 14 A1 43 68 11 48 FC F7 50 FF 05 E0 1E 2C 03 D1 16 A1 0D 48 FC F7 49 FF 64 1C \n" +
                "E4 B2 1E 2C E8 D3 04 E0 1C A1 09 48 6A 68 FC F7 3F FF 07 49 20 A2 28 69 CD E9 01 21 00 90 D5 E9 \n" +
                "02 23 1E A1 00 20 00 F0 95 F9 00 F0 70 F9 FE E7 26 6F 00 30 30 40 00 30 45 72 72 6F 72 20 63 6F \n" +
                "64 65 20 30 58 25 30 34 58 3A 20 25 73 00 00 00 45 72 72 6F 72 20 63 6F 64 65 20 30 58 25 30 34 \n" +
                "58 3A 20 4E 6F 20 66 6F 75 6E 64 20 69 6E 66 6F 72 6D 61 74 69 6F 6E 2E 00 00 00 00 28 25 73 29 \n" +
                "20 69 73 20 6E 6F 74 20 65 73 74 61 62 6C 69 73 68 65 64 2E 00 00 00 00 25 73 00 00 61 70 70 5F \n" +
                "65 72 72 6F 72 2E 63 00 0E B5 00 29 0A D0 06 A0 73 22 CD E9 00 20 02 91 0C 4B 0D A2 17 A1 03 20 \n" +
                "00 F0 48 F9 0E BD 00 00 41 64 76 65 72 74 69 6E 67 20 73 74 61 72 74 65 64 20 66 61 69 6C 65 64 \n" +
                "28 30 58 25 30 32 58 29 2E 00 00 00 AC E1 02 30 2E 2E 5C 53 72 63 5C 75 73 65 72 5F 63 61 6C 6C \n" +
                "62 61 63 6B 5C 75 73 65 72 5F 67 61 70 5F 63 61 6C 6C 62 61 63 6B 2E 63 00 00 00 00 75 73 65 72 ";
        byte[] dat = new byte[1031 + 4];
        int ret = HexStringParser.parse(abc, dat, 0, dat.length);
        Assert.assertEquals(dat.length, ret);
        System.out.println(new HexStringBuilder(2048).dump(dat, 32).toString());

        int sum = 0;
        for (int j = 0; j < dat.length; j++) {
            sum += 0xff & dat[j];
        }
        sum = 0xffff & sum;

        System.out.println(new HexStringBuilder(8).put(sum, 2).toString());

        Assert.assertEquals(0x87FA, sum);
    }
}