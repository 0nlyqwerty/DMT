package com.example.administrator.delaymonitoringtool;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class Util {
    static final String TAG = "Util";



    // close key board
    public static void closeKeyboard(EditText mEditText, Context mContext){
        ((InputMethodManager) mContext.getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(mEditText.getWindowToken(),0);
    }

    //EditText Empty Check function
    public static boolean checkEmpty(EditText mEditText){
        if("".equals(mEditText.getText().toString()) || mEditText == null)
            return true;
        else
            return false;
    }

    // stringBuffer 에 "\n"를 포함한 (String)text 를 append 한다
    public static void append(StringBuffer stringBuffer, String text){
        if (stringBuffer != null){
            stringBuffer.append(new StringBuffer(String.valueOf(text)).append("\n").toString());
        }
    }

    // write Txt To File
    public static void writeTxtToFile(String strcontent, String filePath, String fileName){
        String strContent = new StringBuilder(String.valueOf(strcontent)).append("\r\n").toString();
        try{
            File file = makeFilePath(filePath, fileName);
            if(!file.exists()){
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            Log.e(TAG, "Error on write File : " + e);
        }
    }

    // make File path
    public static File makeFilePath(String filePath, String fileName){
        Exception e;
        File file = null;
        makeRootDir(filePath);
        try {
            File file2 = new File(new StringBuilder(String.valueOf(filePath)).append(fileName).toString());
            try {
                if (file2.exists()) {
                    return file2;
                }
                file2.createNewFile();
                return file2;
            } catch (Exception e2) {
                e = e2;
                file = file2;
                //Create file error
                e.printStackTrace();
                return file;
            }
        } catch (Exception e3) {
            e = e3;
            //Create file error
            e.printStackTrace();
            return file;
        }
    }

    // make Root Directory
    public static void makeRootDir(String filePath){
        Exception e;
        try {
            File file = new File(filePath);
            try {
                if (file.exists()) {
                    Log.i(TAG, "dir is exist....: " + file.getPath());
                    return;
                }
                file.mkdirs();
                Log.i(TAG, "make dir" + file.getPath());
            } catch (Exception e2) {
                e = e2;
                Log.i(TAG, String.valueOf(e));
            }
        } catch (Exception e3) {
            e = e3;
            Log.i(TAG, String.valueOf(e));
        }
    }

    // convert EditText's texts
    public static String getTtoS(EditText et){ return et.getText().toString(); };
    public static int getTtoI(EditText et){ return Integer.parseInt(et.getText().toString()); }

    // get device information
    public static String getSystemVersion() { return Build.VERSION.RELEASE; }
    public static String getSystemModel() { return Build.MODEL; }
    public static String getDeviceBrand() { return Build.BRAND; }

    public static String currentTime(){
        String time;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        time = simpleDateFormat.format(new Date(System.currentTimeMillis())).toString();
        return time;
    }



    public static void writeLog(String str, String dest){
        try{
            File file = new File(dest);
            if(file.exists() == false){
                file.createNewFile();
            }
        }catch (IOException e) {
            Log.e(TAG, "can't create new folder : " + e);
        }

        try {
            BufferedWriter bfw = new BufferedWriter(new FileWriter(dest,true));
            bfw.write("[" + Util.currentTime() + "] " + str);
            bfw.write("\n");
            bfw.flush();
            bfw.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found : " + e);
        } catch (IOException e) {
            Log.e(TAG, "error : " + e);
        }

    }


    public static void setSettingPref(String ip, String repeats, String interval, String size ,SharedPreferences pref){
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("prefIp",ip);
        editor.putString("prefRepeats",repeats);
        editor.putString("prefInterval",interval);
        editor.putString("prefSize",size);
        editor.commit();
    }

    public static int getPid(Process p){
        int pid = -1;

        try {
            Field f = p.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            pid = f.getInt(p);
            f.setAccessible(false);
        }catch (Throwable e) {
            pid = -1;
        }
        return pid;
    }
}