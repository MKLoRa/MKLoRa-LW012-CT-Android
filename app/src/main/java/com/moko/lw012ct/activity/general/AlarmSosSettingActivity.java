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
import com.moko.lib.loraui.dialog.BottomDialog;
import com.moko.lw012ct.activity.BaseActivity;
import com.moko.lw012ct.databinding.Lw012ActivitySosAlarmSettingBinding;
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

public class AlarmSosSettingActivity extends BaseActivity {
    private Lw012ActivitySosAlarmSettingBinding mBind;
    private boolean mReceiverTag;
    private final ArrayList<String> mValues = new ArrayList<>(8);
    private final ArrayList<String> triggerMode = new ArrayList<>(6);
    private int mSelectedPos;
    private int mSelectedMode;
    private int modeFlag, posFlag, intervalFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = Lw012ActivitySosAlarmSettingBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        EventBus.getDefault().register(this);
        mValues.add("BLE");
        mValues.add("GPS");
        mValues.add("BLE+GPS");
        mValues.add("BLE*GPS");
        triggerMode.add("Double Click");
        triggerMode.add("Triple Click");
        triggerMode.add("Long Press 1s");
        triggerMode.add("Long Press 2s");
        triggerMode.add("Long Press 3s");
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
        showSyncingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>(4);
        orderTasks.add(OrderTaskAssembler.getAlarmSosTriggerType());
        orderTasks.add(OrderTaskAssembler.getAlarmSosPosStrategy());
        orderTasks.add(OrderTaskAssembler.getAlarmSosReportInterval());
        orderTasks.add(OrderTaskAssembler.getAlarmSosNotifyEnable());
        LoRaLW012CTMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));

        mBind.tvTriggerMode.setOnClickListener(v -> {
            if (isWindowLocked()) return;
            BottomDialog dialog = new BottomDialog();
            dialog.setDatas(triggerMode, mSelectedMode);
            dialog.setListener(value -> {
                mSelectedMode = value;
                mBind.tvTriggerMode.setText(triggerMode.get(value));
            });
            dialog.show(getSupportFragmentManager());
        });

        mBind.tvPosStrategy.setOnClickListener(v -> {
            if (isWindowLocked()) return;
            BottomDialog dialog = new BottomDialog();
            dialog.setDatas(mValues, mSelectedPos);
            dialog.setListener(value -> {
                mSelectedPos = value;
                mBind.tvPosStrategy.setText(mValues.get(value));
            });
            dialog.show(getSupportFragmentManager());
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 400)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        final String action = event.getAction();
        runOnUiThread(() -> {
            if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
                finish();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.POSTING, priority = 400)
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
                                case KEY_ALARM_SOS_TRIGGER_TYPE:
                                    modeFlag = value[5] & 0xff;
                                    break;

                                case KEY_ALARM_SOS_POS_STRATEGY:
                                    posFlag = value[5] & 0xff;
                                    break;

                                case KEY_ALARM_SOS_REPORT_INTERVAL:
                                    intervalFlag = value[5] & 0xff;
                                    break;
                                case KEY_ALARM_SOS_NOTIFY_ENABLE:
                                    if ((value[5] & 0xff) == 1 && modeFlag == 1 && posFlag == 1 && intervalFlag == 1) {
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
                                case KEY_ALARM_SOS_TRIGGER_TYPE:
                                    if (length == 1) {
                                        mSelectedMode = value[5] & 0xff;
                                        mBind.tvTriggerMode.setText(triggerMode.get(mSelectedMode));
                                    }
                                    break;

                                case KEY_ALARM_SOS_POS_STRATEGY:
                                    if (length == 1) {
                                        mSelectedPos = value[5] & 0xff;
                                        mBind.tvPosStrategy.setText(mValues.get(mSelectedPos));
                                    }
                                    break;

                                case KEY_ALARM_SOS_REPORT_INTERVAL:
                                    if (length == 2) {
                                        int interval = MokoUtils.toInt(Arrays.copyOfRange(value, 5, value.length));
                                        mBind.etReportInterval.setText(String.valueOf(interval));
                                        mBind.etReportInterval.setSelection(mBind.etReportInterval.getText().length());
                                    }
                                    break;

                                case KEY_ALARM_SOS_NOTIFY_ENABLE:
                                    if (length == 1) {
                                        int result = value[5] & 0xFF;
                                        mBind.cbNotifySosStart.setChecked((result & 0x01) == 1);
                                        mBind.cbNotifySosEnd.setChecked((result >> 1 & 0x01) == 1);
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
            modeFlag = 0;
            posFlag = 0;
            intervalFlag = 0;
            int type = (mBind.cbNotifySosStart.isChecked() ? 1 : 0) | (mBind.cbNotifySosEnd.isChecked() ? 2 : 0);
            int interval = Integer.parseInt(mBind.etReportInterval.getText().toString().trim());
            List<OrderTask> orderTasks = new ArrayList<>(4);
            orderTasks.add(OrderTaskAssembler.setAlarmSosTriggerType(mSelectedMode));
            orderTasks.add(OrderTaskAssembler.setAlarmSosPosStrategy(mSelectedPos));
            orderTasks.add(OrderTaskAssembler.setAlarmSosReportInterval(interval));
            orderTasks.add(OrderTaskAssembler.setAlarmSosNotifyEnable(type));
            LoRaLW012CTMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        } else {
            ToastUtils.showToast(this, "Para error!");
        }
    }

    private boolean isValid() {
        if (TextUtils.isEmpty(mBind.etReportInterval.getText())) return false;
        int interval = Integer.parseInt(mBind.etReportInterval.getText().toString());
        return interval >= 10 && interval <= 600;
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
