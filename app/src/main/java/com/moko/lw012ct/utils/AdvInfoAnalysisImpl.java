package com.moko.lw012ct.utils;

import android.os.ParcelUuid;
import android.os.SystemClock;

import com.moko.ble.lib.utils.MokoUtils;
import com.moko.lw012ct.entity.AdvInfo;
import com.moko.support.lw012ct.entity.DeviceInfo;
import com.moko.support.lw012ct.service.DeviceInfoParseable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import no.nordicsemi.android.support.v18.scanner.ScanRecord;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class AdvInfoAnalysisImpl implements DeviceInfoParseable<AdvInfo> {
    private HashMap<String, AdvInfo> advInfoHashMap;

    public AdvInfoAnalysisImpl() {
        this.advInfoHashMap = new HashMap<>();
    }

    @Override
    public AdvInfo parseDeviceInfo(DeviceInfo deviceInfo) {
        ScanResult result = deviceInfo.scanResult;
        ScanRecord record = result.getScanRecord();
        Map<ParcelUuid, byte[]> map = record.getServiceData();
        if (map == null || map.isEmpty())
            return null;
        int deviceType = -1;
        int batteryPower = -1;
        int lowPowerState = -1;
        boolean verifyEnable = false;
        Iterator iterator = map.keySet().iterator();
        while (iterator.hasNext()) {
            ParcelUuid parcelUuid = (ParcelUuid) iterator.next();
            if (parcelUuid.toString().startsWith("0000aa17")) {
                byte[] bytes = map.get(parcelUuid);
                if (bytes != null) {
                    deviceType = bytes[0] & 0xFF;
                    lowPowerState = (bytes[1] >> 4) & 0x01;
                    verifyEnable = ((bytes[1] >> 5) & 0x01) == 0x01;
                    batteryPower = MokoUtils.toInt(Arrays.copyOfRange(bytes, 2, 4));
                }
            }
        }
        if (deviceType == -1)
            return null;
        AdvInfo advInfo;
        if (advInfoHashMap.containsKey(deviceInfo.mac)) {
            advInfo = advInfoHashMap.get(deviceInfo.mac);
            advInfo.name = deviceInfo.name;
            advInfo.rssi = deviceInfo.rssi;
            advInfo.lowPowerState = lowPowerState;
            advInfo.deviceType = deviceType;
            advInfo.batteryPower = batteryPower;
            long currentTime = SystemClock.elapsedRealtime();
            long intervalTime = currentTime - advInfo.scanTime;
            advInfo.intervalTime = intervalTime;
            advInfo.scanTime = currentTime;
            advInfo.txPower = record.getTxPowerLevel();
            advInfo.verifyEnable = verifyEnable;
            advInfo.connectable = result.isConnectable();
        } else {
            advInfo = new AdvInfo();
            advInfo.name = deviceInfo.name;
            advInfo.mac = deviceInfo.mac;
            advInfo.rssi = deviceInfo.rssi;
            advInfo.lowPowerState = lowPowerState;
            advInfo.deviceType = deviceType;
            advInfo.batteryPower = batteryPower;
            advInfo.scanTime = SystemClock.elapsedRealtime();
            advInfo.txPower = record.getTxPowerLevel();
            advInfo.verifyEnable = verifyEnable;
            advInfo.connectable = result.isConnectable();
            advInfoHashMap.put(deviceInfo.mac, advInfo);
        }

        return advInfo;
    }
}
