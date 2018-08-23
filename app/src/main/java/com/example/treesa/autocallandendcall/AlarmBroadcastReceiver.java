package com.example.treesa.autocallandendcall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;

public class AlarmBroadcastReceiver extends BroadcastReceiver {
        ArrayList<String> phones = new ArrayList<>();
        public AlarmBroadcastReceiver(){
            super();
            phones.add("15100059274");
            phones.add("17631384991");
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("AlarmBroadcastReceiver","onReceive 启动打电话线程 -==-=--=-=--=-");
        }
}
