package contact.helper.android.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.downloader.DownloadListener;
import com.downloader.DownloadTask;
import com.downloader.FileDownloader;
import com.timer.TimerCallback;
import com.timer.TimerInfo;
import com.timer.TimerManager;


public class MainActivity extends Activity implements DownloadListener {

    private static final String TAG = "FileDownloader__";
    int a, b, c;
    Button goUp;
    Button goDown;
    SeekBar volumebar;
    LinearLayout player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testDownload();

        //testTimer();
    }

    TextView tvs[];

    private void testDownload() {
        tvs = new TextView[5];
        tvs[0] = (TextView) findViewById(R.id.tv1);
        tvs[1] = (TextView) findViewById(R.id.tv2);
        tvs[2] = (TextView) findViewById(R.id.tv3);
        tvs[3] = (TextView) findViewById(R.id.tv4);
        tvs[4] = (TextView) findViewById(R.id.tv5);


        final String urls[] = {
                "http://shows.vogueimg.com.cn/showspic/FashionImages/S2016RTW/london/zoe-jordan/collection/zoe-jordan-spring-2016-001h.jpg.360X540.jpg_mark.jpg",
                "http://m2yd.pc6.com/mac/BeyondCompare.dmg",
                "http://shows.vogueimg.com.cn/showspic/FashionImages/S2016RTW/london/zoe-jordan/collection/zoe-jordan-spring-2016-005h.jpg.360X540.jpg_mark.jpg",
                "http://shows.vogueimg.com.cn/showspic/FashionImages/S2016RTW/london/zoe-jordan/collection/zoe-jordan-spring-2016-009h.jpg.360X540.jpg_mark.jpg",
                "http://shows.vogueimg.com.cn/showspic/FashionImages/S2016RTW/london/zoe-jordan/collection/zoe-jordan-spring-2016-013h.jpg.360X540.jpg_mark.jpg",
        };

        final String f[] = new String[urls.length];

        for (int i = 0; i < urls.length; i++) {
            f[i] = Environment.getExternalStorageDirectory() + "/d/f" + i + ".bin";
            FileDownloader.getInstance().startDownload(urls[i], f[i], this);
        }


        Thread thread = new Thread() {
            @Override
            public void run() {
                final int T = 10000;
                int t = 0;

                while (true) {
                    try {
                        sleep(13000);

                        for (String url : urls) {
                            FileDownloader.getInstance().pauseTask(url);
                        }
                        sleep(13000);


                        if (t > T) {
                            return;
                        }

                        for (int i = 0; i < urls.length; i++) {
                            FileDownloader.getInstance().startDownload(urls[i], f[i], MainActivity.this);
                        }

                        t += 300;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        thread.start();
    }


    @Override
    public void onDownloadEvent(DownloadTask task) {
        String f = task.getInfo().getFilePathName();
        int pos = TextUtils.indexOf(f, ".") - 1;
        int index = Integer.valueOf(f.substring(pos, pos + 1));
        String info =
                task.toString() + ",index:" +
                        index;
        tvs[index].setText(info
        );

        Log.e(TAG, "info:" + info);

    }


    private void testTimer() {
        TimerManager.getInstance().init(this);


        TimerManager.getInstance().setTimer("test1_4", new TimerCallback() {
            @Override
            public void onTimer(TimerInfo timerInfo) {
                Log.e(TAG, "" + timerInfo);
            }
        }, 600, false);

        TimerManager.getInstance().setTimer("test1_1", new TimerCallback() {
            @Override
            public void onTimer(TimerInfo timerInfo) {
                Log.e(TAG, "" + timerInfo);
            }
        }, 120, true);
        //
        ////        Log.e(TAG, "" + timerInfo);
        //        Log.e(TAG, "" + timerInfo);
        //    }
        //}, 240, true);
        //
    }
}
