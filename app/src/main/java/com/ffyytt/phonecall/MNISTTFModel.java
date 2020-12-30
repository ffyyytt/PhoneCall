package com.ffyytt.phonecall;

/*
* Model from https://github.com/ffyyytt/CFB/blob/main/mnist_TF.ipynb
* */


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class MNISTTFModel extends Model {
    private String MODEL_PATH = "model.tflite";
    private String LABEL_PATH = "labels.txt";
    private int DIM_IMG_SIZE_W = 28;
    private int DIM_IMG_SIZE_H = 28;
    private int DIM_PIXEL_SIZE = 1;
    private int BATCH_SIZE = 1;
    private int NUM_CLASSES = 10;


    private List<String> labelList;
    private TFModel model;
    private ByteBuffer imgData = null;
    private int[] intValues = new int[DIM_IMG_SIZE_W * DIM_IMG_SIZE_H];

    MNISTTFModel(Activity activity) throws IOException {
        labelList = loadLabelList(activity);
        model = new TFModel(activity, DIM_IMG_SIZE_W, DIM_IMG_SIZE_H, DIM_PIXEL_SIZE, BATCH_SIZE, NUM_CLASSES, MODEL_PATH);
        imgData =
                ByteBuffer.allocateDirect(4 * DIM_IMG_SIZE_W * DIM_IMG_SIZE_H * DIM_PIXEL_SIZE * BATCH_SIZE);
        imgData.order(ByteOrder.nativeOrder());
    }

    private List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    public String predict(Bitmap bitmap)
    {
        convertBitmapToByteBuffer(bitmap);
        int result = model.predict(imgData)[0];
        return labelList.get(result);
    }

    public String predict_proba(Bitmap bitmap)
    {
        String result = "";
        convertBitmapToByteBuffer(bitmap);
        float[] result_proba = model.predict_proba(imgData)[0];
        for (int i = 0; i < result_proba.length; i++)
        {
            result += String.format("\n%s: %4.2f",labelList.get(i),result_proba[i]);
        }
        return result.substring(1);
    }

    public void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        //bitmap = getResizedBitmap(bitmap, DIM_IMG_SIZE_W, DIM_IMG_SIZE_H);
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_W; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_H; ++j) {
                imgData.putFloat(1-(intValues[pixel++] & 0xFF)/255);
            }
        }
    }

    public void close() {
        model.close();
    }

    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

}
