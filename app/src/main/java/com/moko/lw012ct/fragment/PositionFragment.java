package com.moko.lw012ct.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.moko.ble.lib.task.OrderTask;
import com.moko.lw012ct.R;
import com.moko.lw012ct.activity.DeviceInfoActivity;
import com.moko.lw012ct.databinding.Lw012FragmentPosBinding;
import com.moko.support.lw012ct.LoRaLW012CTMokoSupport;
import com.moko.support.lw012ct.OrderTaskAssembler;

import java.util.ArrayList;

public class PositionFragment extends Fragment {
    private static final String TAG = PositionFragment.class.getSimpleName();
    private Lw012FragmentPosBinding mBind;
    private DeviceInfoActivity activity;
    private boolean mExtremeModeEnable;
    private boolean mVoltageReportEnable;

    public PositionFragment() {
    }


    public static PositionFragment newInstance() {
        PositionFragment fragment = new PositionFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        mBind = Lw012FragmentPosBinding.inflate(inflater, container, false);
        activity = (DeviceInfoActivity) getActivity();
        return mBind.getRoot();
    }

    public void setExtremeModeEnable(int enable) {
        mExtremeModeEnable = enable == 1;
        mBind.ivGPSExtremeMode.setImageResource(mExtremeModeEnable ? R.drawable.lw012_ic_checked : R.drawable.lw012_ic_unchecked);
    }

    public void setVoltageReportEnable(int enable) {
        mVoltageReportEnable = enable == 1;
        mBind.ivVoltageReport.setImageResource(mVoltageReportEnable ? R.drawable.lw012_ic_checked : R.drawable.lw012_ic_unchecked);
    }



    public void changeExtremeMode() {
        mExtremeModeEnable = !mExtremeModeEnable;
        activity.showSyncingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setGPSExtremeModeL76C(mExtremeModeEnable ? 1 : 0));
        orderTasks.add(OrderTaskAssembler.getGPSExtremeModeL76());
        LoRaLW012CTMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }
    public void changeVoltageReport() {
        mVoltageReportEnable = !mVoltageReportEnable;
        activity.showSyncingProgressDialog();
        ArrayList<OrderTask> orderTasks = new ArrayList<>();
        orderTasks.add(OrderTaskAssembler.setVoltageReportEnable(mVoltageReportEnable ? 1 : 0));
        orderTasks.add(OrderTaskAssembler.getVoltageReportEnable());
        LoRaLW012CTMokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
    }
}
