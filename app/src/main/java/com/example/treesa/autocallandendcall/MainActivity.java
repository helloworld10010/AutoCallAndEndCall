package com.example.treesa.autocallandendcall;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> phones = new ArrayList<>();
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            callAndEndCall((String)msg.obj);
        }
    };
    AlarmBroadcastReceiver receiver;
    PowerManager.WakeLock mWl;
    PowerManager mPm;
    private View.OnClickListener clickListener;
    private Button add;
    private Button start;
    private RecyclerView recyclerView;
    private PhonesAdapter adapter;
    private List<Phones> list;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        // get the note DAO
        DaoSession daoSession = ((CustomApp) getApplication()).getDaoSession();
        PhonesDao phonesDao = daoSession.getPhonesDao();
        list = phonesDao.queryBuilder()
                .list();

        start = findViewById(R.id.btn_phone2);
        add = findViewById(R.id.add);
        recyclerView = findViewById(R.id.recycle_view);

        mPm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWl = mPm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myservice");
        mWl.acquire(5000);

        adapter = new PhonesAdapter(R.layout.item_recycleview, list);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        clickListener = (View v) -> {
            switch (v.getId()){
                case R.id.add:
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    LayoutInflater inflater = getLayoutInflater();
                    View inflate = inflater.inflate(R.layout.dialog_signin,null);
                    builder.setView(inflate);
                    builder.setPositiveButton("确定", (dialog, which) -> {
                        EditText input = inflate.findViewById(R.id.input);
                        String inputNumber = input.getText().toString();
                        if(!TextUtils.isEmpty(inputNumber)){
                            Phones phones = new Phones();
                            phones.setNumber(inputNumber);
                            phonesDao.insert(phones);

                            List<Phones> newList = phonesDao.queryBuilder()
                                    .list();
                            for(Phones p:newList){
                                if(!list.contains(p)){
                                    list.add(p);
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }

                        Toast.makeText(MainActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                    builder.create().show();
                    break;
                case R.id.delete:
                    break;
                case R.id.btn_phone2:
                    Toast.makeText(MainActivity.this,"开始工作",Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(() -> {

                        new Clock(phones).start();

                        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        Intent intent = new Intent();
                        intent.setAction("action.CALL_EVERY_PHONE_NUMBER");
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this,
                                100, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        // rtc 从1970开始唤醒cpu
                        alarm.setRepeating(AlarmManager.RTC_WAKEUP,
                                5000, 60*1000*60*2, pendingIntent);

                        receiver = new AlarmBroadcastReceiver();
                        registerReceiver(receiver,new IntentFilter("action.CALL_EVERY_PHONE_NUMBER"));
                    },1000*5);
                    break;
            }
        };

        adapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                int id = view.getId();
                Toast.makeText(MainActivity.this,"删除 == id "+id,Toast.LENGTH_SHORT).show();
            }
        });
        add.setOnClickListener(clickListener);
        start.setOnClickListener(clickListener);

    }

    public void callAndEndCall(String phone){
        try {
            // 开始直接拨打电话
            Intent intent2 = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone));
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent2);
            Toast.makeText(MainActivity.this, "拨打电话！", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(() -> {
                Log.e("MainActivity","准备挂断电话 --- "+phone);
                try {
                    // 延迟5秒后自动挂断电话
                    // 首先拿到TelephonyManager
                    TelephonyManager telMag = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    Class<TelephonyManager> c = TelephonyManager.class;

                    // 再去反射TelephonyManager里面的私有方法 getITelephony 得到 ITelephony对象
                    Method mthEndCall = c.getDeclaredMethod("getITelephony", (Class[]) null);
                    //允许访问私有方法
                    mthEndCall.setAccessible(true);
                    final Object obj = mthEndCall.invoke(telMag, (Object[]) null);

                    // 再通过ITelephony对象去反射里面的endCall方法，挂断电话
                    Method mt = obj.getClass().getMethod("endCall");
                    //允许访问私有方法
                    mt.setAccessible(true);
                    mt.invoke(obj);
                    Toast.makeText(MainActivity.this, "挂断电话！", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 10 * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWl.isHeld()) {
            mWl.release();
        }
        if(receiver!=null)
            unregisterReceiver(receiver);
    }

    class AlarmBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("AlarmBroadcastReceiver","onReceive 启动打电话线程 -==-=--=-=--=-");
            new Clock(phones).start();
        }
    }

    class Clock extends Thread {

        ArrayList<String> phones;
        public Clock(ArrayList<String> phones){
            this.phones = phones;
        }
        @Override
        public void run() {
                for(String phone:phones){
                    Message obtain = Message.obtain();
                    obtain.obj = phone;
                    handler.sendMessage(obtain);
                    SystemClock.sleep(20*1000);
                }
        }
    }
}
