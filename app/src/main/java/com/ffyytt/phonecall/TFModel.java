package com.ffyytt.phonecall;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collections;

public class TFModel {
    protected Activity activity;
    protected int INPUT_IMG_W;
    protected int INPUT_IMG_H;
    protected int INPUT_IMG_P;
    protected int BATCH_SIZE;
    protected int NUM_CLASSES;

    protected String MODEL_PATH;

    private Interpreter tflite;

    private float[][] result_predict_proba;
    private int[] result_predict;

    TFModel(Activity _activity, int input_img_w, int input_img_h, int input_img_p, int batch_size, int num_classes, String model_path) throws IOException {
        INPUT_IMG_W = input_img_w;
        INPUT_IMG_H = input_img_h;
        INPUT_IMG_P = input_img_p;
        BATCH_SIZE = batch_size;
        activity = _activity;
        MODEL_PATH = model_path;
        NUM_CLASSES = num_classes;

        prepare();
    }

    private void prepare() throws IOException {
        tflite = new Interpreter(loadModelFile(activity));
        result_predict_proba = new float[BATCH_SIZE][NUM_CLASSES];
        result_predict = new int[BATCH_SIZE];
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float[][] predict_proba(ByteBuffer byteBuffer) {
        float[][] result = result_predict_proba.clone();
        if (tflite == null) {
            return result;
        }
        tflite.run(byteBuffer, result);
        return result;
    }

    private int OneHotEncoding(float[] proba)
    {
        int maxIdx = 0;
        for (int i = 0; i < proba.length; i++)
        {
            if (proba[i] > proba[maxIdx])
            {
                maxIdx = i;
            }
        }
        return maxIdx;
    }

    public int[] predict(ByteBuffer byteBuffer)
    {
        int[] result = result_predict.clone();
        float[][] result_proba = result_predict_proba.clone();
        if (tflite == null) {
            return result;
        }
        result_proba = predict_proba(byteBuffer);
        for (int i = 0; i < BATCH_SIZE; i++)
        {
            result[i] = OneHotEncoding(result_proba[i]);
        }
        return result;
    }

    public void close() {
        tflite.close();
        tflite = null;
    }
}
