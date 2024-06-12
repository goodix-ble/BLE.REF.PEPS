package com.goodix.ble.libble;

import android.util.SparseArray;

import java.util.HashMap;
import java.util.UUID;

public class BleUuid {
    // CLIENT CHARACTERISTIC CONFIG DESCRIPTOR
    public static final UUID CCCD = BleUuid.from(0x2902);

    public final static UUID GENERIC_ATTRIBUTE_SERVICE = BleUuid.from(0x1801);
    public static final UUID SERVICE_CHANGED_CHARACTERISTIC = BleUuid.from(0x2A05);

    private static HashMap<String, UUID> cache;
    private static SparseArray<UUID> cacheFor16bit;

    public synchronized static UUID from(String uuid) {
        uuid = uuid.toUpperCase();

        if (cache == null) {
            cache = new HashMap<>(64);
        }

        UUID existUuid = cache.get(uuid);
        if (existUuid == null) {
            existUuid = UUID.fromString(uuid);
            cache.put(uuid, existUuid);
        }

        return existUuid;
    }

    public synchronized static UUID from(int uuid16bit32bit) {
        if (cacheFor16bit == null) {
            cacheFor16bit = new SparseArray<>(64);
        }

        UUID uuid = cacheFor16bit.get(uuid16bit32bit);
        if (uuid == null) {
            uuid = new UUID(0x00000000_0000_1000L | ((long) uuid16bit32bit << 32), 0x8000_00805F9B34FBL);
            cacheFor16bit.put(uuid16bit32bit, uuid);
        }

        return uuid;
    }

    public static boolean is16bit32bitUuid(UUID uuid) {
        if (uuid != null) {
            if (uuid.getLeastSignificantBits() == 0x8000_00805F9B34FBL) {
                return ((uuid.getMostSignificantBits() & 0x00000000_FFFF_FFFFL) == 0x00000000_0000_1000L);
            }
        }
        return false;
    }

    public static int get16bit32bitUuid(UUID uuid) {
        if (uuid != null) {
            return (int) ((uuid.getMostSignificantBits() >> 32) & 0xFFFFFFFFL);
        }
        return 0;
    }
}
