package com.goodix.ble.libcomx.transceiver.buffer.ring;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

public class RingBufferTest {

    @Test
    public void getSize() {
        final int BUFFER_SIZE = 5;
        RingBuffer ringBuffer = new RingBuffer(BUFFER_SIZE);
        Assert.assertEquals(0, ringBuffer.getSize());
        for (int i = 0; i < BUFFER_SIZE * 4; i++) {
            byte val = (byte) i;
            ringBuffer.put(val);
            Assert.assertEquals(1, ringBuffer.getSize());
            ringBuffer.put(val);
            Assert.assertEquals(2, ringBuffer.getSize());
            ringBuffer.put(val);
            Assert.assertEquals(3, ringBuffer.getSize());
            ringBuffer.put(val);
            Assert.assertEquals(4, ringBuffer.getSize());
            ringBuffer.put(val);
            Assert.assertEquals(4, ringBuffer.getSize());
            ringBuffer.drop(1);
            Assert.assertEquals(3, ringBuffer.getSize());
            ringBuffer.drop(2);
            Assert.assertEquals(1, ringBuffer.getSize());
            ringBuffer.drop(1);
            Assert.assertEquals(0, ringBuffer.getSize());
            ringBuffer.drop(2);
            Assert.assertEquals(0, ringBuffer.getSize());
            System.out.println("===> R: " + ringBuffer.getReadPos() + "   W: " + ringBuffer.getWritePos());
        }
    }

    @Test
    public void get() {
        final int BUFFER_SIZE = 10;
        RingBuffer ringBuffer = new RingBuffer(BUFFER_SIZE);
        byte[] test = new byte[BUFFER_SIZE];
        for (int i = 0; i < BUFFER_SIZE; i++) {
            test[i] = (byte) (i + 0x50);
        }
        Assert.assertEquals(0, ringBuffer.put(null, 0, 0));
        Assert.assertEquals(BUFFER_SIZE - 1, ringBuffer.put(test, 0, 0));
        Assert.assertEquals(0, ringBuffer.put(test, 0, 0));

        for (int i = 0; i < ringBuffer.getSize(); i++) {
            Assert.assertEquals(test[i], ringBuffer.get(i));
        }

        for (int k = 0; k < BUFFER_SIZE * 4; k++) {
            System.out.println("===> R: " + ringBuffer.getReadPos() + "   W: " + ringBuffer.getWritePos());
            ringBuffer.dropAll();
            Assert.assertEquals(BUFFER_SIZE - 1, ringBuffer.put(test, 0, 0));
            for (int i = 0; i < ringBuffer.getSize(); i++) {
                Assert.assertEquals(test[i], ringBuffer.get(i));
            }
        }
    }

    @Test
    public void get1() {
        final int BUFFER_SIZE = 20;
        final int INPUT_SIZE = 3;
        final int CHECK_SIZE = INPUT_SIZE * 3;
        final int DUMP_SIZE = CHECK_SIZE;

        RingBuffer ringBuffer = new RingBuffer(BUFFER_SIZE);
        byte[] inputBuf = new byte[INPUT_SIZE];
        byte[] checkBuf = new byte[CHECK_SIZE];
        byte[] dumpBuf = new byte[DUMP_SIZE];

        int val = 1;
        for (int k = 0; k < BUFFER_SIZE; k++) {
            for (int j = 0; j < CHECK_SIZE; j += INPUT_SIZE) {
                for (int i = 0; i < INPUT_SIZE; i++) {
                    inputBuf[i] = (byte) (val++);
                }
                Assert.assertEquals(INPUT_SIZE, ringBuffer.put(inputBuf, 0, 0));
                System.arraycopy(inputBuf, 0, checkBuf, j, INPUT_SIZE);
            }
            Assert.assertEquals(DUMP_SIZE, ringBuffer.get(0, dumpBuf));
            Assert.assertArrayEquals(checkBuf, dumpBuf);
            Assert.assertEquals(DUMP_SIZE, ringBuffer.getSize());
            ringBuffer.drop(DUMP_SIZE);
            Assert.assertEquals(0, ringBuffer.getSize());
            System.out.println("===> R: " + ringBuffer.getReadPos() + "   W: " + ringBuffer.getWritePos());
        }
    }

    @Test
    public void pop() {
        final int BUFFER_SIZE = 20;
        final int TEST_SIZE = 20 * 3;

        RingBuffer ringBuffer = new RingBuffer(BUFFER_SIZE);
        byte[] testBuf = new byte[TEST_SIZE];
        byte[] dumpBuf = new byte[TEST_SIZE];
        Random random = new Random();

        random.nextBytes(testBuf);

        for (int i = 0; i < TEST_SIZE; ) {
            int stepSize = random.nextInt(BUFFER_SIZE / 2) + 1;
            if (stepSize + i > TEST_SIZE) {
                stepSize = TEST_SIZE - i;
            }

            Assert.assertEquals(stepSize, ringBuffer.put(testBuf, i, stepSize));
            Assert.assertEquals(stepSize, ringBuffer.pop(0, dumpBuf));

            for (int k = 0; k < stepSize; k++) {
                Assert.assertEquals(testBuf[i], dumpBuf[k]);
                i++;
            }

            System.out.println("===> R: " + ringBuffer.getReadPos() + "   W: " + ringBuffer.getWritePos());
        }
    }
}