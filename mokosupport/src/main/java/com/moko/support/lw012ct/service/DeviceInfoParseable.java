package com.moko.support.lw012ct.service;

import com.moko.support.lw012ct.entity.DeviceInfo;

public interface DeviceInfoParseable<T> {
    T parseDeviceInfo(DeviceInfo deviceInfo);
}
