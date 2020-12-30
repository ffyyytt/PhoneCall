package com.ffyytt.phonecall;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.LinkedList;
import java.util.List;

public class SURFFLANNMatchingHomography {
    public int run(Bitmap bitmap0, Bitmap bitmap1)
    {
        Mat imgMAT0 = new Mat(bitmap0.getHeight(), bitmap0.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap0.copy(Bitmap.Config.ARGB_8888, true), imgMAT0);

        Mat imgMAT1 = new Mat(bitmap1.getHeight(), bitmap1.getWidth(), CvType.CV_8UC1);
        Utils.bitmapToMat(bitmap1.copy(Bitmap.Config.ARGB_8888, true), imgMAT1);

        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        MatOfKeyPoint keypoints_0 = new MatOfKeyPoint();
        MatOfKeyPoint keypoints_1  = new MatOfKeyPoint();
        detector.detect(imgMAT0, keypoints_0);
        detector.detect(imgMAT1, keypoints_1);

        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB); //2 = SURF;

        Mat descriptor_object = new Mat();
        Mat descriptor_scene = new Mat() ;

        extractor.compute(imgMAT0, keypoints_0, descriptor_object);
        extractor.compute(imgMAT1, keypoints_1, descriptor_scene);

        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING); // 1 = FLANNBASED
        MatOfDMatch matches = new MatOfDMatch();

        matcher.match(descriptor_object, descriptor_scene, matches);
        List<DMatch> matchesList = matches.toList();

        double max_dist = 0.0;
        double min_dist = 100.0;

        for(int i = 0; i < descriptor_object.rows(); i++){
            double dist = (double) matchesList.get(i).distance;
            if(dist < min_dist) min_dist = dist;
            if(dist > max_dist) max_dist = dist;
        }

        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();

        for(int i = 0; i < descriptor_object.rows(); i++){
            if(matchesList.get(i).distance < 100){
                good_matches.addLast(matchesList.get(i));
            }
        }

        return good_matches.size();
    }
}
