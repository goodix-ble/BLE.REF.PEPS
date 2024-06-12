package com.example.myapplication1;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Glen on 9/18/2017.
 */

public class SharedPreferenceUtil {
    private Context mContext;
    private final String SHAREPRE = "E_CALL";
    private final String APP_ID = "appStringId";
    private final String BOND_DEVICE_ID = "bondedDeviceStringID";

    public SharedPreferenceUtil(Context mContext) {
        this.mContext = mContext;
    }

    public String getAppID() {
        SharedPreferences mSP = mContext.getSharedPreferences(SHAREPRE, Activity.MODE_PRIVATE);
        String appid = mSP.getString(APP_ID, null);
        if(appid == null){
            appid = createRandom(false,6);
            SharedPreferences.Editor editor = mSP.edit();
            editor.putString(APP_ID, appid);
            editor.apply();
        }
        return appid;
    }

    public void clearInfo() {
        SharedPreferences mSP = mContext.getSharedPreferences(SHAREPRE, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSP.edit();
        editor.putString(APP_ID, null);
        editor.putString("name", null);
        editor.putString("addr", null);
        editor.putString("shareKey", null);
        editor.putBoolean("SHARED_INFO", false);
        editor.putString("commonAdvName", "Com123456");
        editor.apply();
    }

    public int getBondDeviceCount(){
        SharedPreferences mSP = mContext.getSharedPreferences(SHAREPRE, Activity.MODE_PRIVATE);
        return mSP.getInt("count", 0);
    }

    public String getBondDeviceName() {
        SharedPreferences mSP = mContext.getSharedPreferences(SHAREPRE, Activity.MODE_PRIVATE);
        return mSP.getString( "name", null);
    }

    public String getBondDeviceAddr() {
        SharedPreferences mSP = mContext.getSharedPreferences(SHAREPRE, Activity.MODE_PRIVATE);
        return mSP.getString("addr", null);
    }

    public void BondDeviceAdd(String name, String addr) {
        SharedPreferences mSP = mContext.getSharedPreferences(SHAREPRE, Activity.MODE_PRIVATE);
        //int num =  mSP.getInt("count", 0);
       // num +=1;
        SharedPreferences.Editor editor = mSP.edit();
        //editor.putInt("count", num);
        editor.putString("name", name);
        editor.putString("addr", addr);
        editor.apply();
    }

    public void saveShareKey(byte[] shareKey) {
        SharedPreferences mSP = mContext.getSharedPreferences(SHAREPRE, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSP.edit();
        editor.putString("shareKey", HexUtil.encodeHexStr(shareKey));
        editor.apply();
    }

    public byte[] getShareKey() {
        byte[] defaultReturn = {0x01, 0x01};
        SharedPreferences mSP = mContext.getSharedPreferences(SHAREPRE, Activity.MODE_PRIVATE);
        String shareKey = mSP.getString("shareKey",null);
        if(shareKey != null) {
            DfuLog.i("shareKey"+shareKey.length()+" " +shareKey);
            String key = shareKey.substring(0,64);
            return HexUtil.HexString2Bytes(key);
        }
        return defaultReturn;
    }

    public boolean ifSharedInfo() {
        SharedPreferences mSP = mContext.getSharedPreferences(SHAREPRE, Activity.MODE_PRIVATE);
        boolean sharedInfo = mSP.getBoolean("SHARED_INFO", false);
        if(sharedInfo == false){
            SharedPreferences.Editor editor = mSP.edit();
            editor.putBoolean("SHARED_INFO", true);
            editor.apply();
        }
        return sharedInfo;
    }

    public void saveCommonAdvName(String name){
        SharedPreferences mSP = mContext.getSharedPreferences(SHAREPRE, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSP.edit();
        editor.putString("commonAdvName", name);
        editor.apply();
    }

    public String getCommonAdvName(){
        SharedPreferences mSP = mContext.getSharedPreferences(SHAREPRE, Activity.MODE_PRIVATE);
        return mSP.getString("commonAdvName", "Com123456");
    }

    private  String createRandom(boolean numberFlag, int length){
        String retStr = "";
        String strTable = numberFlag ? "1234567890" : "1234567890abcdefghijkmnpqrstuvwxyz";
        int len = strTable.length();
        boolean bDone = true;
        do {
            retStr = "";
            int count = 0;
            for (int i = 0; i < length; i++) {
                double dblR = Math.random() * len;
                int intR = (int) Math.floor(dblR);
                char c = strTable.charAt(intR);
                if (('0' <= c) && (c <= '9')) {
                    count++;
                }
                retStr += strTable.charAt(intR);
            }
            if (count >= 2) {
                bDone = false;
            }
        } while (bDone);

        return retStr;
    }

}
