package com.moko.lw012ct.dialog;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.moko.lw012ct.R;
import com.moko.lw012ct.databinding.Lw012DialogLoadingBinding;
import com.moko.lw012ct.view.ProgressDrawable;

import androidx.core.content.ContextCompat;

public class LoadingDialog extends com.moko.lw012ct.dialog.MokoBaseDialog<Lw012DialogLoadingBinding> {
    public static final String TAG = LoadingDialog.class.getSimpleName();

    @Override
    protected Lw012DialogLoadingBinding getViewBind(LayoutInflater inflater, ViewGroup container) {
        return Lw012DialogLoadingBinding.inflate(inflater, container, false);
    }

    @Override
    protected void onCreateView() {
        ProgressDrawable progressDrawable = new ProgressDrawable();
        progressDrawable.setColor(ContextCompat.getColor(getContext(), R.color.lw012_text_black_4d4d4d));
        mBind.ivLoading.setImageDrawable(progressDrawable);
        progressDrawable.start();
    }

    @Override
    public int getDialogStyle() {
        return R.style.LW012CenterDialog;
    }

    @Override
    public int getGravity() {
        return Gravity.CENTER;
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public float getDimAmount() {
        return 0.7f;
    }

    @Override
    public boolean getCancelOutside() {
        return false;
    }

    @Override
    public boolean getCancellable() {
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((ProgressDrawable) mBind.ivLoading.getDrawable()).stop();
    }
}
