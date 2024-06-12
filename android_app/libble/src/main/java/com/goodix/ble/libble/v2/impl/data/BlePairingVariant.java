package com.goodix.ble.libble.v2.impl.data;

public class BlePairingVariant {
    public static final int PIN = 0;
    public static final int PASSKEY = 1;
    public static final int PASSKEY_CONFIRMATION = 2;
    public static final int CONSENT = 3;
    public static final int DISPLAY_PASSKEY = 4;
    public static final int DISPLAY_PIN = 5;
    public static final int OOB_CONSENT = 6;

    public static String toString(final int variant) {
        switch (variant) {
            case PIN:
                return "PIN";
            case PASSKEY:
                return "PASSKEY";
            case PASSKEY_CONFIRMATION:
                return "PASSKEY_CONFIRMATION";
            case CONSENT:
                return "CONSENT";
            case DISPLAY_PASSKEY:
                return "DISPLAY_PASSKEY";
            case DISPLAY_PIN:
                return "DISPLAY_PIN";
            case OOB_CONSENT:
                return "OOB_CONSENT";
            default:
                return "UNKNOWN";
        }
    }
}
