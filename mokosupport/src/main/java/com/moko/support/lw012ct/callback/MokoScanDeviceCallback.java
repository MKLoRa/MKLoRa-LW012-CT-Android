package com.moko.support.lw012ct.callback;

import com.moko.support.lw012ct.entity.DeviceInfo;

public interface MokoScanDeviceCallback {
    void onStartScan();

    void onScanDevice(DeviceInfo device);

    void onStopScan();
}
