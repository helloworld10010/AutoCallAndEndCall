package com.example.treesa.autocallandendcall;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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


import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends AppCompatActivity {
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
    private Button callingTime;
    private Button period;
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
        period = findViewById(R.id.period);
        callingTime = findViewById(R.id.calling_time);


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
                case R.id.period:
                    AlertDialog.Builder builder2 = new AlertDialog.Builder(MainActivity.this);
                    LayoutInflater inflater2 = getLayoutInflater();
                    View view = inflater2.inflate(R.layout.dialog_period,null);
                    builder2.setView(view);
                    builder2.setPositiveButton("确定", (dialog, which) -> {
                        EditText input = view.findViewById(R.id.input);
                        String inputNumber = input.getText().toString();
                        int period2 = Integer.valueOf(inputNumber);
                        if(period2>10 || period2<1){
                            Toast.makeText(this, "超过范围", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        SharedPreferences sp = getSharedPreferences("auto_call", Context.MODE_PRIVATE);
                        SharedPreferences.Editor edit = sp.edit();
                        edit.putInt("period",Integer.valueOf(inputNumber));
                        edit.commit();
                        Toast.makeText(MainActivity.this, period2+"小时/次，周期修改后重启生效", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    });
                    builder2.create().show();
                    break;
                case R.id.calling_time:
                    AlertDialog.Builder builder3 = new AlertDialog.Builder(MainActivity.this);
                    LayoutInflater inflater3 = getLayoutInflater();
                    View view2 = inflater3.inflate(R.layout.dialog_callling_time,null);
                    builder3.setView(view2);
                    builder3.setPositiveButton("确定", (dialog, which) -> {
                        EditText input = view2.findViewById(R.id.input);
                        String inputNumber = input.getText().toString();
                        int period2 = Integer.valueOf(inputNumber);
                        if(period2>30 || period2<5){
                            Toast.makeText(this, "超过范围", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        SharedPreferences sp = getSharedPreferences("auto_call", Context.MODE_PRIVATE);
                        SharedPreferences.Editor edit = sp.edit();
                        edit.putInt("calling_time",Integer.valueOf(inputNumber));
                        edit.commit();
                        Toast.makeText(MainActivity.this, period2+"秒/次，修改后重启生效", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    });
                    builder3.create().show();
                    break;
                case R.id.btn_phone2:
                    Toast.makeText(MainActivity.this,"开始工作",Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(() -> {
                        new Clock(list).start();
                        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        Intent intent = new Intent();
                        intent.setAction("action.CALL_EVERY_PHONE_NUMBER");
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this,
                                100, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                        // 读取sp中周期变量
                        SharedPreferences sp = getSharedPreferences("auto_call", Context.MODE_PRIVATE);
                        int auto_call = sp.getInt("auto_call", 2);
                        // rtc 从1970开始唤醒cpu
                        alarm.setRepeating(AlarmManager.RTC_WAKEUP,
                                5000, 60*1000*60*auto_call, pendingIntent);

                        receiver = new AlarmBroadcastReceiver();
                        registerReceiver(receiver,new IntentFilter("action.CALL_EVERY_PHONE_NUMBER"));
                    },1000*5);
                    break;
                default:
            }
        };

        /**
         * position对应集合中的数据
         * 从集合中获取id，从数据库中删除
         */
        adapter.setOnItemChildClickListener((adapter, view, position) -> {
            Phones phones = list.get(position);
            phonesDao.delete(phones);
            list.remove(position);
            adapter.notifyDataSetChanged();
            Toast.makeText(MainActivity.this,"删除成功",Toast.LENGTH_SHORT).show();
        });
        add.setOnClickListener(clickListener);
        start.setOnClickListener(clickListener);
        period.setOnClickListener(clickListener);
        callingTime.setOnClickListener(clickListener);

    }

    public void callAndEndCall(String phone){
        SharedPreferences sp = getSharedPreferences("auto_call", Context.MODE_PRIVATE);
        int calling_time = sp.getInt("calling_time", 10);
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
            }, calling_time * 1000);
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
    }

    class AlarmBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("AlarmBroadcastReceiver","onReceive 启动打电话线程 -==-=--=-=--=-");
            new Clock(list).start();
        }
    }

    class Clock extends Thread {

        List<Phones> phones;
        public Clock(List<Phones> phones){
            this.phones = phones;
        }
        @Override
        public void run() {
                for(Phones phone:phones){
                    Message obtain = Message.obtain();
                    obtain.obj = phone.getNumber();
                    handler.sendMessage(obtain);
                    SystemClock.sleep(20*1000);
                }
        }
    }
}
