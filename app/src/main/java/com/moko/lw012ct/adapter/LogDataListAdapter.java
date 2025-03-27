package com.moko.lw012ct.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.moko.lw012ct.R;
import com.moko.lw012ct.entity.LogData;

public class LogDataListAdapter extends BaseQuickAdapter<LogData, BaseViewHolder> {

    public LogDataListAdapter() {
        super(R.layout.lw012_item_log_data);
    }

    @Override
    protected void convert(BaseViewHolder helper, LogData item) {
        helper.setText(R.id.tv_time, item.name);
        helper.setImageResource(R.id.iv_checked, item.isSelected ? R.drawable.ic_selected : R.drawable.ic_unselected);
    }
}
