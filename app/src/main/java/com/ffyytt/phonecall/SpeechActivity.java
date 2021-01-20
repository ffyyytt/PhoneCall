package com.ffyytt.phonecall;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ActivityNotFoundException;
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
import android.speech.RecognizerIntent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class SpeechActivity extends AppCompatActivity implements View.OnTouchListener, SensorEventListener, RecognitionListener {

    LinearLayout linearLayout;

    private SensorManager mSensorManager0;
    private Sensor mProximity;
    private SensorManager mSensorManager1;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    boolean isDrawing;
    Vibrator vibrator;
    GifView gifView;

    SpeechRecognizer recognizer;
    String KWS_SEARCH = "wakeup";
    String FORECAST_SEARCH = "forecast";
    String DIGITS_SEARCH = "digits";
    String PHONE_SEARCH = "phones";
    String MENU_SEARCH = "menu";
    String KEYPHRASE = "hi";


    private MakeCall makeCall = new MakeCall(this);

    AudioRecorder audioRecorder;
    String PhoneNumber = "";
    TextView tv;
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

        tv = (TextView)findViewById(R.id.textView2);
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
                    tv.setText(PhoneNumber);
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
        startListen();
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);

        Toast.makeText(getApplicationContext(),"Ready!!!",Toast.LENGTH_SHORT).show();
    }

    void startListen()
    {
        try {
            Assets assets = new Assets(SpeechActivity.this);
            File assetDir = assets.syncAssets();
            recognizer = SpeechRecognizerSetup.defaultSetup()
                    .setAcousticModel(new File(assetDir, "en-us-ptm"))
                    .setDictionary(new File(assetDir, "cmudict-en-us.dict"))
                    .setKeywordThreshold(1e-5f)
                    .setRawLogDir(assetDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                    .getRecognizer();
            recognizer.addListener(this);
            recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
            File menuGrammar = new File(assetDir, "menu.gram");
            recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);

            // Create grammar-based search for digit recognition
            File digitsGrammar = new File(assetDir, "digits.gram");
            recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);

            // Create language model search
            File languageModel = new File(assetDir, "weather.dmp");
            recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);

            // Phonetic search
            File phoneticModel = new File(assetDir, "en-phone.dmp");
            recognizer.addAllphoneSearch(PHONE_SEARCH, phoneticModel);
            switchSearch(KWS_SEARCH);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                if (!isDrawing) {
                    if (PhoneNumber.length() == 0) {
                        vibrator(500);
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                    }
                    else
                    {
                        makeCall.Call(PhoneNumber,0);
                        recognizer.stop();
                    }
                }
                else
                {
                    exit();
                }
            }
        }
    }

    void exit()
    {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory( Intent.CATEGORY_HOME );
        intent.setFlags(0);
        startActivity(intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.finishAndRemoveTask();
        }
    }

    private void vibrator(long[] pattern)
    {
        vibrator.vibrate(pattern,-1);
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
            isDrawing = true;
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
            isDrawing = false;
            audioRecorder.stop();
            gifView.setImageResource(R.drawable.speechwaiting);

            File file = new File(Environment.getExternalStorageDirectory().getPath(), "PFB");
            Wave w = new Wave(file.getAbsolutePath() + "/AudioRecorder.wav");

            Spectrogram spectrogram = new Spectrogram(w);
            Bitmap bSpect = testSpectrum(this, spectrogram.getNormalizedSpectrogramData());
            //Toast.makeText(getApplicationContext(), surfflannMatchingHomography.run(bSpect, bSpect)+"", Toast.LENGTH_SHORT).show();

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

            float score[] = {getScore(w,w0),
                    getScore(w,w1),
                    getScore(w,w2),
                    getScore(w,w3),
                    getScore(w,w4),
                    getScore(w,w5),
                    getScore(w,w6),
                    getScore(w,w7),
                    getScore(w,w8),
                    getScore(w,w9),
            };
            int maxIdx = 0;
            for (int i = 0; i < score.length; i++)
            {
                if (score[i] > score[maxIdx])
                    maxIdx = i;
            }

            Toast.makeText(getApplicationContext(),"Result: "+maxIdx+", Score: "+score[maxIdx],Toast.LENGTH_SHORT).show();
            PhoneNumber += maxIdx;

            String Result = maxIdx+"";

            switch (Result)
            {
                case "0":
                    playResource(R.raw.zero);
                    gifView.setImageResource(R.drawable.zerogif);
                    vibrator(0);
                    break;
                case "1":
                    playResource(R.raw.one);
                    gifView.setImageResource(R.drawable.onegif);
                    vibrator(100);
                    break;
                case "2":
                    playResource(R.raw.two);
                    gifView.setImageResource(R.drawable.twogif);
                    vibrator(new long[]{0, 100, 100, 100});
                    break;
                case "3":
                    playResource(R.raw.three);
                    gifView.setImageResource(R.drawable.threegif);
                    vibrator(new long[]{0, 100, 100, 100, 100, 100});
                    break;
                case "4":
                    playResource(R.raw.four);
                    gifView.setImageResource(R.drawable.fourgif);
                    vibrator(new long[]{0, 100, 100, 100, 100, 100, 100, 100});
                    break;
                case "5":
                    playResource(R.raw.five);
                    gifView.setImageResource(R.drawable.fivegif);
                    vibrator(new long[]{0, 100, 500, 100});
                    break;
                case "6":
                    playResource(R.raw.six);
                    gifView.setImageResource(R.drawable.sixgif);
                    vibrator(new long[]{0, 500, 100, 500});
                    break;
                case "7":
                    playResource(R.raw.seven);
                    gifView.setImageResource(R.drawable.sevengif);
                    vibrator(new long[]{0, 500, 100, 500, 100, 500});
                    break;
                case "8":
                    playResource(R.raw.eight);
                    gifView.setImageResource(R.drawable.eightgif);
                    vibrator(new long[]{0, 500, 100, 500, 100, 500, 100, 500});
                    break;
                case "9":
                    playResource(R.raw.nine);
                    gifView.setImageResource(R.drawable.ninegif);
                    vibrator(new long[]{0, 100, 500, 100, 500, 100});
                    break;
            }

            tv.setText(PhoneNumber);
            return true;
        }
        return false;
    }

    float getSimilarHomo(Wave w1, Wave w2)
    {
        Spectrogram spectrogram1 = new Spectrogram(w1);
        Spectrogram spectrogram2 = new Spectrogram(w2);
        Bitmap bSpect1 = testSpectrum(this, spectrogram1.getNormalizedSpectrogramData());
        Bitmap bSpect2 = testSpectrum(this, spectrogram2.getNormalizedSpectrogramData());
        return surfflannMatchingHomography.run(bSpect1, bSpect2);
    }
    float getSimilarF(Wave w1, Wave w2)
    {
        return w1.getFingerprintSimilarity(w2).getScore();
    }
    float getScore(Wave w1, Wave w2)
    {
        return getSimilarHomo(w1, w2) + 100*getSimilarF(w1, w2);
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;
        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
        {
            recognizer.stop();
            startSpeechToText(1);
        }
    }

    private void startSpeechToText(int i) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(), getString(R.string.speech_not_supported), Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (resultCode == -1 && data != null) {
                    makeCall.Call(PhoneNumber + data.getStringArrayListExtra("android.speech.extra.RESULTS").get(0).replace(" ",""),0);
                }
                //switchSearch(KEYPHRASE);
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis) {

    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void onTimeout() {

    }
}