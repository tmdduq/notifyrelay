package com.osy.notifyrelay;

import android.content.Context;

import com.osy.util.LastTalk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReplayConstraint {
    final String TAG = "ReplyConstraint";
    Context context = null;
    private static ReplayConstraint instance = null;

    public Map<String, LastTalk> lt = new HashMap<>();

    public ArrayList<String> targetRoom = new ArrayList<>();
    public ArrayList<String> sourceRoom = new ArrayList<>();


    public static ReplayConstraint getInstance(){
        if(instance ==null) instance = new ReplayConstraint();
        return instance;
    }
    public void setContext(Context context){
        this.context = context;
    }



}
