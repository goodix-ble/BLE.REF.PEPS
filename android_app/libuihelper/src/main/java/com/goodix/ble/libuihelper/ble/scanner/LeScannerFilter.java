package com.goodix.ble.libuihelper.ble.scanner;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class LeScannerFilter implements ILeScannerFilter, Parcelable {
    public boolean checkLocalName;
    public String localName;

    public boolean checkAddress;
    public String address;
    public Pattern addressPattern;

    public boolean checkRssi;
    public int minRssi;

    public boolean checkServiceUuid;
    public boolean matchAnyServiceUuid;
    public final ArrayList<UUID> serviceUuids;

    protected LeScannerFilter(Parcel in) {
        checkLocalName = in.readByte() != 0;
        localName = in.readString();
        checkAddress = in.readByte() != 0;
        address = in.readString();
        String pattern = in.readString();
        if (pattern != null) {
            addressPattern = Pattern.compile(pattern);
        }
        checkRssi = in.readByte() != 0;
        minRssi = in.readInt();
        checkServiceUuid = in.readByte() != 0;
        matchAnyServiceUuid = in.readByte() != 0;

        int size = in.readInt();
        serviceUuids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            serviceUuids.add(new UUID(in.readLong(), in.readLong()));
        }
    }

    public LeScannerFilter() {
        serviceUuids = new ArrayList<>(4);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (checkLocalName ? 1 : 0));
        dest.writeString(localName);
        dest.writeByte((byte) (checkAddress ? 1 : 0));
        dest.writeString(address);
        dest.writeString(addressPattern != null ? addressPattern.pattern() : null);
        dest.writeByte((byte) (checkRssi ? 1 : 0));
        dest.writeInt(minRssi);
        dest.writeByte((byte) (checkServiceUuid ? 1 : 0));
        dest.writeByte((byte) (matchAnyServiceUuid ? 1 : 0));

        dest.writeInt(serviceUuids.size());
        for (UUID uuid : serviceUuids) {
            dest.writeLong(uuid.getMostSignificantBits());
            dest.writeLong(uuid.getLeastSignificantBits());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LeScannerFilter> CREATOR = new Creator<LeScannerFilter>() {
        @Override
        public LeScannerFilter createFromParcel(Parcel in) {
            return new LeScannerFilter(in);
        }

        @Override
        public LeScannerFilter[] newArray(int size) {
            return new LeScannerFilter[size];
        }
    };

    @Override
    public boolean match(LeScannerReport report) {
        if (checkLocalName) {
            String name = report.advData.getLocalName();
            if (localName == null || name == null || !name.contains(this.localName)) {
                return false;
            }
        }

        if (checkAddress) {
            if (report.address == null || (address == null && addressPattern == null)) {
                return false;
            }
            if (address != null && !report.address.contains(address)) {
                return false;
            }
            if (addressPattern != null && !addressPattern.matcher(report.address).matches()) {
                return false;
            }
        }

        if (checkRssi && report.rssi < minRssi) {
            return false;
        }

        if (checkServiceUuid) {
            ArrayList<UUID> uuids = this.serviceUuids;
            List<UUID> peerUuids = report.advData.getServiceUuids();
            if (uuids == null || peerUuids == null) {
                return false;
            }

            int matchCnt = 0;
            for (UUID uuid : uuids) {
                if (peerUuids.contains(uuid)) {
                    if (matchAnyServiceUuid) {
                        return true;
                    } else {
                        matchCnt++;
                    }
                }
            }

            return matchCnt == uuids.size();
        }

        return true;
    }
}
