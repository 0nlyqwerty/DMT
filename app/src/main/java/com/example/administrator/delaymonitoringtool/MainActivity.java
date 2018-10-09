package com.example.administrator.delaymonitoringtool;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int SYSTEM_ALERT_WINDOW = 1;
    private static final int WRITE_EXTERNAL_STORAGE = 2;

    String TAG = "DMT_";
    String stringFileNewName;
    String logSavePath = (Environment.getExternalStorageDirectory() + "/DMT/PING_LOG/");

    StringBuffer sbWritePingLog = new StringBuffer();

    Boolean isTestRunning = Boolean.valueOf(false);
    Boolean pingTestEnd = false;

    Handler handler = new Handle();

    ScrollView mLogScrollView = null;

    // OBJECT 선언
    LinearLayout mainLayout;

    EditText editIp;
    EditText editRepeats;
    EditText editInterval;
    EditText editSize;

    Button saveSetBtn;
    Button loadSetBtn;
    Button saveLogBtn;
    Button clearLogBtn;
    Button startBtn;
    Button stopBtn;
    Button runBtn;
    Button stopRunBtn;

    TextView showLog;
    Switch swRun;

    class Handle extends Handler {
        public void handleMessage(Message msg){
            if (msg.what == 1){

            }
            if ((msg.what == 2) && showLog != null){
                showLog.setText(msg.getData().getString("result"));

            }
            if (msg.what == 3){
                Toast.makeText(MainActivity.this, "msg.what is 3", Toast.LENGTH_LONG).show();
            }

            // @@
            if (mLogScrollView != null){
                mLogScrollView.fullScroll(130); // 자동 스크롤
            }
        }
    }

    class RunPing implements Runnable{
        @Override
        public void run() {
            try {
                enabledEditText(false);
                startBtn.setClickable(false);
                ping(Util.getTtoS(editIp), Util.getTtoI(editRepeats), Util.getTtoI(editInterval), Util.getTtoI(editSize), sbWritePingLog);
                startBtn.setClickable(true);
                enabledEditText(true);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // @@ why return?
            return;
        }
    }

    // @@ have to implement LOGSAVE
    public void saveLog(){
        if(!isTestRunning){
            if("".equals(showLog.getText().toString()) || showLog == null){
                Toast.makeText(this, "There is no Log for SAVE", Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd_HHmmss");
            stringFileNewName = Util.getDeviceBrand() + "_" + Util.getSystemModel() + "_"
                    + Util.getSystemVersion() + "_" + SDF.format(new Date(System.currentTimeMillis())) + "_Log.txt";
            renameAndSaveLog(stringFileNewName);
        } else if(isTestRunning){
            Toast.makeText(this, "Can't save while testing", Toast.LENGTH_SHORT).show();
        }
    }

    // @@ have to implement LOGCLEAR
    public void clearLog(){
        if(!(isTestRunning.booleanValue()) && (showLog != null)){
            AlertDialog.Builder noticeAlert = new AlertDialog.Builder(this);
            noticeAlert.setTitle("CLEAR LOG");
            noticeAlert.setMessage("Will you clear LOG?");
            noticeAlert.setNegativeButton("NO", new dialogDismiss());
            noticeAlert.setPositiveButton("YES", new clearLogBuf());
            noticeAlert.create();
            noticeAlert.show();
        }else if (isTestRunning){
            Toast.makeText(this, "Can't clear while testing", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mainLayout = (LinearLayout) findViewById(R.id.mainLayout);
        mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editInterval.getWindowToken(),0);
            }
        });


        // OBJECT 초기화
        editIp = (EditText) findViewById(R.id.editIp);
        editRepeats = (EditText) findViewById(R.id.editRepeats);
        editInterval = (EditText) findViewById(R.id.editInterval);
        editSize = (EditText) findViewById(R.id.editSize);

        showLog = (TextView) findViewById(R.id.textLog);

        saveSetBtn = (Button) findViewById(R.id.btnSetSave);
        saveSetBtn.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        SharedPreferences setValue = getSharedPreferences("setValue",MODE_PRIVATE);
                        SharedPreferences.Editor editor = setValue.edit();
                        editor.putString("prefIp",Util.getTtoS(editIp));
                        editor.putString("prefRepeats",Util.getTtoS(editRepeats));
                        editor.putString("prefInterval",Util.getTtoS(editInterval));
                        editor.putString("prefSize",Util.getTtoS(editSize));
                        editor.commit();
                        showLog.setText(setValue.getString("prefIp",""));
                    }
                }
        );

        loadSetBtn = (Button) findViewById(R.id.btnSetLoad);
        loadSetBtn.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        SharedPreferences setValue = getSharedPreferences("setValue",MODE_PRIVATE);
                        editIp.setText(setValue.getString("prefIp",""));
                        editRepeats.setText(setValue.getString("prefRepeats",""));
                        editInterval.setText(setValue.getString("prefInterval",""));
                        editSize.setText(setValue.getString("prefSize",""));
                    }
                }
        );

        saveLogBtn = (Button) findViewById(R.id.btnLogSave);
        saveLogBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveLog();
                    }
                }
        );

        clearLogBtn = (Button) findViewById(R.id.btnLogClear);
        clearLogBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clearLog();
                    }
                }
        );

        startBtn = (Button) findViewById(R.id.btnStart);
        startBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        //Check empty
                        if(Util.checkEmpty(editIp)){
                            Toast.makeText(MainActivity.this, "Please input IP", Toast.LENGTH_LONG).show();
                            return;
                        }else if(Util.checkEmpty(editRepeats)){
                            Toast.makeText(MainActivity.this, "Please input Repeats", Toast.LENGTH_LONG).show();
                            return;
                        }else if(Util.checkEmpty(editInterval)){
                            Toast.makeText(MainActivity.this, "Please input Interval", Toast.LENGTH_LONG).show();
                            return;
                        }else if(Util.checkEmpty(editSize)){
                            Toast.makeText(MainActivity.this, "Please input Size", Toast.LENGTH_LONG).show();
                            return;
                        }
                        new Thread(new RunPing()).start();
                    }
                }
        );

        stopBtn = (Button) findViewById(R.id.btnStop);
        stopBtn.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        pingTestEnd=true;
                    }
                }
        );


        runBtn = (Button) findViewById(R.id.runBtn);
        runBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "A", Toast.LENGTH_LONG).show();
                        StartService();
                    }
                }
        );

        stopRunBtn = (Button) findViewById(R.id.stopRunBtn);
        stopRunBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "B", Toast.LENGTH_LONG).show();
                        StopService();
                    }
                }
        );
/*
        //SWITCH CODE
        swRun = (Switch) findViewById(R.id.swRun);
        swRun.setBackgroundColor(Color.RED);
        swRun.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    // start service code
                    StartService();
                }
                else{
                    // stop service code
                    StopService();
                }
            }
        });
*/
        PermissionCheck();
    }

    private void PermissionCheck(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW) != PackageManager.PERMISSION_GRANTED )
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW}, WRITE_EXTERNAL_STORAGE);
            else if(!Settings.canDrawOverlays(this))
                requestSysAlertPermission();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M){
            switch (requestCode){
                case WRITE_EXTERNAL_STORAGE:
                    if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                        //denied
                        Toast.makeText(MainActivity.this, "ABC", Toast.LENGTH_LONG).show();
                    }else{
                        //granted
                        if (!Settings.canDrawOverlays(this))
                        {
                            requestSysAlertPermission();
                        }
                    }
                    break;
            }
        }
    }

    private void requestSysAlertPermission()
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, SYSTEM_ALERT_WINDOW);
        }
    }


    //PING
    public void ping(String ip, int repeats, int interval, int size, StringBuffer stringBuffer) throws IOException, InterruptedException {
        int lineNumber = 0;
        this.isTestRunning = Boolean.valueOf(true);
        boolean isSuccess;
        Message msg1;   // handler 한테 보내줄때 쓸 msg3


        String cmd = "ping -c " + repeats + " " + "-i" + " " + interval + " " + "-s" + " " + size + " " + ip;
        //String cmd = "ping -c 5 -i 2 -w 3 127.0.0.1";

        Bundle bundlePingResult = new Bundle();

        Process process = Runtime.getRuntime().exec(cmd);
        Util.append(stringBuffer, "//////// Ping Test START ////////");

        BufferedReader pingReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        this.pingTestEnd = false;

        while (!(this.pingTestEnd)) {
            line = pingReader.readLine();
            lineNumber++;
            if(line == null)
                this.pingTestEnd = true;

            if(!(this.pingTestEnd)){   //testing
                if(lineNumber < 2 || lineNumber > repeats + 1){
                    Util.append(stringBuffer, "● "+line);
                } else{
                    Util.append(stringBuffer, "[" + Util.currentTime() + "]" + "\n" + line);
                }

                bundlePingResult.putString("result", stringBuffer.toString());
                if (this.handler != null) {
                    Message msg2 = this.handler.obtainMessage();
                    msg2.what = 2;
                    msg2.setData(bundlePingResult);
                    this.handler.sendMessage(msg2);
                }
            }else if(this.pingTestEnd){ // test end
                process.destroy();
                int resultStatus = process.waitFor(); // process.waitFor
                if (resultStatus == 0) {
                    Util.append(stringBuffer, "exec cmd success! cmd : " + cmd);
                    isSuccess = true;   // @@ have to check
                } else {
                    Util.append(stringBuffer, "exec cmd fail... resultStatus : " + resultStatus);
                    isSuccess = false;  // @@ have to check
                }
                Util.append(stringBuffer, "\n//////// Ping Test END ////////");

                bundlePingResult.putString("result", stringBuffer.toString());
                Message msg2 = this.handler.obtainMessage();
                msg2.what = 2;
                msg2.setData(bundlePingResult);
                this.handler.sendMessage(msg2);
            }
        }

        if (process != null) {
            process.destroy();
        }
        if (pingReader != null) {
            pingReader.close();
        }

        Message msg4 = this.handler.obtainMessage();
        msg4.what = 4;
        this.handler.sendMessage(msg4);

        // 테스트 종료
        this.isTestRunning = Boolean.valueOf(false);
    }

    public void onResume() {
        super.onResume();
        Util.closeKeyboard(this.editInterval, this);
    }

    // EditText enable or disable
    public void enabledEditText(Boolean enabledEditText){
        if(enabledEditText){
            this.editIp.setEnabled(true);
            this.editRepeats.setEnabled(true);
            this.editInterval.setEnabled(true);
            this.editSize.setEnabled(true);
        }else if(!(enabledEditText)){
            this.editIp.setEnabled(false);
            this.editRepeats.setEnabled(false);
            this.editInterval.setEnabled(false);
            this.editSize.setEnabled(false);
        }else{
            Log.i(TAG, "enableEditText can't enter any if function");
        }
    }

    public void enabledButton(Boolean enabledButton){
        if(enabledButton){
            this.saveSetBtn.setEnabled(true);
            this.saveLogBtn.setEnabled(true);
            this.clearLogBtn.setEnabled(true);
            this.startBtn.setEnabled(true);
        }else if(!enabledButton){
            this.saveSetBtn.setEnabled(false);
            this.saveLogBtn.setEnabled(false);
            this.clearLogBtn.setEnabled(false);
            this.startBtn.setEnabled(false);
        }else{
            Log.i(TAG, "enableButton can't enter any if function");
        }
    }


    // Rename And Save Log
    public void renameAndSaveLog(String oldName){
        final String mOldName = oldName;
        View textEntryView = LayoutInflater.from(this).inflate(R.layout.rename_dialog, null);
        final EditText mname_edit = (EditText) textEntryView.findViewById(R.id.rename_edit);
        mname_edit.setText(mOldName);

        new AlertDialog.Builder(this).setView(textEntryView).setPositiveButton("YES",
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Action for 'yes' button
                if(Util.getTtoS(mname_edit).equals("") || mname_edit.length() <= 4
                        || !Util.getTtoS(mname_edit).substring(mname_edit.length() - 4, mname_edit.length()).equals(".txt")) {
                    mname_edit.setText(mOldName);
                    Toast.makeText(MainActivity.this, "There is no file name\nor\nDoesn't include .txt at end of file name", Toast.LENGTH_LONG).show();
                    return;
                }
                stringFileNewName = mname_edit.getText().toString();
                Util.writeTxtToFile(showLog.getText().toString(), logSavePath, stringFileNewName);
                Toast.makeText(MainActivity.this, "LOG : " + logSavePath + stringFileNewName, Toast.LENGTH_LONG).show();
            }
        }).setNegativeButton("NO", new dialogDismiss()).show();
    }



    // dialog dismiss
    class dialogDismiss implements DialogInterface.OnClickListener{
        @Override
        public void onClick(DialogInterface dialog, int which) {
            Toast.makeText(MainActivity.this, "dialog dismissed", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }
    }

    // clear showLog and buffer
    class clearLogBuf implements DialogInterface.OnClickListener{
        @Override
        public void onClick(DialogInterface dialog, int which) {
            clearLogAndBuffer(showLog, sbWritePingLog);
            //dialog.dismiss();
        }
    }
    public void clearLogAndBuffer(TextView tv, StringBuffer stringBuffer){
        if(tv != null)
            tv.setText("");
        if(stringBuffer != null)
            stringBuffer.delete(0, stringBuffer.length());
    }


    private void StartService(){
        if(!MainService.Run){
            //permission check
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_EXTERNAL_STORAGE);
            }

            // Data empty check
            if(Util.checkEmpty(editIp)){
                Toast.makeText(MainActivity.this, "Please input IP", Toast.LENGTH_LONG).show();
                return;
            }else if(Util.checkEmpty(editInterval)){
                Toast.makeText(MainActivity.this, "Please input Interval", Toast.LENGTH_LONG).show();
                return;
            }

            //give ip and interval value to MainService from MainActivity when service start
            String dest = Util.getTtoS(editIp);
            String interval = Util.getTtoS(editInterval);
            Intent intent = new Intent(this, MainService.class);
            Log.d(TAG, " " + dest + " " + interval);
            intent.putExtra("dest", dest);
            intent.putExtra("interval", interval);
            startService(intent);

            // create notification

            // notification channel init
            String channelID = "channel";
            String channelName = "Channel Name";
            int importance = NotificationManager.IMPORTANCE_LOW;

            // nm init
            NotificationManager mNM =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            // Version Check
            if(Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(
                        channelID, channelName, importance);

                mNM.createNotificationChannel(mChannel);
            }

            // notification settings
            Intent mMyIntent = new Intent(this, MainActivity.class);
            PendingIntent mPendingIntent = PendingIntent.getActivity(this, 1, mMyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, channelID)
                            .setSmallIcon(R.mipmap.ic_launcher_round)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round))
                            .setContentTitle("DTT")
                            .setContentText("Ping test Started. touch to DTT")
                            .setAutoCancel(true)
                            .setContentIntent(mPendingIntent);

            // notify
            mNM.notify(1, mBuilder.build());

            // Go home
            Intent intentGoHome = new Intent(Intent.ACTION_MAIN);
            intentGoHome.addCategory(Intent.CATEGORY_HOME);
            intentGoHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentGoHome);
        }
    }

    private void StopService(){
        if(MainService.Run){
            Intent intent = new Intent(this, MainService.class);
            stopService(intent);
        }

    }
}

// TODO: Have to implement close keyboard function -> finish. have to apply
// TODO: Have to implement disable buttons or editTexts -> finish. apply after run switch to button
// TODO: Have to add detail debug log like Log.d



// TODO: Check AutoScroll part..
// TODO: Check several init codes like process, buffer clear or something


// TODO: At editTexts, keyboard have to disappear if touch other space

// TODO: Let's destroy useless values and functions
// TODO: I think Run button is better than Run Switch...
// TODO: Let's try separating classes
// TODO: Let's try combine START and RUN switch

// + SKT ip : 210.220.163.82

/*
1) delay 숫자에 따라 warning color disaly
2) 결과를 새로운 뷰에 display. ex) min max avr
3) Check Empty



0. editIp 에 single line 속성 부여
1. Run 시 Home 으로 돌아감
2. startBtn 터치 시 setClickable false
3. Run 시 notification 띄움, 해당 노티 터치 시 DMT 앱으로 진입
4. edit ip 엔터키 disable



1.셋팅값 저장할수 있는 항목 추가 필요
-> 현재는 따로 버튼으로 바꿨지만
Test Start 시 option 값 저장, Resume 시 값 호출하면 될것같음.

2.시험 중 중단할 수 있는 STOP 기능 추가 필요
->

3.Log 창에 Ping time 값 표시하는 항목 추가 필요
-> Interval 과 Size 옵션이 겹칠 경우 time 출력이 어려움.
size 의 default 는 64byte, interval 은 1초임. 통계를 하기 위해서는
size 혹은 interval 둘중 하나를 사용하지 않아야함.
혹시 interval 옵션이 테스트 시간때문이라면,
대체재로 테스트 시간을 초단위로 설정 할 수 있는 옵션이 있음.
놉!!!!!!!!!!!!!!!!!!!!!!!!!!!! packet size가 16 이상이어야만 나오는거였음 -_-


4.Log 창에 Rrt min/ avg /max/mdev 값 표시 추가 필요
-> min, avg, max, mdev 값 말하는것. Interval 와 Size 옵션이 겹칠경우 통계가 어려움

5.Run on 이후 floating 창에 pingtime 진행되는 시간 및 실시간 ping time 값 표시 추가 필요
->

6.Floating 창으로 동작시 Repeats / Size 설정값도 인식가능하도록 수정필요.
-> 이것도 3,4번과 마찬가지로

 */