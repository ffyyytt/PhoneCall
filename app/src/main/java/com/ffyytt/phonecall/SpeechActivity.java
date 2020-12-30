package com.ffyytt.phonecall;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.musicg.fingerprint.FingerprintSimilarity;
import com.musicg.wave.Wave;
import com.musicg.wave.extension.Spectrogram;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class SpeechActivity extends AppCompatActivity implements View.OnTouchListener, SensorEventListener {

    LinearLayout linearLayout;

    private SensorManager mSensorManager0;
    private Sensor mProximity;
    private SensorManager mSensorManager1;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    Vibrator vibrator;
    GifView gifView;

    AudioRecorder audioRecorder;
    String PhoneNumber = "";
    SURFFLANNMatchingHomography surfflannMatchingHomography = new SURFFLANNMatchingHomography();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        gifView = (GifView)findViewById(R.id.GV);
        gifView.setImageResource(R.drawable.speechwaiting);

        linearLayout = (LinearLayout)findViewById(R.id.Speech_LinearLayout);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mSensorManager0 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager0.getDefaultSensor(Sensor.TYPE_PROXIMITY);


        mSensorManager1 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager1.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeDetector();
        mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake(int count) {
                if (PhoneNumber.length() > 0)
                {
                    PhoneNumber = PhoneNumber.substring(0, PhoneNumber.length()-1);
                    Toast.makeText(getApplicationContext(), PhoneNumber, Toast.LENGTH_SHORT).show();
                    playResource(R.raw.deleted);
                }
                else
                {
                    vibrator(500);
                }
            }
        });

        linearLayout.setOnTouchListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager0.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager1.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager0.unregisterListener(this);
        mSensorManager1.unregisterListener(mShakeDetector);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            if (event.values[0] == 0) {
                if (PhoneNumber.length() == 0) {
                    vibrator(500);
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void vibrator(int milliseconds)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            vibrator.vibrate(milliseconds);
        }
    }

    void playResource(int resID)
    {
        MediaPlayer mediaPlayer= MediaPlayer.create(this,resID);
        mediaPlayer.start();
    }

    boolean exists(String path)
    {
        File file = new File(path);
        if(file.exists())
            return true;
        else
            return false;
    }

    public Bitmap testSpectrum(Context context, double[][] data) {
        Paint paint = new Paint();
        Bitmap bmp;
        if (data != null) {
            paint.setStrokeWidth(1);
            int width = data.length;
            int height = data[0].length;
            int[] arrayCol = new int[width * height];
            int counter = 0;
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int value;
                    int color;
                    value = 255 - (int) (data[j][i] * 255);
                    color = (value << 16 | value << 8 | value | 255 << 24);
                    arrayCol[counter] = color;
                    counter++;
                }
            }
            return Bitmap.createBitmap(arrayCol, width, height, Bitmap.Config.ARGB_8888);
        }
        return null;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_DOWN) {
            gifView.setImageResource(R.drawable.listening);
            Toast.makeText(getApplicationContext(), "STARTED", Toast.LENGTH_SHORT).show();
            try {
                audioRecorder = new AudioRecorder("");
                audioRecorder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        else if (action == MotionEvent.ACTION_MOVE) {
            return true;
        }
        else if (action == MotionEvent.ACTION_UP) {
            audioRecorder.stop();
            gifView.setImageResource(R.drawable.speechwaiting);

            File file = new File(Environment.getExternalStorageDirectory().getPath(), "PFB");
            Wave w = new Wave(file.getAbsolutePath() + "/AudioRecorder.wav");

            Spectrogram spectrogram = new Spectrogram(w);
            Bitmap bSpect = testSpectrum(this, spectrogram.getNormalizedSpectrogramData());
            Toast.makeText(getApplicationContext(), surfflannMatchingHomography.run(bSpect, bSpect)+"", Toast.LENGTH_SHORT).show();

            for (int i = 0; i < 10; i++)
            {
                if (!exists(file.getAbsolutePath()+"/"+i+".wav")) {
                    Toast.makeText(getApplicationContext(), "File not found!", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }

            Wave w0 = new Wave(file.getAbsolutePath() + "/0.wav");
            Wave w1 = new Wave(file.getAbsolutePath() + "/1.wav");
            Wave w2 = new Wave(file.getAbsolutePath() + "/2.wav");
            Wave w3 = new Wave(file.getAbsolutePath() + "/3.wav");
            Wave w4 = new Wave(file.getAbsolutePath() + "/4.wav");
            Wave w5 = new Wave(file.getAbsolutePath() + "/5.wav");
            Wave w6 = new Wave(file.getAbsolutePath() + "/6.wav");
            Wave w7 = new Wave(file.getAbsolutePath() + "/7.wav");
            Wave w8 = new Wave(file.getAbsolutePath() + "/8.wav");
            Wave w9 = new Wave(file.getAbsolutePath() + "/9.wav");

            float score[] = {w.getFingerprintSimilarity(w0).getScore(),
                    w.getFingerprintSimilarity(w1).getScore(),
                    w.getFingerprintSimilarity(w2).getScore(),
                    w.getFingerprintSimilarity(w3).getScore(),
                    w.getFingerprintSimilarity(w4).getScore(),
                    w.getFingerprintSimilarity(w5).getScore(),
                    w.getFingerprintSimilarity(w6).getScore(),
                    w.getFingerprintSimilarity(w7).getScore(),
                    w.getFingerprintSimilarity(w8).getScore(),
                    w.getFingerprintSimilarity(w9).getScore()};
            int maxIdx = 0;
            for (int i = 0; i < score.length; i++)
            {
                if (score[i] > score[maxIdx])
                    maxIdx = i;
            }

            Toast.makeText(getApplicationContext(),"Result: "+maxIdx+", Score: "+score[maxIdx],Toast.LENGTH_SHORT).show();

            return true;
        }
        return false;
    }
}