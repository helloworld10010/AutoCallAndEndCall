package com.example.treesa.autocallandendcall;

import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

class PhonesAdapter extends BaseQuickAdapter<Phones,BaseViewHolder> {

    public PhonesAdapter(int layoutResId, @Nullable List<Phones> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, Phones item) {
        helper.setText(R.id.phone,item.getNumber());
        helper.addOnClickListener(R.id.delete);
    }
}
