package com.downloader.com;

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by danger on
 * 15/11/24.
 */
public class EventBus {

    public interface EventHandler {
        public void onGlobalEvent(String eventId, Object... args);
    }

    private class Caller implements Runnable {
        String eventId;
        Object[] args;

        public Caller(String eventId, Object[] args) {
            this.eventId = eventId;
            this.args = args;
        }

        @Override
        public void run() {
            callHandlers(eventId, args);
        }
    }

    private HashMap<String, ArrayList<WeakReference<EventHandler>>> listeners = new HashMap<>();
    private static EventBus eventBus = new EventBus();
    final Handler handler;

    public static EventBus get() {
        return eventBus;
    }

    public EventBus() {
        handler = new Handler(Looper.getMainLooper());
    }

    public void subScribe(EventHandler handler, String... event) {
        if (event == null || event.length == 0 || handler == null) {
            return;
        }

        for (String eventId : event) {
            ArrayList<WeakReference<EventHandler>> list = listeners.get(eventId);
            if (list == null) {
                list = new ArrayList<>();
                list.add(new WeakReference<EventHandler>(handler));
                listeners.put(eventId, list);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    WeakReference<EventHandler> oneHandler = list.get(i);
                    if (oneHandler.get() == null) { // 删除已经被翻译的节点
                        list.remove(i--);
                    } else if (oneHandler.equals(handler)) {
                        return;
                    }
                }
                list.add(new WeakReference<EventHandler>(handler));
            }
        }

    }

    public void post(String event, Object... args) {
        if (Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId()) { // 当前线程是ui线程，直接调用
            callHandlers(event, args);
        } else {
            handler.post(new Caller(event, args));
        }
    }

    private void callHandlers(String eventId, Object... args) {
        if (!listeners.containsKey(eventId)) {
            return;
        }
        ArrayList<WeakReference<EventHandler>> list = listeners.get(eventId);
        for (int i = 0; i < list.size(); i++) {
            WeakReference<EventHandler> oneHandler = list.get(i);
            if (oneHandler.get() == null) {
                list.remove(i--);
            } else {
                oneHandler.get().onGlobalEvent(eventId, args);
            }
        }
    }
}
