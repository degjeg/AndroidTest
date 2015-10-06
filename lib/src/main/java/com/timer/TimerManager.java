package com.timer;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;

import com.downloader.Logcat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Dengjun on 2015/9/30.
 */
public class TimerManager {

    private static final String TAG = "Timer_";

    public static final int HEARTBEAT_CYCLE = 1;

    File timerInfoFile;
    private static TimerManager instance;

    HashMap<String, TimerInfo> timerInfoHashMap;
    HashMap<String, TimerCallback> callbackHashMap;
    long diffOfSysTimeAndElapsedTime;

    boolean isInit = false;

    HandlerThread daemonThread;
    Handler handler;


    public TimerManager() {
        timerInfoHashMap = new HashMap<>();
        callbackHashMap = new HashMap<>();
        checkDaemonThreadStatus();

    }

    public static TimerManager getInstance() {
        if (instance == null) {
            synchronized (TimerManager.class) {
                instance = new TimerManager();
            }
        }
        return instance;
    }

    private void checkDaemonThreadStatus() {
        if (daemonThread != null && daemonThread.isAlive() && !daemonThread.isInterrupted()) {
            return;
        }

        if (daemonThread != null) {
            daemonThread.quit();
        }

        daemonThread = new HandlerThread("TimerDaemon");
        daemonThread.start();
        handler = new Handler(daemonThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                long start = SystemClock.elapsedRealtime();
                heartBeat();
                long used = SystemClock.elapsedRealtime() - start;
                long nextTime = (used < HEARTBEAT_CYCLE * 1000) ? HEARTBEAT_CYCLE * 1000 - used : 1;

                handler.removeMessages(1);

                if (needRun()) {
                    handler.sendEmptyMessageDelayed(1, nextTime);
                }

                return true;
            }
        });

        handler.sendEmptyMessageDelayed(1, 60000);
    }

    public void init(Context context) {
        checkDaemonThreadStatus();

        if (isInit) {
            return;
        }

        isInit = true;

        diffOfSysTimeAndElapsedTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();

        timerInfoFile = new File(context.getFilesDir(), String.format("timer_%s.dat", getClass().getName().hashCode()));
        if (!timerInfoFile.exists() || timerInfoFile.length() < 10) {
            return;
        }
        try {
            String jsonTimerString = FileUtil.loadFileToString(timerInfoFile);
            JSONObject jsonObject = new JSONObject(jsonTimerString);

            long oldDiffOfSysTimeAndElapsedTime = jsonObject.getLong("diff");
            boolean timeChanged = Math.abs(oldDiffOfSysTimeAndElapsedTime - diffOfSysTimeAndElapsedTime) > 5 * 60 * 1000;
            Logcat.e(TAG, "changed:" + timeChanged);

            if (!jsonObject.has("ts")) {
                return;
            }
            JSONArray timersNode = jsonObject.getJSONArray("ts");

            for (int i = (timersNode.length() - 1); i >= 0; i--) {
                JSONObject timerNode = timersNode.getJSONObject(i);
                TimerInfo timerInfo = TimerInfo.parseFromJson(timerNode);

                if (timerInfo != null) {
                    if (timeChanged) { // 如果时间已经被修改过
                        timerInfo.setLastTime(0); // 使次的时间失效
                    }

                    Logcat.d(TAG, timerInfo.toString());
                    timerInfoHashMap.put(timerInfo.getName(), timerInfo);
                }
            }
        } /*catch (IOException e) {
            // e.printStackTrace();
            Logcat.e(TAG, "init", e);
        } */ catch (JSONException e) {
            // e.printStackTrace();
            Logcat.e(TAG, "init", e);
        }

        handler.sendEmptyMessage(1);
    }

    public void flush() {
        if (timerInfoHashMap.isEmpty()) {
            return;
        }

        JSONObject jsonObject = new JSONObject();
        long tmpDiffOfSysTimeAndElapsedTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        try {
            jsonObject.put("diff", tmpDiffOfSysTimeAndElapsedTime);
            JSONArray timersArray = new JSONArray();

            int index = 0;
            for (TimerInfo info : timerInfoHashMap.values()) {
                timersArray.put(index++, info.toJson());
            }
            jsonObject.put("ts", timersArray);
            FileUtil.saveToFile(timerInfoFile, jsonObject.toString());
        } catch (JSONException e) {
            //e.printStackTrace(); // should not happen
        }
    }

    public boolean needRun() {
        Iterator<TimerInfo> iterator = timerInfoHashMap.values().iterator();
        while (iterator.hasNext()) {
            TimerInfo oneTimerInfo = iterator.next();
            if (oneTimerInfo.getTriggerCount() == 0 // 还有未执行完的
                    || oneTimerInfo.isCycle()) { // 有循环执行的
                return true;
            }
        }

        return false;
    }

    /**
     * 需要有地方周期性的调用此方法
     */
    public void heartBeat() {
        Iterator<TimerInfo> iterator = timerInfoHashMap.values().iterator();

        long tmpDiffOfSysTimeAndElapsedTime = System.currentTimeMillis() - SystemClock.elapsedRealtime();

        boolean timeChanged = Math.abs(diffOfSysTimeAndElapsedTime - tmpDiffOfSysTimeAndElapsedTime) > 5 * 60 * 1000;

        if (timeChanged) {
            Logcat.e(TAG, "changed 1:" + timeChanged);
            diffOfSysTimeAndElapsedTime = tmpDiffOfSysTimeAndElapsedTime;
        }
        while (iterator.hasNext()) {
            TimerInfo oneTimerInfo = iterator.next();

            if (!oneTimerInfo.isCycle() && oneTimerInfo.getTriggerCount() > 0) { // 已经完成的timer
                continue;
            }

            if (timeChanged || oneTimerInfo.getLastTime() == 0) { // 时间已被修改，timerinfo的last
                oneTimerInfo.setElapsed(oneTimerInfo.getElapsed() + HEARTBEAT_CYCLE);
            } else {
                long elapsedSeconds = (SystemClock.elapsedRealtime() - oneTimerInfo.getLastTime()) / 1000;
                oneTimerInfo.setElapsed(oneTimerInfo.getElapsed() + elapsedSeconds);
            }
            oneTimerInfo.setLastTime(SystemClock.elapsedRealtime());

            if (oneTimerInfo.getElapsed() >= oneTimerInfo.getTriggerTime()) { // 触发定时器
                oneTimerInfo.setElapsed(0);
                oneTimerInfo.setLastTime(0);
                oneTimerInfo.setTriggerCount(oneTimerInfo.getTriggerCount() + 1);

                if (callbackHashMap.containsKey(oneTimerInfo.getName())) {
                    new Thread(new WorkThread(oneTimerInfo, callbackHashMap.get(oneTimerInfo.getName())))
                            .start();
                }
                if (!oneTimerInfo.isCycle()) {
                    callbackHashMap.remove(oneTimerInfo.getName());
                }
            }
        }

        flush();
    }

    /**
     * @param name  定时器的名字，不可以重复，一个name只可使用一次
     * @param r     任务执行体
     * @param cycle 是否是循环或者一次性的
     * @paramlong triggerTime 单位分钟，triggerTime过后触发定时器
     */
    public void setTimer(String name, TimerCallback r, long triggerTime, boolean cycle) {
        TimerInfo info = timerInfoHashMap.get(name);
        if (info == null) {
            info = new TimerInfo();
            info.setLastTime(SystemClock.elapsedRealtime());

        }

        callbackHashMap.put(name, r);
        info.setName(name);
        info.setCycle(cycle);
        info.setTriggerTime(triggerTime);

        timerInfoHashMap.put(name, info);
        handler.sendEmptyMessageDelayed(1, 1000);
    }

    private class WorkThread implements Runnable {
        TimerCallback worker;
        TimerInfo info;

        public WorkThread(TimerInfo info, TimerCallback worker) {
            this.info = info;
            this.worker = worker;
        }

        @Override
        public void run() {
            worker.onTimer(info);
        }
    }
}
