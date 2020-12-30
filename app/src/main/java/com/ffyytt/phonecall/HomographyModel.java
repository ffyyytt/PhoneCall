package com.ffyytt.phonecall;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class HomographyModel extends Model {
    SURFFLANNMatchingHomography surfflannMatchingHomography;
    public String predict(Context context, Bitmap bitmap)
    {
        int[] score = score(context, bitmap);
        int maxIds = 0;
        for (int i = 0; i < score.length; i++)
        {
            if (score[i]> score[maxIds])
            {
                maxIds = i;
            }
        }
        return maxIds+"";
    }
    public int[] score(Context context, Bitmap bitmap)
    {
        int[] result = new int[10];
        result[0] = getScore(context, R.drawable.zero, bitmap);
        result[1] = getScore(context, R.drawable.one, bitmap);
        result[2] = getScore(context, R.drawable.two, bitmap);
        result[3] = getScore(context, R.drawable.three, bitmap);
        result[4] = getScore(context, R.drawable.four, bitmap);
        result[5] = getScore(context, R.drawable.five, bitmap);
        result[6] = getScore(context, R.drawable.six, bitmap);
        result[7] = getScore(context, R.drawable.seven, bitmap);
        result[8] = getScore(context, R.drawable.eight, bitmap);
        result[9] = getScore(context, R.drawable.nine, bitmap);
        return result;
    }

    public int[] score(Context context, Bitmap bitmap, SQLiteDatabase db)
    {
        int[] result = new int[10];
        Cursor cursor;
        byte[] bArray;
        Bitmap img;

        cursor = db.rawQuery("SELECT * FROM student WHERE id==0", null);
        cursor.moveToFirst();
        bArray = cursor.getBlob(cursor.getColumnIndex("IMAGE"));
        img = BitmapFactory.decodeByteArray(bArray, 0, bArray.length);
        result[0] = surfflannMatchingHomography.run(img, bitmap);


        cursor = db.rawQuery("SELECT * FROM student WHERE id==1", null);
        cursor.moveToFirst();
        bArray = cursor.getBlob(cursor.getColumnIndex("IMAGE"));
        img = BitmapFactory.decodeByteArray(bArray, 0, bArray.length);
        result[1] = surfflannMatchingHomography.run(img, bitmap);

        cursor = db.rawQuery("SELECT * FROM student WHERE id==2", null);
        cursor.moveToFirst();
        bArray = cursor.getBlob(cursor.getColumnIndex("IMAGE"));
        img = BitmapFactory.decodeByteArray(bArray, 0, bArray.length);
        result[2] = surfflannMatchingHomography.run(img, bitmap);

        cursor = db.rawQuery("SELECT * FROM student WHERE id==3", null);
        cursor.moveToFirst();
        bArray = cursor.getBlob(cursor.getColumnIndex("IMAGE"));
        img = BitmapFactory.decodeByteArray(bArray, 0, bArray.length);
        result[3] = surfflannMatchingHomography.run(img, bitmap);

        cursor = db.rawQuery("SELECT * FROM student WHERE id==4", null);
        cursor.moveToFirst();
        bArray = cursor.getBlob(cursor.getColumnIndex("IMAGE"));
        img = BitmapFactory.decodeByteArray(bArray, 0, bArray.length);
        result[4] = surfflannMatchingHomography.run(img, bitmap);

        cursor = db.rawQuery("SELECT * FROM student WHERE id==5", null);
        cursor.moveToFirst();
        bArray = cursor.getBlob(cursor.getColumnIndex("IMAGE"));
        img = BitmapFactory.decodeByteArray(bArray, 0, bArray.length);
        result[5] = surfflannMatchingHomography.run(img, bitmap);

        cursor = db.rawQuery("SELECT * FROM student WHERE id==6", null);
        cursor.moveToFirst();
        bArray = cursor.getBlob(cursor.getColumnIndex("IMAGE"));
        img = BitmapFactory.decodeByteArray(bArray, 0, bArray.length);
        result[6] = surfflannMatchingHomography.run(img, bitmap);

        cursor = db.rawQuery("SELECT * FROM student WHERE id==7", null);
        cursor.moveToFirst();
        bArray = cursor.getBlob(cursor.getColumnIndex("IMAGE"));
        img = BitmapFactory.decodeByteArray(bArray, 0, bArray.length);
        result[7] = surfflannMatchingHomography.run(img, bitmap);

        cursor = db.rawQuery("SELECT * FROM student WHERE id==8", null);
        cursor.moveToFirst();
        bArray = cursor.getBlob(cursor.getColumnIndex("IMAGE"));
        img = BitmapFactory.decodeByteArray(bArray, 0, bArray.length);
        result[8] = surfflannMatchingHomography.run(img, bitmap);

        cursor = db.rawQuery("SELECT * FROM student WHERE id==9", null);
        cursor.moveToFirst();
        bArray = cursor.getBlob(cursor.getColumnIndex("IMAGE"));
        img = BitmapFactory.decodeByteArray(bArray, 0, bArray.length);
        result[9] = surfflannMatchingHomography.run(img, bitmap);

        return result;
    }

    public int getScore(Context context, int resID, Bitmap bitmap)
    {
        Bitmap img = BitmapFactory.decodeResource(context.getResources(), resID);
        return surfflannMatchingHomography.run(img, bitmap);
    }
}
