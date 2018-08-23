package com.example.treesa.autocallandendcall;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    Button btn_phone2;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        btn_phone2 = findViewById(R.id.btn_phone2);

        phones.add("13235364220");
        phones.add("13061195162");
        phones.add("15650094105");
        phones.add("17125048412");

        btn_phone2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });
        mPm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWl = mPm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myservice");
        mWl.acquire(5000);
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
