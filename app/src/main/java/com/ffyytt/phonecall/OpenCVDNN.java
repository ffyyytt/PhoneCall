package com.ffyytt.phonecall;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.dnn.Net;

import static org.opencv.core.Core.SORT_DESCENDING;
import static org.opencv.core.Core.SORT_EVERY_ROW;
import static org.opencv.core.Core.sortIdx;

public class OpenCVDNN extends Model {
    Net net;
    public void loadweight(String ModelFile, String ConfigFile)
    {
        try
        {
            net = Net.readFromModelOptimizer(ConfigFile, ModelFile);
        }
        finally {

        }
    }
    public String predict(Bitmap bitmap) {
        Mat img = new Mat();
        Bitmap bmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp, img);
        net.setInput(img);
        Mat prob = net.forward();
        Mat sorted_idx = null;
        sortIdx(prob, sorted_idx,SORT_EVERY_ROW + SORT_DESCENDING);
        return String.valueOf(sorted_idx.get(0,0));
    }
}
