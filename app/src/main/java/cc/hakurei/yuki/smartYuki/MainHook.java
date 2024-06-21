package cc.hakurei.yuki.smartYuki;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.KeyEvent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage{

//    private int LongPressTime =0;
    private DebounceTask pdt;
    public Context sysContext;
    private AudioManager mAudioManager;
    private long lastPressAStartAtMs = 0L;
    private long lastPressVStartAtMs = 0L; //for volume keys

    private int clickTime = 0;

    private int mVolFunction = FUNCTION_NONE;

    private static final int FUNCTION_NONE = 0;
    private static final int FUNCTION_BACKWORD = 1;
    private static final int FUNCTION_FORWORD = 2;
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        pdt = DebounceTask.build(new Runnable() {
            @Override
            public void run() {
                onPress();
            }
        },500L);

        if(sysContext == null){
            sysContext = AndroidAppHelper.currentApplication(); //获取到android 的上下文
            initAudioMgr();
        }

        XposedHelpers.findAndHookMethod("com.android.server.input.InputManagerService", lpparam.classLoader, "interceptKeyBeforeQueueing",
                KeyEvent.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        KeyEvent keyEvent = (KeyEvent) param.args[0];

                        int kc = keyEvent.getKeyCode();
                        if (kc != KeyEvent.KEYCODE_ASSIST && !isVolumeKey(kc))
                            return;

                        int flags = keyEvent.getFlags();
                        if (hasFlag(flags, KeyEvent.FLAG_FROM_SYSTEM))
                            return;

                        if (keyEvent.getAction() != KeyEvent.ACTION_DOWN && keyEvent.getAction() != KeyEvent.ACTION_UP)
                            return;



                        long t = SystemClock.uptimeMillis();
                        if(isVolumeKey(kc)){

                            if(keyEvent.getAction() == KeyEvent.ACTION_DOWN){
                                lastPressVStartAtMs = t;;
                                mVolFunction = kc == KeyEvent.KEYCODE_VOLUME_UP ? FUNCTION_BACKWORD : FUNCTION_FORWORD;
                                Logger.d("set Function to:"+mVolFunction);
                            }

                            if (!isCombinationTimeout()) {
                                param.setResult(0); //如果还未超时长就拦截掉； event拦截相关: https://blog.csdn.net/dongxianfei/article/details/129268412
                                Logger.d("prevent vol key in InputManagerService");
                            }

                        } else { //is assist key
                            param.setResult(0); //谷歌键直接拦截没商量
                            if(keyEvent.getAction() == KeyEvent.ACTION_DOWN){
                                Logger.d("interceptKeyBeforeQueueing KEYCODE_ASSIST DOWN;");
                                if(t - lastPressAStartAtMs <=300){
                                    clickTime +=1;
                                } else {
                                    clickTime = 1;
                                }
                                lastPressAStartAtMs = t;
                            } else {
                                //assist key up
                                Logger.d("interceptKeyBeforeQueueing KEYCODE_ASSIST UP;");
                                pdt.timerRun();
                            }
                        }
                        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && isCombinationTimeout()) {
                            mVolFunction = FUNCTION_NONE;
                            Logger.d("clear Function");
                        }
                        onPressStart();
                    }

                }
        );

    }
    boolean hasFlag(int flags, int flag) {
        return (flags & flag) != flag;
    }

    boolean isVolumeKey(int keycode) {
        return keycode == KeyEvent.KEYCODE_VOLUME_DOWN || keycode==KeyEvent.KEYCODE_VOLUME_UP;
    }

    boolean isCombinationTimeout() { //组合键超时
        return Math.abs(lastPressAStartAtMs-lastPressVStartAtMs)>300;
    }
    private void initAudioMgr(){
        mAudioManager = (AudioManager) sysContext.getSystemService(Context.AUDIO_SERVICE);
    }

    //code from github.com/Hepolise/VolumeKeyMusicManagerModule: sendMediaButtonEvent & dispatchMediaButtonEvent
    private void sendMediaButtonEvent(int kcode){
        long eventTime = SystemClock.uptimeMillis();
        KeyEvent keyEvent = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, kcode, 0);
        dispatchMediaButtonEvent(keyEvent);
        keyEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP);
        dispatchMediaButtonEvent(keyEvent);

    }

    private void dispatchMediaButtonEvent(KeyEvent keyEvent) {
        try{
            mAudioManager.dispatchMediaKeyEvent(keyEvent);
        }catch (Exception e){
            Logger.e(e.getLocalizedMessage());
        }
    }
    private void onPressStart() {//暂时还没用
        Logger.d("onPressStart triggered");
    }
    private void onPress() { //无论长短
        Logger.d("onPress triggered");
//        Logger.d(String.valueOf(is_long_press));
        switch (mVolFunction){
            case FUNCTION_FORWORD:
                sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
                Logger.d("toggled FUNCTION_FORWORD");
                break;
            case FUNCTION_BACKWORD:
                sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_REWIND);
                Logger.d("toggled FUNCTION_BACKWORD");
                break;
        }
        if(mVolFunction!=FUNCTION_NONE)
            return;
        //mFunction is NONE
        switch (clickTime){
            case 1:
                sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                Logger.d("toggled PLAY_PAUSE");
                break;
            case 2:
                sendMediaButtonEvent(KeyEvent.KEYCODE_MEDIA_STOP);
                Logger.d("toggled MEDIA_STOP");
                break;
        }
    }
}