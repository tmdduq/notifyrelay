package com.osy.notifyrelay;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.osy.util.LastTalk;

import java.util.ArrayList;
import java.util.List;

public class NotifiService extends NotificationListenerService {
    final String TAG = "NotifiService";
    ReplayConstraint rs;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Log.i(TAG, "call onNotificationPosted");
        rs = ReplayConstraint.getInstance();
        rs.setContext(this);

        if(!sbn.getPackageName().contains("com.kakao.talk")){
            Log.i(TAG,"packName : "+sbn.getPackageName());
            return;
        }
        Log.i(TAG,"packName : "+sbn.getPackageName());
        new Thread( ()-> {
            Notification.WearableExtender wearableExtender =new Notification.WearableExtender(sbn.getNotification());
            List<Notification.Action> wearableAction = wearableExtender.getActions();

            for(Notification.Action act : wearableAction){
                if(act.getRemoteInputs() != null && act.getRemoteInputs().length>0){
                    replyString(getApplicationContext(), act, sbn.getNotification());
                    stopSelf();
                }
            }
        }).start();
    }

    public void replyString(Context context, Notification.Action act, Notification notification){
        try {
            String sender = notification.extras.getString("android.title");
            String roomName = notification.extras.getString("android.subText");
            if(roomName==null) roomName = sender;
            String message =  notification.extras.getString("android.text");

            LastTalk lastTalk = new LastTalk(context,act);

            for(String t : rs.targetRoom)
                if(roomName.matches(t)){
                    Log.i(TAG, "targetRoom Save - targetRoom: " + t);
                    rs.lt.put(roomName, lastTalk);
                }

            if(rs.targetRoom.size()<1){
                Log.i(TAG, "Not save targetRoom");
                return;
            }
            if(rs.sourceRoom.size()<1){
                Log.i(TAG, "NOT SourceRoom - roomName: " + roomName);
                return;
            }
            if(message.matches("사진을 보냈습니다.")) return;
            if(message.matches("이모티콘을 보냈습니다.")) return;

            StringBuilder sb = new StringBuilder();
            for(String s : rs.sourceRoom)
                if(roomName.matches(s)){
                    for(String t : rs.targetRoom) {
                        sb.append(roomName +"->"+t+"\n");
                        LastTalk targetRoom = rs.lt.get(t);
                        String message2 = "["+roomName+"]\n"+message;
                        sendMessage(targetRoom.getContext(), targetRoom.getAct(), message2);
                    }
                }

            Intent intent = new Intent("com.osy.notifyrelay");
            intent.putExtra("sb", sb.toString());
            intent.putExtra("message", message);
            sendBroadcast(intent);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();// 이전에 있던 모든 Notification 알림 제거
        super.onDestroy();
    }

    public void sendMessage(Context context, Notification.Action act, String message){
        Intent sendIntent = new Intent();
        Bundle msg = new Bundle();
        for (RemoteInput inputable : act.getRemoteInputs())
            msg.putCharSequence(inputable.getResultKey(), message);
        RemoteInput.addResultsToIntent(act.getRemoteInputs(), sendIntent, msg);

        try {
            act.actionIntent.send(context, 0, sendIntent);
            Log.i(TAG,"send() to"+ message);
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

}
