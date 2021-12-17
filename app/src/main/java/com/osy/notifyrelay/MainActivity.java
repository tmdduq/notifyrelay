package com.osy.notifyrelay;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    static boolean globalOnOff = true;

    ReplayConstraint rs;
    ScrollView sv;
    private NotificationReceiver receiver;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        rs = ReplayConstraint.getInstance();
        sv = findViewById(R.id.logScrollView);

        ((Button)findViewById(R.id.addSourceRoomButton)).setOnClickListener(view->{
            EditText e = findViewById(R.id.sourceRoomEditText);
            String s = e.getText().toString();
            if(s.length()<1) return;
            if(!rs.sourceRoom.contains(s)) rs.sourceRoom.add(s);
            e.setText("");
            printScrollText("source");
        });
        ((Button)findViewById(R.id.delSourceRoomButton)).setOnClickListener(view->{
            EditText e = findViewById(R.id.sourceRoomEditText);
            String s = e.getText().toString();
            if(s.length()<1) rs.sourceRoom.clear();
            for(int i = 0 ; i < rs.sourceRoom.size() ; i++)
                if(rs.sourceRoom.get(i).contains(s)) rs.sourceRoom.remove(i--);
            e.setText("");
            printScrollText("source");
        });
        ((Button)findViewById(R.id.addTargetRoomButton)).setOnClickListener(view->{
            EditText e= findViewById(R.id.targetRoomEditText);
            String s = e.getText().toString();
            if(s.length()<1) return;
            if(!rs.targetRoom.contains(s)) rs.targetRoom.add(s);
            e.setText("");
            printScrollText("target");
        });
        ((Button)findViewById(R.id.delTargetRoomButton)).setOnClickListener(view->{
            EditText e = findViewById(R.id.targetRoomEditText);
            String s = e.getText().toString();
            if(s.length()<1) rs.sourceRoom.clear();
            for(int i = 0 ; i < rs.targetRoom.size() ; i++)
                if(rs.targetRoom.get(i).contains(s)) rs.targetRoom.remove(i--);
            e.setText("");
            printScrollText("target");
        });
        ((Button)findViewById(R.id.addBigdataButton)).setOnClickListener(view->{
            String[] source = new String[]{"청공지","실서무","청서무"};
            String[] target = new String[]{"빅데터팀"};
            for(String s : source) if(!rs.sourceRoom.contains(s)) rs.sourceRoom.add(s);
            for(String t : target) if(!rs.targetRoom.contains(t)) rs.targetRoom.add(t);
            printScrollText("target");
            printScrollText("source");
        });
        ((EditText)findViewById(R.id.targetRoomEditText)).setOnKeyListener((v,keyCode,event)->{
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(((EditText)findViewById(R.id.targetRoomEditText)).getWindowToken(), 0);
                return true;
            }
            return false;
        });
        ((EditText)findViewById(R.id.sourceRoomEditText)).setOnKeyListener((v,keyCode,event)->{
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(((EditText)findViewById(R.id.sourceRoomEditText)).getWindowToken(), 0);
                return true;
            }
            return false;
        });
//        ((Button)findViewById(R.id.button3_set)).setOnClickListener(view->
 //               startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));

        receiver = new NotificationReceiver( );
        IntentFilter filter = new IntentFilter("com.osy.notifyrelay");
        registerReceiver(receiver,filter);
    }

    @Override
    protected void onResume() {
        printScrollText("source");
        printScrollText("target");
        super.onResume();
    }

    public void printScrollText(String type){
        TextView tv = null;
        StringBuilder sb = new StringBuilder();
        if(type == "source"){
            tv = findViewById(R.id.sourceRoomTextview);
            for(String s : rs.sourceRoom)
                sb.append(s+"\n");
        }
        if(type == "target"){
            tv = findViewById(R.id.targetRoomTextview);

            for(String t : rs.targetRoom)
                sb.append(t+"\n");
        }
        tv.setText(sb.toString());
        basicNotiCreate();
    }

    private void basicNotiCreate(){
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();// 이전에 있던 모든 Notification 알림 제거
        if(rs.targetRoom.size()<1 || rs.sourceRoom.size()<1) return;

        String NOTIFICATION_ID = "10001";
        String NOTIFICATION_NAME = "source to target notifyrelay";

        int IMPORTANCE = NotificationManager.IMPORTANCE_HIGH;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_ID, NOTIFICATION_NAME, IMPORTANCE);
            notificationManager.createNotificationChannel(channel);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0});
        }

        String source = "";
        for(String s : rs.sourceRoom) source += ", "+s;
        source = source.replaceFirst(", ","[") +"]";
        String target = "";
        for(String t : rs.targetRoom) target += ", "+t;
        target = target.replaceFirst(", ","[") +"]";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this,NOTIFICATION_ID)
                .setContentTitle(target+"으로 중계") //타이틀 TEXT
                .setContentText(source+"에서 발생하는 카톡을 중계합니다.") //세부내용 TEXT
                .setSmallIcon (R.drawable.ic_launcher_background) //필수 (안해주면 에러)
                .setOngoing(true)
                .setVibrate(new long[]{0});

        notificationManager.notify(0, builder.build());
    }



    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            String sb = intent.getStringExtra("sb");
            Log.i("MainActivity", " Broad Receive(sender/message) ");

            if(message.length()>20) message = message.substring(0,18)+"...";
            message = message.replaceAll("\n"," ");

            TextView tv = findViewById(R.id.logTextView);
            String timeStamp = new SimpleDateFormat("[hh:mm] ").format(Calendar.getInstance().getTime());
            tv.setText(timeStamp + sb +" >> "+message +"\n" +tv.getText());
//            sv.post(()->sv.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }
}