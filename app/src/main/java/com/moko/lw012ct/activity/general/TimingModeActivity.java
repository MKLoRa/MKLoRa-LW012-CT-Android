package com.moko.lw012ct.activity.general;


import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.lib.loraui.dialog.BottomDialog;
import com.moko.lw012ct.R;
import com.moko.lw012ct.activity.BaseActivity;
import com.moko.lw012ct.adapter.TimePointAdapter;
import com.moko.lw012ct.databinding.Lw012ActivityTimingModeBinding;
import com.moko.lw012ct.entity.TimePoint;
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

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TimingModeActivity extends BaseActivity implements BaseQuickAdapter.OnItemChildClickListener {

    private Lw012ActivityTimingModeBinding mBind;
    private boolean mReceiverTag = false;
    private boolean savedParamsError;
    private ArrayList<String> mValues;
    private int mSelected;
    private ArrayList<TimePoint> mTimePoints;
    private TimePointAdapter mAdapter;
    private ArrayList<String> mHourValues;
    private ArrayList<String> mMinValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBind = Lw012ActivityTimingModeBinding.inflate(getLayoutInflater());
        setContentView(mBind.getRoot());
        mValues = new ArrayList<>();
        mValues.add("BLE");
        mValues.add("GPS");
        mValues.add("BLE+GPS");
        mValues.add("BLE*GPS");
        mHourValues = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            mHourValues.add(String.format("%02d", i));
        }
        mMinValues = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            mMinValues.add(String.format("%02d", i));
        }
        mTimePoints = new ArrayList<>();
        mAdapter = new TimePointAdapter(mTimePoints);
        // 开启滑动删除
//        mAdapter.enableSwipeItem();
//        mAdapter.setOnItemSwipeListener(onItemSwipeListener);
        mAdapter.setOnItemChildClickListener(this);
        mAdapter.openLoadAnimation();
        mBind.rvTimePoint.setLayoutManager(new LinearLayoutManager(this));
        mBind.rvTimePoint.setAdapter(mAdapter);

        TimingModeActivity.SwipeToDeleteCallback callback = new TimingModeActivity.SwipeToDeleteCallback();
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(mBind.rvTimePoint);

        EventBus.getDefault().register(this);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
        showSyncingProgressDialog();
        mBind.tvTimingPosStrategy.postDelayed(() -> {
            List<OrderTask> orderTasks = new ArrayList<>();
            orderTasks.add(OrderTaskAssembler.getTimePosStrategy());
            orderTasks.add(OrderTaskAssembler.getTimePosReportPoints());
            LoRaLW012CTMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
        }, 500);
    }


    //    OnItemSwipeListener onItemSwipeListener = new OnItemSwipeListener() {
//        @Override
//        public void onItemSwipeStart(RecyclerView.ViewHolder viewHolder, int pos) {
//        }
//
//        @Override
//        public void clearView(RecyclerView.ViewHolder viewHolder, int pos) {
//        }
//
//        @Override
//        public void onItemSwiped(RecyclerView.ViewHolder viewHolder, int pos) {
//            mTimePoints.remove(pos);
//            int size = mTimePoints.size();
//            for (int i = 1; i <= size; i++) {
//                TimeSegmentedPoint point = mTimePoints.get(i - 1);
//                point.name = String.format("Time Period %d", i);
//            }
//            mAdapter.replaceData(mTimePoints);
//        }
//
//        @Override
//        public void onItemSwipeMoving(Canvas canvas, RecyclerView.ViewHolder viewHolder, float v, float v1, boolean b) {
//        }
//    };

    public class SwipeToDeleteCallback extends ItemTouchHelper.Callback {

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            // 不支持拖动
            int dragFlags = 0;
            // 支持从右向左滑动
            int swipeFlags = ItemTouchHelper.START;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            mTimePoints.remove(position);
            int size = mTimePoints.size();
            for (int i = 1; i <= size; i++) {
                TimePoint point = mTimePoints.get(i - 1);
                point.name = String.format("Time Period %d", i);
            }
            mAdapter.replaceData(mTimePoints);
        }
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
                                    case KEY_TIME_MODE_POS_STRATEGY:
                                        if (result != 1) {
                                            savedParamsError = true;
                                        }
                                        break;
                                    case KEY_TIME_MODE_REPORT_TIME_POINT:
                                        if (result != 1) {
                                            savedParamsError = true;
                                        }
                                        if (savedParamsError) {
                                            ToastUtils.showToast(TimingModeActivity.this, "Opps！Save failed. Please check the input characters and try again.");
                                        } else {
                                            ToastUtils.showToast(this, "Save Successfully！");
                                        }
                                        break;
                                }
                            }
                            if (flag == 0x00) {
                                // read
                                switch (configKeyEnum) {
                                    case KEY_TIME_MODE_POS_STRATEGY:
                                        if (length > 0) {
                                            int strategy = value[5] & 0xFF;
                                            mSelected = strategy;
                                            mBind.tvTimingPosStrategy.setText(mValues.get(mSelected));
                                        }
                                        break;
                                    case KEY_TIME_MODE_REPORT_TIME_POINT:
                                        if (length > 0) {
                                            byte[] rawBytes = Arrays.copyOfRange(value, 5, 5 + length);
                                            for (int i = 0; i < length; i += 2) {
                                                int index = i / 2;
                                                int point = MokoUtils.toInt(Arrays.copyOfRange(rawBytes, i, i + 2));
                                                int hour = point / 60;
                                                int min = point % 60;
                                                TimePoint timePoint = new TimePoint();
                                                timePoint.name = String.format("Time Point %d", index + 1);
                                                if (hour == 24) {
                                                    timePoint.hour = String.format("%02d", 0);
                                                } else {
                                                    timePoint.hour = String.format("%02d", hour);
                                                }
                                                timePoint.min = String.format("%02d", min);
                                                mTimePoints.add(timePoint);
                                                mAdapter.replaceData(mTimePoints);
                                            }
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
            // 注销广播
            unregisterReceiver(mReceiver);
        }
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
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        if (isWindowLocked())
            return;
        TimePoint timePoint = (TimePoint) adapter.getItem(position);
        if (view.getId() == R.id.tv_point_hour) {
            int select = Integer.parseInt(timePoint.hour);
            BottomDialog dialog = new BottomDialog();
            dialog.setDatas(mHourValues, select);
            dialog.setListener(value -> {
                timePoint.hour = mHourValues.get(value);
                adapter.notifyItemChanged(position);
            });
            dialog.show(getSupportFragmentManager());
        }
        if (view.getId() == R.id.tv_point_min) {
            int select = Integer.parseInt(timePoint.min);
            BottomDialog dialog = new BottomDialog();
            dialog.setDatas(mMinValues, select);
            dialog.setListener(value -> {
                timePoint.min = mMinValues.get(value);
                adapter.notifyItemChanged(position);
            });
            dialog.show(getSupportFragmentManager());
        }
    }

    public void selectPosStrategy(View view) {
        if (isWindowLocked())
            return;
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mValues, mSelected);
        dialog.setListener(value -> {
            mSelected = value;
            mBind.tvTimingPosStrategy.setText(mValues.get(value));
        });
        dialog.show(getSupportFragmentManager());
    }

    public void onTimePointAdd(View view) {
        if (isWindowLocked())
            return;
        int size = mTimePoints.size();
        if (size >= 10) {
            ToastUtils.showToast(this, "You can set up to 10 time points!");
            return;
        }
        TimePoint timePoint = new TimePoint();
        timePoint.name = String.format("Time Point %d", size + 1);
        timePoint.hour = String.format("%02d", 0);
        timePoint.min = String.format("%02d", 0);
        mTimePoints.add(timePoint);
        mAdapter.replaceData(mTimePoints);
    }

    public void onSave(View view) {
        savedParamsError = false;
        showSyncingProgressDialog();
        List<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setTimePosStrategy(mSelected));
        ArrayList<Integer> points = new ArrayList<>();
        for (TimePoint point : mTimePoints) {
            int hour = Integer.parseInt(point.hour);
            int min = Integer.parseInt(point.min);
            if (hour == 0 && min == 0) {
                points.add(1440);
                continue;
            }
            points.add(hour * 60 + min);
        }
        // TODO: 2024/12/12 清除设置返回错误 
        orderTasks.add(OrderTaskAssembler.setTimePosReportPoints(points));
        LoRaLW012CTMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }
}
