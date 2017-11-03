package com.aravind.android.flashlight;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.CollapsibleActionView;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.Random;

import static com.aravind.android.flashlight.R.id.cal;
import static com.aravind.android.flashlight.R.id.tv;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    double calib=8000;
    private AdView mAdView;
    TextView mStatusView;
    Thread runner;
    private static double mEMA = 0.0;
    static final private double EMA_FILTER = 0.6;

    final Runnable updater = new Runnable(){

        public void run(){
            updateTv();
        };
    };

    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    int playing = 0,recording=0,pausing=0,threader=0;

    private MediaRecorder mRecorder = null;

    boolean mStartPlaying = true;

    boolean mStartRecording = true;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();

    }
    final Handler mHandler = new android.os.Handler();

    public static final int MY_PERMISSIONS_CAMERA=99;

    public boolean checkcam() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission. CAMERA)) {
                new AlertDialog.Builder(this)
                        .setPositiveButton(R.string.app_name, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.CAMERA},
                                        MY_PERMISSIONS_CAMERA);
                            }
                        })
                        .create()
                        .show();


            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission. CAMERA},
                        MY_PERMISSIONS_CAMERA);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStatusView = (TextView) findViewById(R.id.status);
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        try {
            if (runner == null) {
                runner = new Thread() {
                    public void run() {
                        while (runner != null) {
                            try {
                                Thread.sleep(100);
                                Log.i("Noise", "Tock");
                            } catch (InterruptedException e) {
                            }
                            ;
                            mHandler.post(updater);
                        }
                    }
                };
                runner.start();
                Log.d("Noise", "start runner()");
            }
            startRecorder();
        }catch (Exception e){
            Toast.makeText(this, "Give Permission to make it run and restart the app", Toast.LENGTH_LONG).show();
        }
    }

    int led=0;

    public void ftor(View view)
    {
        if(led==0)
        { turnonflash();
            led=1;
        }
        else {
            led = 0;
            turnoffflash();
        }
    }

    void turnonflash(){
        checkcam();
        ImageButton im =(ImageButton) findViewById(R.id.led);
        im.setImageResource(R.drawable.temp1);
            try {
                try {
                    CameraManager manager = (CameraManager) getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
                    String[] list = manager.getCameraIdList();
                    manager.setTorchMode(list[0], true);
                } catch (CameraAccessException cae) {
                    cae.printStackTrace();
                }
            }catch (Exception f)
            {}


    }

    void turnoffflash()
    {
        ImageButton im =(ImageButton) findViewById(R.id.led);
        im.setImageResource(R.drawable.temp);

            try {
                try {
                    CameraManager manager = (CameraManager) getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
                    String[] list = manager.getCameraIdList();
                    manager.setTorchMode(list[0], false);
                } catch (CameraAccessException cae) {
                    cae.printStackTrace();
                }
            }catch (Exception f)
            {}
    }

    public void fadd(View view)
    {
        if(calib<20000)
         calib+=500;
        EditText tvc= (EditText) findViewById(R.id.cal);
        tvc.setText(""+ calib);
    }

    public void fmin(View view)
    {
        if(calib>0)
        calib-=500;
        EditText tvc=(EditText) findViewById(R.id.cal);
        tvc.setText(""+calib);
    }


    public void startRecorder(){
        if (mRecorder == null)
        {
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            try
            {
                mRecorder.prepare();
            }catch (java.io.IOException ioe) {
                android.util.Log.e("[Monkey]", "IOException: " +
                        android.util.Log.getStackTraceString(ioe));

            }catch (java.lang.SecurityException e) {
                android.util.Log.e("[Monkey]", "SecurityException: " +
                        android.util.Log.getStackTraceString(e));
            }
            try
            {
                mRecorder.start();
            }catch (java.lang.SecurityException e) {
                android.util.Log.e("[Monkey]", "SecurityException: " +
                        android.util.Log.getStackTraceString(e));
            }

            //mEMA = 0.0;
        }

    }
    public void stopRecorder() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public void updateTv(){
        mStatusView.setText(Double.toString((getAmplitudeEMA())));
        String rec = mStatusView.getText().toString();
        EditText tvc= (EditText) findViewById(R.id.cal);
        String cal = tvc.getText().toString();
        double calibrate = Double.parseDouble(cal);
        calib=calibrate;
        TextView tv = (TextView) findViewById(R.id.tv);
        Double rec1 = Double.parseDouble(rec);
        Typeface tf = Typeface.createFromAsset(getAssets(),"digital-7.ttf");
        tv.setTypeface(tf);
        DecimalFormat df = new DecimalFormat("##.##");
        tv.setText(""+df.format(rec1)+" db");
        ImageView im1 = (ImageView) findViewById(R.id.a);
        ImageView im2 = (ImageView) findViewById(R.id.b);
        ImageView im3 = (ImageView) findViewById(R.id.c);
        ImageView im4 = (ImageView) findViewById(R.id.d);
        ImageView im5 = (ImageView) findViewById(R.id.e);
        ImageView im6 = (ImageView) findViewById(R.id.f);
        ImageView im7 = (ImageView) findViewById(R.id.g);
        ImageView im8 = (ImageView) findViewById(R.id.h);
        ImageView im9 = (ImageView) findViewById(R.id.i);
        ImageView im10 = (ImageView) findViewById(R.id.j);
        ImageView im11 = (ImageView) findViewById(R.id.k);
        ImageView im12 = (ImageView) findViewById(R.id.l);
        ImageView im13 = (ImageView) findViewById(R.id.m);
        ImageView im14 = (ImageView) findViewById(R.id.n);
        if(rec1<15000)
            im14.setVisibility(View.GONE);
        else
            im14.setVisibility(View.VISIBLE);
        if(rec1<14000)
            im13.setVisibility(View.GONE);
        else
            im13.setVisibility(View.VISIBLE);
        if(rec1<13000)
            im12.setVisibility(View.GONE);
        else
            im12.setVisibility(View.VISIBLE);
        if(rec1<12000)
            im11.setVisibility(View.GONE);
        else
            im11.setVisibility(View.VISIBLE);
        if(rec1<11000)
            im10.setVisibility(View.GONE);
        else
            im10.setVisibility(View.VISIBLE);
        if(rec1<10000)
            im9.setVisibility(View.GONE);
        else
            im9.setVisibility(View.VISIBLE);
        if(rec1<9000)
            im8.setVisibility(View.GONE);
        else
            im8.setVisibility(View.VISIBLE);
        if(rec1<8000)
            im7.setVisibility(View.GONE);
        else
            im7.setVisibility(View.VISIBLE);
        if(rec1<7000)
            im6.setVisibility(View.GONE);
        else
            im6.setVisibility(View.VISIBLE);
        if(rec1<6000)
            im5.setVisibility(View.GONE);
        else
            im5.setVisibility(View.VISIBLE);
        if(rec1<5000)
            im4.setVisibility(View.GONE);
        else
            im4.setVisibility(View.VISIBLE);
        if(rec1<4000)
            im3.setVisibility(View.GONE);
        else
            im3.setVisibility(View.VISIBLE);
        if(rec1<3000)
            im2.setVisibility(View.GONE);
        else
            im2.setVisibility(View.VISIBLE);
        if(rec1<2000)
            im1.setVisibility(View.GONE);
        else
            im1.setVisibility(View.VISIBLE);
        CheckBox chk= (CheckBox) findViewById(R.id.chkbox);
        boolean box=chk.isChecked();
        if(led!=1) {
            if (rec1 > calib && box)
                turnonflash();
            else
                turnoffflash();
            CheckBox chk1= (CheckBox) findViewById(R.id.chkbox1);
            boolean box1=chk1.isChecked();
            if(box1) {
                LinearLayout l1 = (LinearLayout) findViewById(R.id.l1);
                if(rec1>calib)
                {
                    if(dis==1) {
                        l1.setBackgroundColor(Color.parseColor("#ff0000"));
                        dis++;
                    }
                    else if(dis==2)
                    {
                        l1.setBackgroundColor(Color.parseColor("#ff69b4"));
                        dis++;
                    }
                    else if(dis==3) {
                        l1.setBackgroundColor(Color.parseColor("#551A8B"));
                        dis++;
                    }
                    else if(dis==4) {
                        l1.setBackgroundColor(Color.parseColor("#FFA500"));
                        dis++;
                    }
                    else if(dis==5) {
                        l1.setBackgroundColor(Color.parseColor("#00FFFF"));
                        dis++;
                    }
                    else if(dis==6) {
                        l1.setBackgroundColor(Color.parseColor("#FF00FF"));
                        dis++;
                    }
                    else if(dis==7) {
                        l1.setBackgroundColor(Color.parseColor("#FF0000"));
                        dis++;
                    }
                    else if(dis==8) {
                        l1.setBackgroundColor(Color.parseColor("#00FF00"));
                        dis++;
                    }
                    else if(dis==9) {
                        l1.setBackgroundColor(Color.parseColor("#0000FF"));
                        dis++;
                    }
                    else if(dis==10) {
                        l1.setBackgroundColor(Color.parseColor("#FFFFFF"));
                        dis=1;
                    }
                }
            }
            else
            {
                LinearLayout l1 = (LinearLayout) findViewById(R.id.l1);
                l1.setBackgroundColor(Color.parseColor("#000000"));
            }
        }
    }
    int dis=1;
    public double soundDb(double ampl){
        return  20 * Math.log10(getAmplitudeEMA() / ampl);
    }
    public double getAmplitude() {
        if (mRecorder != null)
            return  (mRecorder.getMaxAmplitude());
        else
            return 0;

    }
    public double getAmplitudeEMA() {
        double amp =  getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }

}
