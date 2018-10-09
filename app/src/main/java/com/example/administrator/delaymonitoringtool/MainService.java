package com.example.administrator.delaymonitoringtool;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainService extends Service {
    public static String TAG = "DMT";
    StringBuffer sbWritePing = new StringBuffer();
    private static boolean RunTest = false;
    public static boolean Run = false;
    Handler handler = new Handle();
    TextView dlyValue;
    TextView minValue;
    TextView maxValue;
    TextView avrValue;
    private View mView;
    String destFromActivity;
    String intervalFromActivity;
    String fileDest;
    String folderDest;
    private static WindowManager mManager;
    private static WindowManager.LayoutParams mParams;

    private float mTouchX, mTouchY;
    private int mViewX, mViewY;

    private boolean isMove = false;

    private boolean isEND = false;


    float sumDelay = (float) 0.0;
    float minDelay = (float) 100.0;
    float maxDelay = (float) 0.0;
    int count = 0;
    class Handle extends Handler {
        public void handleMessage(Message msg){

            if (msg.what == 1 ){

                String showText = msg.getData().getString("test");
                String[] arr = showText.split(" ");
                String ip = arr[3].replace(":","");
                String seq = arr[4].replace("icmp_seq=","");
                String delay = arr[6].replace("time=","").replace(" ms","");
                String size = arr[0];
                int intSeq = Integer.parseInt(seq);
                float floatDelay = Float.parseFloat(delay);
                if(floatDelay < minDelay)
                    minDelay = floatDelay;
                if(floatDelay > maxDelay)
                    maxDelay = floatDelay;
                if(intSeq==1){
                    sumDelay += (floatDelay)/(float)2;
                }else{
                    sumDelay += floatDelay;
                }

                Log.d(TAG, "sumDelay="+sumDelay+"/seq="+intSeq+" "+seq);
                float avrDelay = sumDelay / (float) intSeq;
                String strMinDelay = String.format("%.3f",minDelay);
                String strMaxDelay = String.format("%.3f",maxDelay);
                String strAvrDelay = String.format("%.3f",avrDelay);

                if (arr != null){
                    dlyValue.setText(delay);
                    minValue.setText(strMinDelay);
                    maxValue.setText(strMaxDelay);
                    avrValue.setText(strAvrDelay);
                } else {
                    Log.d(TAG, "arr is null");
                }
                if(count != 0)  //to first duplicate
                    Util.writeLog(showText,fileDest);
                count++;
                if(!RunTest){
                    Util.writeLog("[ Min Delay : " + strMinDelay + " // Max Delay : " + strMaxDelay + " // Delay Average : " + strAvrDelay + " ]",fileDest);
                    Util.writeLog("////////ping test end",fileDest);
                    Toast.makeText(MainService.this,"Ping result saved at [ /DMT/ ] .",Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    private View.OnTouchListener mViewTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:

                    mTouchX = event.getRawX();
                    mTouchY = event.getRawY();
                    mViewX = mParams.x;
                    mViewY = mParams.y;

                    break;

                case MotionEvent.ACTION_MOVE:
                    isMove = true;

                    int x = (int) (event.getRawX() - mTouchX);
                    int y = (int) (event.getRawY() - mTouchY);

                    final int num = 5;
                    if ((x > -num && x < num) && (y > -num && y < num)) {
                        isMove = false;
                        break;
                    }

                    mParams.x = mViewX + x;
                    mParams.y = mViewY + y;

                    mManager.updateViewLayout(mView, mParams);

                    break;
            }

            return true;
        }
    };

    @Override
    public void onCreate(){
        super.onCreate();
        RunTest = true;

        LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = mInflater.inflate(R.layout.always_on_top_view, null);

        mView.setOnTouchListener(mViewTouchListener);

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        mManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mManager.addView(mView, mParams);

        dlyValue = (TextView) mView.findViewById(R.id.dlyValue);
        minValue = (TextView) mView.findViewById(R.id.minValue);
        maxValue = (TextView) mView.findViewById(R.id.maxValue);
        avrValue = (TextView) mView.findViewById(R.id.avrValue);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        Run = true;
        destFromActivity = intent.getStringExtra("dest");
        intervalFromActivity = intent.getStringExtra("interval");
        Log.d(TAG, "int :" + intervalFromActivity + " " + destFromActivity);
        new Thread(new runPing()).start();
        return super.onStartCommand(intent, flags, startID);
    }

    @Override
    public void onDestroy(){
        Run = false;
        RunTest = false;
        if(mView != null) {
            mManager.removeView(mView);
            mView = null;
        }
        isEND = true;
        super.onDestroy();
    }


    class runPing implements Runnable{
        @Override
        public void run() {
            try {
                ping(destFromActivity, intervalFromActivity, sbWritePing);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public void ping(String dest, String interval, StringBuffer sb) throws IOException, InterruptedException {
        String cmd = "ping -i "+ interval + " " + dest;
        Log.d(TAG, "int :" + intervalFromActivity + " " + destFromActivity);
        Process process = Runtime.getRuntime().exec(cmd);

        Bundle bundlePingResult = new Bundle();

        BufferedReader pingReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMDD_HHmmss");
        String filename = dateFormat.format(new Date(System.currentTimeMillis())).toString();

        folderDest = (Environment.getExternalStorageDirectory() + "/DMT/PING_LOG");
        fileDest = Environment.getExternalStorageDirectory().getAbsolutePath()+"/DMT/DMT_"+ filename +".txt";
        Util.makeRootDir(folderDest);

        Util.writeLog("////////ping test start",fileDest);
        while(RunTest){
            line = pingReader.readLine();
            Log.d(TAG, line);
            if(line == null)
                break;
            bundlePingResult.putString("test", line.toString());
            Message msg = this.handler.obtainMessage();
            msg.what = 1;
            msg.setData(bundlePingResult);
            this.handler.sendMessage(msg);
        }
        process.waitFor();

        if (process != null) {
            process.destroy();
        }
        if (pingReader != null) {
            pingReader.close();
        }
    }
}
