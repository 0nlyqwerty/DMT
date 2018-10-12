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
    TextView eclipseText;
    TextView dlyValue;
    TextView minValue;
    TextView maxValue;
    TextView avrValue;
    private View mView;
    String destFromActivity;
    String repeatFromActivity;
    String intervalFromActivity;
    String sizeFromActivity;
    String fileDest;
    String folderDest;
    private static WindowManager mManager;
    private static WindowManager.LayoutParams mParams;

    private float mTouchX, mTouchY;
    private int mViewX, mViewY;

    private boolean isMove = false;
    private boolean isEND = false;
    boolean readyToGetMsg;


    float sumDelay;
    float minDelay;
    float maxDelay ;
    int count;
    int printOnce;
    int testStop;
    long startTime;
    long eclipseTime;
    String delay="";
    String strMinDelay="-";
    String strMaxDelay="-";
    String strAvrDelay="-";
    class Handle extends Handler {
        public void handleMessage(Message msg){


            if (msg.what == 1 ){

                String showText = msg.getData().getString("line");

                String [] arr = showText.split(" ");
                String ip,seq,size;
                Log.e(TAG,"SONG  " + showText + arr.length);
                if(arr.length == 8){
                    ip = arr[3].replace(":","");
                    seq = arr[4].replace("icmp_seq=","");
                    delay = arr[6].replace("time=","").replace(" ms","");
                    size = arr[0];
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
                    strMinDelay = String.format("%.3f",minDelay);
                    strMaxDelay = String.format("%.3f",maxDelay);
                    strAvrDelay = String.format("%.3f",avrDelay);
                }

                if (arr != null){
                    dlyValue.setText(delay);
                    minValue.setText(strMinDelay);
                    maxValue.setText(strMaxDelay);
                    avrValue.setText(strAvrDelay);
                } else {
                    Log.d(TAG, "arr is null");
                }
                if(arr.length == 8)  //to first duplicate
                    Util.writeLog(showText,fileDest);
                if(count != 0 && arr.length == 5)  //to first duplicate
                    Util.writeLog(showText,fileDest);
                count++;
                if(!RunTest && printOnce == 0){
                    printOnce++;
                    Util.writeLog("[ Min Delay : " + strMinDelay + " // Max Delay : " + strMaxDelay + " // Delay Average : " + strAvrDelay + " ]",fileDest);
                    Util.writeLog("////////ping test end",fileDest);
                    Toast.makeText(MainService.this,"Ping result saved at [ /DMT/ ] .",Toast.LENGTH_LONG).show();
                    testStop = 1;
                    onDestroy();
                }
                readyToGetMsg = true;
            }else if (msg.what==2){
                eclipseTime = System.currentTimeMillis() - startTime;
                String eclipseTimeString = String.format("%02d:%02d:%02d",eclipseTime/(1000*60*60),(eclipseTime/(1000*60))%60,(eclipseTime/1000)%60);
                Log.e(TAG,"SONG T : "+eclipseTimeString);
                eclipseText.setText(eclipseTimeString);
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        Run = true;
        startTime = System.currentTimeMillis();
        destFromActivity = intent.getStringExtra("dest");
        repeatFromActivity = intent.getStringExtra("repeats");
        intervalFromActivity = intent.getStringExtra("interval");
        sizeFromActivity = intent.getStringExtra("size");
        Log.d(TAG, "int :" + intervalFromActivity + " " + destFromActivity);

        if(!RunTest){
            sumDelay = (float) 0.0;
            minDelay = (float) 100.0;
            maxDelay = (float) 0.0;

            Log.e(TAG, "SONG onStartCommand, RunTest become True");
            RunTest = true;
            count = 0;
            printOnce = 0;
            testStop = 0;
            readyToGetMsg = true;

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


            eclipseText = (TextView) mView.findViewById(R.id.eclipseTime);
            dlyValue = (TextView) mView.findViewById(R.id.dlyValue);
            minValue = (TextView) mView.findViewById(R.id.minValue);
            maxValue = (TextView) mView.findViewById(R.id.maxValue);
            avrValue = (TextView) mView.findViewById(R.id.avrValue);

            new Thread(new runPing()).start();
            new Thread(new checkTime()).start();
        }

        return super.onStartCommand(intent, flags, startID);
    }

    @Override
    public void onDestroy(){
        Log.e(TAG,"SONG onDestroy");
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
                ping(destFromActivity, repeatFromActivity, intervalFromActivity, sizeFromActivity, sbWritePing);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class checkTime implements Runnable{
        @Override
        public void run() {
            eclTime();
        }
    }

    public void eclTime(){
        while(RunTest){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Message msg = this.handler.obtainMessage();
            msg.what = 2;
            this.handler.sendMessage(msg);
            Log.e(TAG,"SONG WwW");
        }
    }

    public void ping(String dest, String repeats, String interval, String size, StringBuffer sb) throws IOException, InterruptedException {

        // set cmd and run process
        String cmd = "ping" + " -c " + repeats + " -i " + interval + " -s " + size + " " + dest;
        Log.e(TAG, "SONG cmd : " + cmd);
        Process process = Runtime.getRuntime().exec(cmd);

        // bundle for give message to handler
        Bundle bundlePingResult = new Bundle();

        // get outputs
        BufferedReader pingReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        // log text file name
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMDD_HHmmss");
        String filename = dateFormat.format(new Date(System.currentTimeMillis())).toString();

        // set folder name and make (root dir)
        folderDest = (Environment.getExternalStorageDirectory() + "/DMT/PING_LOG");
        fileDest = Environment.getExternalStorageDirectory().getAbsolutePath()+"/DMT/DMT_"+ filename +".txt";
        Util.makeRootDir(folderDest);

        boolean printFlag = false;
        Util.writeLog("////////ping test start", fileDest);
        Log.e(TAG, "SONG RunTest : "+RunTest);
        while(RunTest){
            line = pingReader.readLine();
            if(line == null){
                RunTest = false;
                Log.e(TAG, "SONG AA");
                break;
            }
            if(readyToGetMsg && printFlag){
                readyToGetMsg = false;
                Log.e(TAG, "SONG BB");
                bundlePingResult.putString("line", line.toString());
                Message msg = this.handler.obtainMessage();
                msg.what = 1;
                msg.setData(bundlePingResult);
                this.handler.sendMessage(msg);
            }
            Log.e(TAG, "SONG ZZ");
            printFlag = true;
        }
        Log.e(TAG, "SONG CC");
        process.waitFor();
        Log.e(TAG, "SONG DD");
        if (process != null) {
            process.destroy();
        }
        if (pingReader != null) {
            pingReader.close();
        }
    }
}