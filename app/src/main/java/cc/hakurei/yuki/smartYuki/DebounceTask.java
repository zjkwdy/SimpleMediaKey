package cc.hakurei.yuki.smartYuki;

import java.util.Timer;
import java.util.TimerTask;

//复制粘贴：https://www.cnblogs.com/love-ice-will-win/p/16557359.html
public class DebounceTask {
    /**
     * 防抖实现关键类
     */
    private Timer timer;
    /**
     * 防抖时间:根据业务评估
     */
    private Long delay;
    /**
     * 开启线程执行任务
     */
    private Runnable runnable;

    public DebounceTask(Runnable runnable,  Long delay) {
        this.runnable = runnable;
        this.delay = delay;
    }

    /**
     *
     * @param runnable 要执行的任务
     * @param delay 执行时间
     * @return 初始化 DebounceTask 对象
     */
    public static DebounceTask build(Runnable runnable, Long delay){
        return new DebounceTask(runnable, delay);
    }

    //Timer类执行:cancel()-->取消操作；schedule()-->执行操作
    public void timerRun(){
        //如果有任务,则取消不执行(防抖实现的关键)
        if(timer!=null){
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //把 timer 设置为空,这样下次判断它就不会执行了
                timer=null;
                //执行 runnable 中的 run()方法
                runnable.run();
            }
        }, delay);
    }
}
