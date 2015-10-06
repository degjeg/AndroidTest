package com.downloader;

import android.util.Log;

/**
 * Created by danger on 15/9/19.
 */
public class Logcat {

    public static void w(String tag, String tr) {
        Log.e(tag, tr);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
    }

    public static void d(String tag, String msg) {
        Log.e(tag, msg);
    }

    public static void i(String tag, String msg) {
        Log.e(tag, msg);
    }
}
