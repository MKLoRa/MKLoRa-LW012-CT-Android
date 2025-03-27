package com.moko.lw012ct.activity.general;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.lw012ct.activity.BaseActivity;
import com.moko.lw012ct.databinding.Lw012ActivityAlarmFunctionBinding;
import com.moko.lw012ct.utils.ToastUtils;
import com.moko.support.lw012ct.LoRaLW012CTMokoSupport;
import com.moko.support.lw012ct.OrderTaskAssembler;
import com.moko.support.lw012ct.entity.OrderCHAR;
import com.moko.support.lw012ct.entity.ParamsKeyEnum;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlarmFunctionActivity extends BaseActivity {
    private Lw012ActivityAlarmFunctionBinding mBind;
    private boolean mReceiverTag;
    private int thresholdFlag;
    private int alarmEnableFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = Lw012ActivityAlarmFunctionBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        EventBus.getDefault().register(this);

        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
        showSyncingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>(4);
        orderTasks.add(OrderTaskAssembler.getTamperAlarmEnable());
        orderTasks.add(OrderTaskAssembler.getTamperAlarmThreshold());
        orderTasks.add(OrderTaskAssembler.getTamperAlarmReportInterval());
        LoRaLW012CTMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                finish();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 300)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (!MokoConstants.ACTION_CURRENT_DATA.equals(action))
            EventBus.getDefault().cancelEventDelivery(event);
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
            }
            if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
                dismissSyncProgressDialog();
            }
            if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
                OrderTaskResponse response = event.getResponse();
                OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
                byte[] value = response.responseValue;
                if (orderCHAR == OrderCHAR.CHAR_PARAMS) {
                    if (null != value && value.length >= 5) {
                        int header = value[0] & 0xFF;// 0xED
                        int flag = value[1] & 0xFF;// read or write
                        int cmd = MokoUtils.toInt(Arrays.copyOfRange(value, 2, 4));
                        ParamsKeyEnum configKeyEnum = ParamsKeyEnum.fromParamKey(cmd);
                        if (header != 0xED || null == configKeyEnum) return;
                        int length = value[4] & 0xFF;
                        if (flag == 0x01) {
                            // write
                            switch (configKeyEnum) {
                                case KEY_TAMPER_ALARM_ENABLE:
                                    alarmEnableFlag = value[5] & 0xff;
                                    break;
                                case KEY_TAMPER_ALARM_THRESHOLD:
                                    thresholdFlag = value[5] & 0xff;
                                    break;
                                case KEY_TAMPER_ALARM_REPORT_INTERVAL:
                                    if (thresholdFlag == 1 && alarmEnableFlag == 1 && (value[5] & 0xff) == 1) {
                                        ToastUtils.showToast(this, "Save Successfully！");
                                    } else {
                                        ToastUtils.showToast(this, "Opps！Save failed. Please check the input characters and try again.");
                                    }
                                    break;
                            }
                        }
                        if (flag == 0x00) {
                            // read
                            switch (configKeyEnum) {
                                case KEY_TAMPER_ALARM_ENABLE:
                                    if (length == 1) {
                                        int enable = value[5] & 0xff;
                                        mBind.cbTamperAlarmEnable.setChecked(enable == 1);
                                    }
                                    break;
                                case KEY_TAMPER_ALARM_THRESHOLD:
                                    if (length == 1) {
                                        int threshold = value[5] & 0xff;
                                        mBind.etLightThreshold.setText(String.valueOf(threshold));
                                    }
                                    break;
                                case KEY_TAMPER_ALARM_REPORT_INTERVAL:
                                    if (length == 2) {
                                        int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 5, 5 + length));
                                        mBind.etReportInterval.setText(String.valueOf(interval));
                                    }
                                    break;
                            }
                        }
                    }
                }
            }
        });
    }

    public void onSave(View view) {
        if (isWindowLocked()) return;
        if (isValid()) {
            showSyncingProgressDialog();
            saveParams();
        } else {
            ToastUtils.showToast(this, "Para error!");
        }
    }

    private void saveParams() {
        alarmEnableFlag = 0;
        thresholdFlag = 0;
        int threshold = Integer.parseInt(mBind.etLightThreshold.getText().toString());
        int interval = Integer.parseInt(mBind.etReportInterval.getText().toString());
        List<OrderTask> orderTasks = new ArrayList<>(3);
        orderTasks.add(OrderTaskAssembler.setTamperAlarmEnable(mBind.cbTamperAlarmEnable.isChecked() ? 1 : 0));
        orderTasks.add(OrderTaskAssembler.setTamperAlarmThreshold(threshold));
        orderTasks.add(OrderTaskAssembler.setTamperAlarmInterval(interval));
        LoRaLW012CTMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }

    private boolean isValid() {
        if (TextUtils.isEmpty(mBind.etLightThreshold.getText())) return false;
        String thresholdStr = mBind.etLightThreshold.getText().toString();
        int threshold = Integer.parseInt(thresholdStr);
        if (threshold < 10 || threshold > 200)
            return false;
        if (TextUtils.isEmpty(mBind.etReportInterval.getText())) return false;
        String intervalStr = mBind.etReportInterval.getText().toString();
        int interval = Integer.parseInt(intervalStr);
        return interval >= 1 && interval <= 14400;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    if (blueState == BluetoothAdapter.STATE_TURNING_OFF) {
                        dismissSyncProgressDialog();
                        finish();
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
            // 注销广播
            unregisterReceiver(mReceiver);
        }
        EventBus.getDefault().unregister(this);
    }

    public void onBack(View view) {
        finish();
    }
}
