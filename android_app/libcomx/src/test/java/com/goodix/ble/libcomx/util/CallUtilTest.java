package com.goodix.ble.libcomx.util;

import org.junit.Assert;
import org.junit.Test;

public class CallUtilTest {

    /*
    java.lang.Thread.getStackTrace(Thread.java:1559) <-- Stack获取函数
    com.goodix.ble.libcomx.util.CallUtil.trace(CallUtil.java:23)
    com.goodix.ble.libcomx.util.CallUtil.trace(CallUtil.java:9)
    com.goodix.ble.libcomx.util.CallUtilTest.callee1(CallUtilTest.java:29)
    com.goodix.ble.libcomx.util.CallUtilTest.testCallee(CallUtilTest.java:17) <-- 打印出该调用者 需要跳过4层
    com.goodix.ble.libcomx.util.CallUtilTest.trace(CallUtilTest.java:10)
     */
    /*
    dalvik.system.VMStack.getThreadStackTrace(Native Method)
    java.lang.Thread.getStackTrace(Thread.java:1538) <-- Stack获取函数
    com.goodix.ble.libcomx.util.CallUtil.trace(CallUtil.java:23)
    com.goodix.ble.libcomx.util.CallUtil.trace(CallUtil.java:9)
    com.goodix.ble.gr.toolbox.app.prj_mesh.MeshActivity.onViewCreated(MeshActivity.java:103)
    com.goodix.ble.gr.toolbox.app.prj_mesh.BaseBleActivity.onCreate(BaseBleActivity.java:101) <-- 打印出该调用者 需要跳过5层
    android.app.Activity.performCreate(Activity.java:7174)
    android.app.Activity.performCreate(Activity.java:7165)
     */
    @Test
    public void trace() {
        testCallee();
    }

    private void testCallee() {
        String ret;

        System.out.println("---------");
        ret = callee1();
        System.out.println(ret);
        Assert.assertEquals(ret, "testCallee(CallUtilTest.java:35)");

        ret = callee2();
        System.out.println(ret);
        Assert.assertTrue(ret.startsWith("testCallee(CallUtilTest.java:39)"));
        Assert.assertEquals(ret.split("\n").length, 3);

        ret = callee3();
        System.out.println(ret);
        Assert.assertTrue(ret.startsWith("testCallee(CallUtilTest.java:44)"));
        Assert.assertEquals(ret.split("\n").length, 5);

        System.out.println("+++++++++");
    }

    private String callee1() {
        return CallUtil.trace(0, null, null).toString();
    }

    private String callee2() {
        return CallUtil.trace(3, "\n  <-", null).toString();
    }

    private String callee3() {
        HexStringBuilder builder = new HexStringBuilder(1024);
        CallUtil.trace(5, "\n  <-", builder);
        return builder.toString();
    }
}