package com.ffyytt.phonecall;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements  View.OnTouchListener, SensorEventListener, View.OnLongClickListener {

    private static final int INPUT_SIZE = 28;
    private DrawModel drawModel;
    private DrawView drawView;
    private GifView gifView;
    private LinearLayout linearLayout;
    private PointF mTmpPiont = new PointF();

    private SensorManager mSensorManager0;
    private Sensor mProximity;
    private SensorManager mSensorManager1;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    Vibrator vibrator;

    private static final int SENSOR_SENSITIVITY = 1;

    private float mLastX;
    private float mLastY;

    String PhoneNumber = "";
    private MakeCall makeCall = new MakeCall(this);
    private boolean isDrawing = false;

    MNISTTFModel mnistClassification;
    MNISTDT mnistdt;
    MNISTXG mnistxg;
    HomographyModel homographyModel;
    ModelServer modelServer;
    OpenCVDNN openCVDNN;

    int ModelType = 1;
    String Result;
    Random random = new Random();

    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        requestWindowFeature(Window.FEATURE_NO_TITLE); //will hide the title
        getSupportActionBar().hide(); // hide the title bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        gifView = (GifView) findViewById(R.id.GifV);
        gifView.setImageResource(R.drawable.waiting);

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (OpenCVLoader.initDebug())
        {
            Toast.makeText(getApplicationContext(), "OpenCV load Successful!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(getApplicationContext(), "OpenCV load Failed!", Toast.LENGTH_SHORT).show();
        }

        requestPermission();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        initDatabase();

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

        try {
            mnistClassification = new MNISTTFModel(this);
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "BUG", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        drawView = (DrawView) findViewById(R.id.draw);
        drawModel = new DrawModel(INPUT_SIZE, INPUT_SIZE);

        drawView.setModel(drawModel);
        drawView.setOnTouchListener(this);

        linearLayout = (LinearLayout) findViewById(R.id.linearlayout);
        linearLayout.setOnLongClickListener(this);

    }

    @Override
    protected void onResume() {
        drawView.onResume();
        super.onResume();
        mSensorManager0.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager1.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        drawView.onPause();
        super.onPause();
        mSensorManager0.unregisterListener(this);
        mSensorManager1.unregisterListener(mShakeDetector);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_DOWN) {
            isDrawing = true;
            processTouchDown(event);
            return true;

        } else if (action == MotionEvent.ACTION_MOVE) {
            processTouchMove(event);
            return true;

        } else if (action == MotionEvent.ACTION_UP) {
            isDrawing = false;
            processTouchUp();
            return true;
        }
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_PROXIMITY)
        {
            if (event.values[0] == 0)
            {
                if (!isDrawing)
                {
                    if (PhoneNumber.length() > 0)
                    {
                        makeCall.Call(PhoneNumber,0);
                    }
                    else
                    {
                        vibrator(500);
                        Intent intent = new Intent(this, SpeechActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                    }
                }
                else
                {
                    exit();
                }
            }
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public boolean onLongClick(View view) {
        if(view.getId() == R.id.linearlayout || view.getId() == R.id.draw)
        {
            exit();
        }
        return false;
    }

    void exit()
    {
        mnistClassification.close();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory( Intent.CATEGORY_HOME );
        intent.setFlags(0);
        startActivity(intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.finishAndRemoveTask();
        }
    }

    void playResource(int resID)
    {
        MediaPlayer mediaPlayer= MediaPlayer.create(this,resID);
        mediaPlayer.start();
    }

    private void initDatabase() {
        db = openOrCreateDatabase("images", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS " + "IMAGES" + " (id INTEGER PRIMARY KEY AUTOINCREMENT,image BLOB)");
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    void requestPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
        }
    }

    private void processTouchDown(MotionEvent event) {
        mLastX = event.getX();
        mLastY = event.getY();
        drawView.calcPos(mLastX, mLastY, mTmpPiont);
        float lastConvX = mTmpPiont.x;
        float lastConvY = mTmpPiont.y;
        drawModel.startLine(lastConvX, lastConvY);
    }

    private void processTouchMove(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        drawView.calcPos(x, y, mTmpPiont);
        float newConvX = mTmpPiont.x;
        float newConvY = mTmpPiont.y;
        drawModel.addLineElem(newConvX, newConvY);

        mLastX = x;
        mLastY = y;
        drawView.invalidate();
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

    private void SaveBitmapToFile(Bitmap bitmap, String Filename)
    {
        String path = Environment.getExternalStorageDirectory().toString();
        OutputStream fOut = null;
        Integer counter = 0;
        File file = new File(path, Filename); // the File to save , append increasing numeric counter to prevent files from getting overwritten.
        try {
            fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
            fOut.flush(); // Not really required
            fOut.close(); // do not forget to close the stream
            MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void SaveBitmapToDB(Bitmap bitmap, String id)
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] bArray = bos.toByteArray();

        ContentValues cvalues = new ContentValues();
        cvalues.put("id", id);
        cvalues.put("image", bArray);

        int idf = (int) db.insertWithOnConflict("IMAGE", null, cvalues, SQLiteDatabase.CONFLICT_IGNORE);
        if (idf == -1) {
            db.update("IMAGE", cvalues, "id=?", new String[] {id});  // number 1 is the _id here, update to variable for your code
        }
    }

    private void vibrator(long[] pattern)
    {
        vibrator.vibrate(pattern,-1);
    }

    private void processTouchUp() {
        drawModel.endLine();

        //predict
        switch (ModelType)
        {
            case 1: //TFlite
                Result = mnistClassification.predict(drawView.getmOffscreenBitmap());
                break;
            case 2: //GradientBoostingDecisionTree LightGBM
                Result = mnistdt.predict(drawView.getmOffscreenBitmap(), INPUT_SIZE, INPUT_SIZE);
                break;
            case 3: //Homography
                Result = homographyModel.predict(getApplicationContext(), drawView.getmOffscreenBitmap());
                break;
            case 4: //Server
                if (isNetworkConnected()) {
                    Result = modelServer.predict(drawView.getmOffscreenBitmap());
                    if (Result != "-1") {
                        break;
                    }
                }
            case 5: //GradientBoostingDecisionTree XGBoost
                Result = mnistxg.predict(drawView.getmOffscreenBitmap(), INPUT_SIZE, INPUT_SIZE);;
                break;
            case 6: //Support Vector Machine
                // https://github.com/ffyyytt/CFB/blob/main/mnist_SVM.ipynb
                // DNN Net openCV support onnx, caffe, tensorflow, pytorch,...
                openCVDNN.loadweight("file:///android_assets/SVM.onnx", "file:///android_asset/labels.txt");
                break;
            case 7: //Caffe Model
                //openCVDNN.loadweight("file://assets/model.caffemodel", "file://assets/labels.txt");
                //break;
            default:
                Result = mnistClassification.predict(drawView.getmOffscreenBitmap());
        }

        //Save user's random samples
        if (random.nextInt(1000) == 999)
        {
            SaveBitmapToFile(drawView.getmOffscreenBitmap(), "PhoneCall/"+Result+".jpg");
            SaveBitmapToDB(drawView.getmOffscreenBitmap(), Result);
        }

        PhoneNumber += Result;
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

        //Toast.makeText(getApplicationContext(), mnistClassification.predict_proba(drawView.getmOffscreenBitmap()), Toast.LENGTH_SHORT).show();
        Toast.makeText(getApplicationContext(), Result, Toast.LENGTH_SHORT).show();
        //reset
        drawModel.clear();
        drawView.reset();
        drawView.invalidate();
    }
}