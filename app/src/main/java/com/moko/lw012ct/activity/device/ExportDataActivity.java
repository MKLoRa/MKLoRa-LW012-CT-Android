package com.moko.lw012ct.activity.device;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.lib.loraui.dialog.AlertMessageDialog;
import com.moko.lw012ct.AppConstants;
import com.moko.lw012ct.R;
import com.moko.lw012ct.activity.BaseActivity;
import com.moko.lw012ct.activity.LoRaLW012CTMainActivity;
import com.moko.lw012ct.adapter.ExportDataListAdapter;
import com.moko.lw012ct.databinding.Lw012ActivityExportDataBinding;
import com.moko.lw012ct.utils.ToastUtils;
import com.moko.lw012ct.utils.Utils;
import com.moko.support.lw012ct.LoRaLW012CTMokoSupport;
import com.moko.support.lw012ct.OrderTaskAssembler;
import com.moko.support.lw012ct.entity.ExportData;
import com.moko.support.lw012ct.entity.OrderCHAR;
import com.moko.support.lw012ct.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import androidx.recyclerview.widget.LinearLayoutManager;

public class ExportDataActivity extends BaseActivity {

    private static final String TRACKED_FILE = "tracked.txt";

    private static String PATH_LOGCAT;
    private Lw012ActivityExportDataBinding mBind;
    private boolean mReceiverTag = false;
    private StringBuilder storeString;
    private ArrayList<ExportData> exportDatas;
    private boolean mIsSync;
    private ExportDataListAdapter adapter;
    private boolean mIsBack;
    private Handler mHandler;
    private boolean mIsStart;
    private int mStartTime;
    private int mSum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = Lw012ActivityExportDataBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        exportDatas = LoRaLW012CTMokoSupport.getInstance().exportDatas;
        storeString = LoRaLW012CTMokoSupport.getInstance().storeString;
        mStartTime = LoRaLW012CTMokoSupport.getInstance().startTime;
        mSum = LoRaLW012CTMokoSupport.getInstance().sum;
        if (exportDatas != null && exportDatas.size() > 0 && storeString != null) {
            mIsStart = true;
            if (mStartTime > 0) {
                mBind.etTime.setText(String.valueOf(mStartTime));
            }
            if (mSum > 0) {
                mBind.tvSum.setText(String.format("Sum:%d", mSum));
            }
            mBind.tvCount.setText(String.format("Count:%d", exportDatas.size()));
            mBind.tvExport.setEnabled(true);
            mBind.tvEmpty.setEnabled(true);
        } else {
            exportDatas = new ArrayList<>();
            storeString = new StringBuilder();
        }
        mHandler = new Handler();
        adapter = new ExportDataListAdapter();
        adapter.openLoadAnimation();
        adapter.replaceData(exportDatas);
        mBind.rvExportData.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvExportData.setAdapter(adapter);
        PATH_LOGCAT = LoRaLW012CTMainActivity.PATH_LOGCAT + File.separator + TRACKED_FILE;
        EventBus.getDefault().register(this);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
        if (!LoRaLW012CTMokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            LoRaLW012CTMokoSupport.getInstance().enableBluetooth();
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                finish();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 200)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (!MokoConstants.ACTION_CURRENT_DATA.equals(action))
            EventBus.getDefault().cancelEventDelivery(event);
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_CURRENT_DATA.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {
                    case CHAR_STORAGE_DATA_NOTIFY:
                        final int length = value.length;
                        int header = value[0] & 0xFF;
                        int flag = value[1] & 0xFF;// notify
                        int cmd = MokoUtils.toInt(Arrays.copyOfRange(value, 2, 4));
                        int len = value[4] & 0xFF;
                        if (header == 0xED && flag == 0x02 && cmd == 0x01) {
                            int dataCount = value[5] & 0xFF;
                            if (dataCount > 0) {
                                Calendar calendar = Calendar.getInstance();
                                String time = Utils.calendar2strDate(calendar, AppConstants.PATTERN_YYYY_MM_DD_HH_MM_SS);
                                int index = 6;
                                while (index < length) {
                                    int dataLength = value[index] & 0xff;
                                    String rawData = "";
                                    if (dataLength > 0) {
                                        index += 1;
                                        byte[] rawDataBytes = Arrays.copyOfRange(value, index, index + dataLength);
                                        rawData = MokoUtils.bytesToHexString(rawDataBytes);
                                    }
                                    ExportData exportData = new ExportData();
                                    exportData.time = time;
                                    exportData.rawData = rawData;
                                    if (mStartTime == 65535) {
                                        exportDatas.add(0, exportData);
                                    } else {
                                        exportDatas.add(exportData);
                                    }
                                    mBind.tvCount.setText(String.format("Count:%d", exportDatas.size()));

                                    storeString.append(String.format("Time:%s", time));
                                    storeString.append("\n");
                                    if (!TextUtils.isEmpty(rawData)) {
                                        storeString.append(String.format("Raw Data:%s", rawData));
                                        storeString.append("\n");
                                    }
                                    storeString.append("\n");
                                    index += dataLength;
                                }
                                adapter.replaceData(exportDatas);
                            } else {
                                byte[] sumBytes = Arrays.copyOfRange(value, 5, length);
                                int sum = MokoUtils.toInt(sumBytes);
                                mBind.tvSum.setText(String.format("Sum:%d", sum));
                                LoRaLW012CTMokoSupport.getInstance().sum = sum;
                            }

                            if (mIsBack && !mIsSync) {
                                if (mHandler.hasMessages(0)) {
                                    mHandler.removeMessages(0);
                                    mHandler.postDelayed(() -> {
                                        dismissSyncProgressDialog();
                                        LoRaLW012CTMokoSupport.getInstance().exportDatas = exportDatas;
                                        LoRaLW012CTMokoSupport.getInstance().storeString = storeString;
                                        LoRaLW012CTMokoSupport.getInstance().startTime = mStartTime;
                                        finish();
                                    }, 2000);
                                }
                            }
                        }
                        break;
                }
            }
            if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
            }
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                if (!mIsBack) {
                    dismissSyncProgressDialog();
                }
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                int responseType = response.responseType;
                byte[] value = response.responseValue;
                switch (orderCHAR) {
                    case CHAR_PARAMS:
                        if (value.length >= 5) {
                            int header = value[0] & 0xFF;// 0xED
                            int flag = value[1] & 0xFF;// read or write
                            int cmd = MokoUtils.toInt(Arrays.copyOfRange(value, 2, 4));
                            if (header != 0xED)
                                return;
                            ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                            if (configKeyEnum == null) {
                                return;
                            }
                            int length = value[4] & 0xFF;
                            if (flag == 0x01) {
                                // write
                                int result = value[5] & 0xFF;
                                switch (configKeyEnum) {
                                    case KEY_CLEAR_STORAGE_DATA:
                                        if (result != 1) {
                                            ToastUtils.showToast(this, "Failed");
                                        } else {
                                            storeString = new StringBuilder();
                                            writeTrackedFile("");
                                            exportDatas.clear();
                                            adapter.replaceData(exportDatas);
                                            mBind.tvExport.setEnabled(false);
                                            mBind.tvSum.setText("Sum:0");
                                            mBind.tvCount.setText("Count:0");
                                            ToastUtils.showToast(this, "Empty success!");
                                        }
                                        break;
                                    case KEY_SYNC_ENABLE:
                                        if (result != 1) {
                                            ToastUtils.showToast(this, "Failed");
                                        } else {
                                            if (mIsBack) {
                                                mHandler.postDelayed(() -> {
                                                    dismissSyncProgressDialog();
                                                    LoRaLW012CTMokoSupport.getInstance().exportDatas = exportDatas;
                                                    LoRaLW012CTMokoSupport.getInstance().storeString = storeString;
                                                    LoRaLW012CTMokoSupport.getInstance().startTime = mStartTime;
                                                    finish();
                                                }, 2000);
                                            }
                                            if (!mIsSync) {
                                                mIsSync = true;
                                                mBind.tvEmpty.setEnabled(false);
                                                mBind.tvExport.setEnabled(false);
                                                Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
                                                mBind.ivSync.startAnimation(animation);
                                                mBind.tvSync.setText("Stop");
                                            } else {
                                                mIsSync = false;
                                                mBind.tvStart.setEnabled(true);
                                                if (exportDatas != null && exportDatas.size() > 0 && storeString != null) {
                                                    mBind.tvEmpty.setEnabled(true);
                                                    mBind.tvExport.setEnabled(true);
                                                }
                                                mBind.ivSync.clearAnimation();
                                                mBind.tvSync.setText("Sync");
                                            }
                                        }
                                        break;
                                    case KEY_READ_STORAGE_DATA:
                                        if (result != 1) {
                                            ToastUtils.showToast(this, "Failed");
                                        } else {
                                            mIsStart = true;
                                            mIsSync = true;
                                            mBind.tvStart.setEnabled(false);
                                            mBind.tvEmpty.setEnabled(false);
                                            mBind.tvExport.setEnabled(false);
                                            Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
                                            mBind.ivSync.startAnimation(animation);
                                            mBind.tvSync.setText("Stop");
                                        }
                                        break;
                                }
                            }
                        }
                        break;
                }
            }
        });
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            dismissSyncProgressDialog();
                            finish();
                            break;
                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiverTag) {
            mReceiverTag = false;
            unregisterReceiver(mReceiver);
        }
        EventBus.getDefault().unregister(this);
    }

    private void back() {
        if (mIsSync) {
            mIsBack = true;
            showSyncingProgressDialog();
            LoRaLW012CTMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setSyncEnable(0));
            return;
        }
        LoRaLW012CTMokoSupport.getInstance().exportDatas = exportDatas;
        LoRaLW012CTMokoSupport.getInstance().storeString = storeString;
        LoRaLW012CTMokoSupport.getInstance().startTime = mStartTime;
        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            back();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onStart(View view) {
        if (isWindowLocked())
            return;
        final String timeStr = mBind.etTime.getText().toString();
        if (TextUtils.isEmpty(timeStr)) {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
            return;
        }
        int time = Integer.parseInt(timeStr);
        if (time < 1 || time > 65535) {
            ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
            return;
        }
        if (LoRaLW012CTMokoSupport.getInstance().exportDatas != null) {
            LoRaLW012CTMokoSupport.getInstance().exportDatas.clear();
            LoRaLW012CTMokoSupport.getInstance().storeString = null;
            LoRaLW012CTMokoSupport.getInstance().startTime = 0;
            LoRaLW012CTMokoSupport.getInstance().sum = 0;
        }
        mStartTime = time;
        storeString = new StringBuilder();
        writeTrackedFile("");
        exportDatas.clear();
        adapter.replaceData(exportDatas);
        mBind.tvSum.setText("Sum:N/A");
        mBind.tvCount.setText("Count:0");
        showSyncingProgressDialog();
        LoRaLW012CTMokoSupport.getInstance().sendOrder(OrderTaskAssembler.readStorageData(time));
    }

    public void onSync(View view) {
        if (!mIsStart)
            return;
        if (isWindowLocked())
            return;
        showSyncingProgressDialog();
        if (!mIsSync) {
            LoRaLW012CTMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setSyncEnable(1));
        } else {
            LoRaLW012CTMokoSupport.getInstance().sendOrder(OrderTaskAssembler.setSyncEnable(0));
        }
    }

    public void onEmpty(View view) {
        if (isWindowLocked())
            return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning!");
        dialog.setMessage("Are you sure to empty the saved tracked datas?");
        dialog.setOnAlertConfirmListener(() -> {
            showSyncingProgressDialog();
            LoRaLW012CTMokoSupport.getInstance().sendOrder(OrderTaskAssembler.clearStorageData());
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onExport(View view) {
        if (isWindowLocked())
            return;
        showSyncingProgressDialog();
        writeTrackedFile("");
        mBind.tvExport.postDelayed(() -> {
            dismissSyncProgressDialog();
            final String log = storeString.toString();
            if (!TextUtils.isEmpty(log)) {
                writeTrackedFile(log);
                File file = getTrackedFile();
                // 发送邮件
                String address = "Development@mokotechnology.com";
                String title = "Tracked Log";
                String content = title;
                Utils.sendEmail(ExportDataActivity.this, address, content, title, "Choose Email Client", file);
            }
        }, 500);
    }

    public void onBack(View view) {
        back();
    }


    public static void writeTrackedFile(String thLog) {
        File file = new File(PATH_LOGCAT);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(thLog);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File getTrackedFile() {
        File file = new File(PATH_LOGCAT);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
