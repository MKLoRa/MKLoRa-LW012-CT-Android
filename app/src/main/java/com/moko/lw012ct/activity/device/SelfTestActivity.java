package com.moko.lw012ct.activity.device;


import android.os.Bundle;
import android.view.View;

import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.lw012ct.activity.BaseActivity;
import com.moko.lw012ct.databinding.Lw012ActivitySelftestBinding;
import com.moko.lw012ct.dialog.AlertMessageDialog;
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

public class SelfTestActivity extends BaseActivity {

    private Lw012ActivitySelftestBinding mBind;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = Lw012ActivitySelftestBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        EventBus.getDefault().register(this);
        showSyncingProgressDialog();
        mBind.tvSelftestStatus.postDelayed(() -> {
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getSelfTestStatus());
            orderTasks.add(OrderTaskAssembler.getPCBAStatus());
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
                            if (flag == 0x00) {
                                // read
                                switch (configKeyEnum) {
                                    case KEY_SELFTEST_STATUS:
                                        if (length > 0) {
                                            int status = value[5] & 0xFF;
                                            mBind.tvSelftestStatus.setVisibility(status == 0 ? View.VISIBLE : View.GONE);
                                            if ((status & 0x01) == 0x01)
                                                mBind.tvGpsStatus.setVisibility(View.VISIBLE);
                                            if ((status & 0x02) == 0x02)
                                                mBind.tvAxisStatus.setVisibility(View.VISIBLE);
                                            if ((status & 0x04) == 0x04)
                                                mBind.tvFlashStatus.setVisibility(View.VISIBLE);
                                        }
                                        break;
                                    case KEY_PCBA_STATUS:
                                        if (length > 0) {
                                            mBind.tvPcbaStatus.setText(String.valueOf(value[5] & 0xFF));
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
