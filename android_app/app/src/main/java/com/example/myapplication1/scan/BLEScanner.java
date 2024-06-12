package com.example.myapplication1.scan;

import android.os.ParcelUuid;

import androidx.annotation.Nullable;


public interface BLEScanner {
	/**
	 * After the buttonless jump from the application mode to the bootloader mode the service
	 * will wait this long for the advertising bootloader (in milliseconds).
	 */
	long TIMEOUT = 20_000L; // ms

	@Nullable
	void searchForAddress(final String deviceAddress, final BLEScanCallback scanCallback);

	@Nullable
	void searchForName(final String devcieName, final BLEScanCallback scanCallback);

	@Nullable
	void searchForServiceUUID(final ParcelUuid uuid, final BLEScanCallback scanCallback);

	@Nullable
	void searchAll(final BLEScanCallback scanCallback);

	@Nullable
	void stopScan();
}
