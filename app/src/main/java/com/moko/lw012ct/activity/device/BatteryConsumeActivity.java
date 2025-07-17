package com.moko.lw012ct.activity.device;


import android.os.Bundle;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.lib.loraui.dialog.AlertMessageDialog;
import com.moko.lw012ct.activity.BaseActivity;
import com.moko.lw012ct.databinding.Lw012ActivityBatteryConsumeBinding;
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

public class BatteryConsumeActivity extends BaseActivity {

    private Lw012ActivityBatteryConsumeBinding mBind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = Lw012ActivityBatteryConsumeBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        EventBus.getDefault().register(this);
        showSyncingProgressDialog();
        mBind.tvAdvTimes.postDelayed(() -> {
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getBatteryInfo());
            orderTasks.add(OrderTaskAssembler.getBatteryInfoAll());
            orderTasks.add(OrderTaskAssembler.getBatteryInfoLast());
            LoRaLW012CTMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }, 500);
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
                                    case KEY_BATTERY_RESET:
                                        if (result == 1) {
                                            AlertMessageDialog dialog = new AlertMessageDialog();
                                            dialog.setMessage("Reset Successfully！");
                                            dialog.setConfirm("OK");
                                            dialog.setCancelGone();
                                            dialog.show(getSupportFragmentManager());
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                // read
                                switch (configKeyEnum) {
                                    case KEY_BATTERY_INFO:
                                        if (length == 40) {
                                            int runtime = MokoUtils.toInt(Arrays.copyOfRange(value, 5, 9));
                                            mBind.tvRuntime.setText(String.format("%d s", runtime));
                                            int advTimes = MokoUtils.toInt(Arrays.copyOfRange(value, 9, 13));
                                            mBind.tvAdvTimes.setText(String.format("%d times", advTimes));
                                            int axisDuration = MokoUtils.toInt(Arrays.copyOfRange(value, 13, 17));
                                            mBind.tvAxisDuration.setText(String.format("%d ms", axisDuration));
                                            int bleFixDuration = MokoUtils.toInt(Arrays.copyOfRange(value, 17, 21));
                                            mBind.tvBleFixDuration.setText(String.format("%d ms", bleFixDuration));
                                            int gpsFixDuration = MokoUtils.toInt(Arrays.copyOfRange(value, 21, 25));
                                            mBind.tvGpsFixDuration.setText(String.format("%d s", gpsFixDuration));
                                            int loraTransmissionTimes = MokoUtils.toInt(Arrays.copyOfRange(value, 25, 29));
                                            mBind.tvLoraTransmissionTimes.setText(String.format("%d times", loraTransmissionTimes));
                                            int loraPower = MokoUtils.toInt(Arrays.copyOfRange(value, 29, 33));
                                            mBind.tvLoraPower.setText(String.format("%d mAS", loraPower));
                                            String batteryConsumeStr = MokoUtils.getDecimalFormat("0.###").format(MokoUtils.toInt(Arrays.copyOfRange(value, 33, 37)) * 0.001f);
                                            mBind.tvBatteryConsume.setText(String.format("%s mAH", batteryConsumeStr));
                                            int staticUploadNum = MokoUtils.toInt(Arrays.copyOfRange(value, 37, 41));
                                            mBind.tvStaticPosPayload.setText(String.format("%d  pieces of payload 1", staticUploadNum));
                                            int moveUploadNum = MokoUtils.toInt(Arrays.copyOfRange(value, 41, 45));
                                            mBind.tvMotionPosPayload.setText(String.format("%d  pieces of payload 2", moveUploadNum));
                                        }
                                        break;
                                    case KEY_BATTERY_INFO_ALL:
                                        if (length == 40) {
                                            int runtime = MokoUtils.toInt(Arrays.copyOfRange(value, 5, 9));
                                            mBind.tvRuntimeAll.setText(String.format("%d s", runtime));
                                            int advTimes = MokoUtils.toInt(Arrays.copyOfRange(value, 9, 13));
                                            mBind.tvAdvTimesAll.setText(String.format("%d times", advTimes));
                                            int axisDuration = MokoUtils.toInt(Arrays.copyOfRange(value, 13, 17));
                                            mBind.tvAxisDurationAll.setText(String.format("%d ms", axisDuration));
                                            int bleFixDuration = MokoUtils.toInt(Arrays.copyOfRange(value, 17, 21));
                                            mBind.tvBleFixDurationAll.setText(String.format("%d ms", bleFixDuration));
                                            int gpsFixDuration = MokoUtils.toInt(Arrays.copyOfRange(value, 21, 25));
                                            mBind.tvGpsFixDurationAll.setText(String.format("%d s", gpsFixDuration));
                                            int loraTransmissionTimes = MokoUtils.toInt(Arrays.copyOfRange(value, 25, 29));
                                            mBind.tvLoraTransmissionTimesAll.setText(String.format("%d times", loraTransmissionTimes));
                                            int loraPower = MokoUtils.toInt(Arrays.copyOfRange(value, 29, 33));
                                            mBind.tvLoraPowerAll.setText(String.format("%d mAS", loraPower));
                                            String batteryConsumeStr = MokoUtils.getDecimalFormat("0.###").format(MokoUtils.toInt(Arrays.copyOfRange(value, 33, 37)) * 0.001f);
                                            mBind.tvBatteryConsumeAll.setText(String.format("%s mAH", batteryConsumeStr));
                                            int staticUploadNum = MokoUtils.toInt(Arrays.copyOfRange(value, 37, 41));
                                            mBind.tvStaticPosPayloadAll.setText(String.format("%d  pieces of payload 1", staticUploadNum));
                                            int moveUploadNum = MokoUtils.toInt(Arrays.copyOfRange(value, 41, 45));
                                            mBind.tvMotionPosPayloadAll.setText(String.format("%d  pieces of payload 2", moveUploadNum));
                                        }
                                        break;
                                    case KEY_BATTERY_INFO_LAST:
                                        if (length == 40) {
                                            int runtime = MokoUtils.toInt(Arrays.copyOfRange(value, 5, 9));
                                            mBind.tvRuntimeLast.setText(String.format("%d s", runtime));
                                            int advTimes = MokoUtils.toInt(Arrays.copyOfRange(value, 9, 13));
                                            mBind.tvAdvTimesLast.setText(String.format("%d times", advTimes));
                                            int axisDuration = MokoUtils.toInt(Arrays.copyOfRange(value, 13, 17));
                                            mBind.tvAxisDurationLast.setText(String.format("%d ms", axisDuration));
                                            int bleFixDuration = MokoUtils.toInt(Arrays.copyOfRange(value, 17, 21));
                                            mBind.tvBleFixDurationLast.setText(String.format("%d ms", bleFixDuration));
                                            int gpsFixDuration = MokoUtils.toInt(Arrays.copyOfRange(value, 21, 25));
                                            mBind.tvGpsFixDurationLast.setText(String.format("%d s", gpsFixDuration));
                                            int loraTransmissionTimes = MokoUtils.toInt(Arrays.copyOfRange(value, 25, 29));
                                            mBind.tvLoraTransmissionTimesLast.setText(String.format("%d times", loraTransmissionTimes));
                                            int loraPower = MokoUtils.toInt(Arrays.copyOfRange(value, 29, 33));
                                            mBind.tvLoraPowerLast.setText(String.format("%d mAS", loraPower));
                                            String batteryConsumeStr = MokoUtils.getDecimalFormat("0.###").format(MokoUtils.toInt(Arrays.copyOfRange(value, 33, 37)) * 0.001f);
                                            mBind.tvBatteryConsumeLast.setText(String.format("%s mAH", batteryConsumeStr));
                                            int staticUploadNum = MokoUtils.toInt(Arrays.copyOfRange(value, 37, 41));
                                            mBind.tvStaticPosPayloadLast.setText(String.format("%d  pieces of payload 1", staticUploadNum));
                                            int moveUploadNum = MokoUtils.toInt(Arrays.copyOfRange(value, 41, 45));
                                            mBind.tvMotionPosPayloadLast.setText(String.format("%d  pieces of payload 2", moveUploadNum));
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

    public void onBatteryReset(View view) {
        if (isWindowLocked())
            return;
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setTitle("Warning！");
        dialog.setMessage("Are you sure to reset battery?");
        dialog.setConfirm("OK");
        dialog.setOnAlertConfirmListener(() -> {
            showSyncingProgressDialog();
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.setBatteryReset());
            orderTasks.add(OrderTaskAssembler.getBatteryInfo());
            orderTasks.add(OrderTaskAssembler.getBatteryInfoAll());
            orderTasks.add(OrderTaskAssembler.getBatteryInfoLast());
            LoRaLW012CTMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        });
        dialog.show(getSupportFragmentManager());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


    public void onBack(View view) {
        backHome();
    }

    @Override
    public void onBackPressed() {
        backHome();
    }

    private void backHome() {
        finish();
    }
}
