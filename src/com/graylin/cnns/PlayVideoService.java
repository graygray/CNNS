package com.graylin.cnns;

import java.io.IOException;

import android.app.IntentService;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;

public class PlayVideoService extends IntentService {

	public MediaPlayer mp = null;
	public static boolean getBroadcastToPlay = false;
	public static boolean getBroadcastToPause = false;
	public static boolean getBroadcastToNext = false;
	public static boolean getBroadcastToPreviious = false;
	public static boolean getBroadcastToFFaward = false;
	public static boolean getBroadcastToRewind = false;
	public boolean isMediaPlayerRun = false;
	
	public PlayVideoService() {
		super("PlayVideoService");
	}
	
	@Override
	protected void onHandleIntent(Intent arg0) {
		
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayVideoService.java:onHandleIntent, " + "");
		}
		
		playVideo();
		
		// end while will go to onDestroy()
		while (!PlayActivity.isStopService ) {
			
			if (getBroadcastToPlay && !isMediaPlayerRun ) {
				getBroadcastToPlay = false;
				playVideo();
			}
			
			if (getBroadcastToPause && isMediaPlayerRun) {
				getBroadcastToPause = false;
				stopVideo();
			}
			
			if ( mp != null) {
				PlayActivity.stopPosition = mp.getCurrentPosition();
				
				if (getBroadcastToPreviious || getBroadcastToRewind) {
					getBroadcastToPreviious = false;
					getBroadcastToRewind = false;
					PlayActivity.stopPosition -= 5000;
					mp.seekTo(PlayActivity.stopPosition);
				}
				// don't do fast forward
//				else if (getBroadcastToNext || getBroadcastToFFaward) {
//					getBroadcastToNext = false;
//					getBroadcastToFFaward = false;
//					PlayActivity.stopPosition += 2000;
//					mp.seekTo(PlayActivity.stopPosition);
//				} 
			}
			
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void playVideo() {
		
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayVideoService.java:playVideo, " + "");
		}
		mp = new MediaPlayer();
		mp.setLooping(true);
		
		try {
			mp.setDataSource(PlayActivity.cnnVideoPath);
			mp.prepare();
			mp.seekTo(PlayActivity.stopPosition);
			mp.start();
			
			PlayActivity.isVideoPlaying = true;
			isMediaPlayerRun = true;
		} catch (IllegalArgumentException e) {
			Log.e("gray", "PlayVideoService.java:onHandleIntent, " + "IllegalArgumentException");
			e.printStackTrace();
		} catch (SecurityException e) {
			Log.e("gray", "PlayVideoService.java:onHandleIntent, " + "SecurityException");
			e.printStackTrace();
		} catch (IllegalStateException e) {
			Log.e("gray", "PlayVideoService.java:onHandleIntent, " + "IllegalStateException");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("gray", "PlayVideoService.java:onHandleIntent, " + "IOException");
			e.printStackTrace();
		}
	}
	
	public void stopVideo() {
		
		if (mp !=  null) {
			
			if (MainActivity.isDebug) {
				Log.e("gray", "PlayVideoService.java:stopVideo, " + "");
			}
			
			mp.stop();
			mp.release();
			mp = null;
			
			PlayActivity.isVideoPlaying = false;
			isMediaPlayerRun = false;
		}
	}
	
	@Override
    public void onDestroy() {
		
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayVideoService.java:onDestroy, " + "");
		}
		
		stopVideo();
    }
	
}

