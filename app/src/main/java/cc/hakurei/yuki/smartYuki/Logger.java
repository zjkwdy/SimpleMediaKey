package cc.hakurei.yuki.smartYuki;

import android.util.Log;

public class Logger {
    private static final String LOG_TAG = "YukiKey";

    public static void d(String s){
        Log.d(LOG_TAG,s);
    }
    public static void e(String s){
        Log.e(LOG_TAG,s);
    }
}
