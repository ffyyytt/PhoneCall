package com.ffyytt.phonecall;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.HashMap;

public class SoundPoolPlayer {
    private SoundPool mShortPlayer= null;
    private HashMap mSounds = new HashMap();

    public SoundPoolPlayer(Context pContext)
    {
        // setup Soundpool
        this.mShortPlayer = new SoundPool(4, AudioManager.STREAM_MUSIC, 0);

        mSounds.put(R.raw.zero, this.mShortPlayer.load(pContext, R.raw.zero, 1));
        mSounds.put(R.raw.one, this.mShortPlayer.load(pContext, R.raw.one, 1));
        mSounds.put(R.raw.two, this.mShortPlayer.load(pContext, R.raw.two, 1));
        mSounds.put(R.raw.three, this.mShortPlayer.load(pContext, R.raw.three, 1));
        mSounds.put(R.raw.four, this.mShortPlayer.load(pContext, R.raw.four, 1));
        mSounds.put(R.raw.five, this.mShortPlayer.load(pContext, R.raw.five, 1));
        mSounds.put(R.raw.six, this.mShortPlayer.load(pContext, R.raw.six, 1));
        mSounds.put(R.raw.seven, this.mShortPlayer.load(pContext, R.raw.seven, 1));
        mSounds.put(R.raw.eight, this.mShortPlayer.load(pContext, R.raw.eight, 1));
        mSounds.put(R.raw.nine, this.mShortPlayer.load(pContext, R.raw.nine, 1));
        mSounds.put(R.raw.deleted, this.mShortPlayer.load(pContext, R.raw.deleted, 1));
    }

    public void playShortResource(int piResource) {
        int iSoundId = (Integer) mSounds.get(piResource);
        this.mShortPlayer.play(iSoundId, 0.99f, 0.99f, 0, 0, 1);
    }

    // Cleanup
    public void release() {
        // Cleanup
        this.mShortPlayer.release();
        this.mShortPlayer = null;
    }
}