package com.timer;


import com.downloader.Logcat;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Dengjun on 2015/9/30.
 */
public class TimerInfo {

    private static final String TAG = "Timer_";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private String name; // 名字（唯一标识）
    private long triggerTime = 0; // 单位毫秒，triggerTime过后触发定时器
    private boolean cycle = false; // 是否是循环或者一次性的

    private long elapsed = 0; // 已经过了的时间
    private long lastTime = 0; // 上次计时时间

    private int triggerCount = 0;  // 已经触发了多少次

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTriggerTime() {
        return triggerTime;
    }

    public void setTriggerTime(long triggerTime) {
        this.triggerTime = triggerTime;
    }

    public boolean isCycle() {
        return cycle;
    }

    public void setCycle(boolean cycle) {
        this.cycle = cycle;
    }

    public long getElapsed() {
        return elapsed;
    }

    public void setElapsed(long elapsed) {
        if (elapsed < 0) {
            elapsed = 0;
        }
        this.elapsed = elapsed;
    }

    public long getLastTime() {
        return lastTime;
    }

    public void setLastTime(long lastTime) {
        this.lastTime = lastTime;
    }

    public int getTriggerCount() {
        return triggerCount;
    }

    public void setTriggerCount(int triggerCount) {
        this.triggerCount = triggerCount;
    }

    public static TimerInfo parseFromJson(JSONObject jsonObject) {
        TimerInfo info = new TimerInfo();
        try {
            info.setName(jsonObject.getString("n"));
            info.setTriggerTime(jsonObject.getLong("t"));
            info.setCycle(jsonObject.getInt("c") > 0); // cycle已被随机化
            info.setElapsed(jsonObject.getLong("e"));
            info.setLastTime(1000L * jsonObject.getLong("l"));
            info.setTriggerCount(jsonObject.getInt("tc"));

        } catch (JSONException e) {
            Logcat.e(TAG, "parse", e);
            info = null;
        }

        return info;
    }


    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("n", name);
            jsonObject.put("t", triggerTime);
            jsonObject.put("c", cycle ? 100 + System.currentTimeMillis() % 10 : 0);
            jsonObject.put("e", elapsed);
            jsonObject.put("l", lastTime / 1000);
            jsonObject.put("tc", triggerCount);
        } catch (JSONException e) {
            // e.printStackTrace(); should not happen
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        String lastTime = DATE_FORMAT.format(new Date(getLastTime()));
        return String.format("[%3d times] %s %4d -> %4d  last:%s [%s]",
                triggerCount, cycle ? "Y" : "N",
                elapsed, triggerTime, lastTime, name);
    }
}
