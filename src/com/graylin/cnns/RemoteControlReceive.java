package com.graylin.cnns;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class RemoteControlReceive extends BroadcastReceiver{
	
	@Override
    public void onReceive(Context context, Intent intent) {
		if (MainActivity.isDebug) {
			Log.e("gray", "RemoteControlReceive.java:onReceive, " + "");
		}
    	
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
        	
            KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if ((KeyEvent.KEYCODE_MEDIA_PLAY == event.getKeyCode()) && (event.getAction() == KeyEvent.ACTION_DOWN)) {
            
            	if (MainActivity.isDebug) {
            		Log.e("gray", "RemoteControlReceive.java:onReceive, " + "KEYCODE_MEDIA_PLAY");
        		}
            	PlayVideoService.getBroadcastToPlay = true;
            	
            } else if ((KeyEvent.KEYCODE_MEDIA_PAUSE == event.getKeyCode()) && (event.getAction() == KeyEvent.ACTION_DOWN)) {

            	if (MainActivity.isDebug) {
            		Log.e("gray", "RemoteControlReceive.java:onReceive, " + "KEYCODE_MEDIA_PAUSE");
        		}
            	PlayVideoService.getBroadcastToPause = true;
            	
            }  else if ((KeyEvent.KEYCODE_MEDIA_FAST_FORWARD == event.getKeyCode()) && (event.getAction() == KeyEvent.ACTION_DOWN)) {

            	if (MainActivity.isDebug) {
            		Log.e("gray", "RemoteControlReceive.java:onReceive, " + "KEYCODE_MEDIA_FAST_FORWARD");
        		}
//            	PlayVideoService.getBroadcastToFFaward = true;
            	PlayActivity.isStopService = true;
            	
            } else if ((KeyEvent.KEYCODE_MEDIA_REWIND == event.getKeyCode()) && (event.getAction() == KeyEvent.ACTION_DOWN)) {

            	if (MainActivity.isDebug) {
            		Log.e("gray", "RemoteControlReceive.java:onReceive, " + "KEYCODE_MEDIA_REWIND");
        		}
            	PlayVideoService.getBroadcastToRewind = true;
            
	        }  else if ((KeyEvent.KEYCODE_MEDIA_NEXT == event.getKeyCode()) && (event.getAction() == KeyEvent.ACTION_DOWN)) {
	
	        	if (MainActivity.isDebug) {
	        		Log.e("gray", "RemoteControlReceive.java:onReceive, " + "KEYCODE_MEDIA_NEXT");
	    		}
//	        	PlayVideoService.getBroadcastToNext = true;
	        	PlayActivity.isStopService = true;
	        	
	        } else if ((KeyEvent.KEYCODE_MEDIA_PREVIOUS == event.getKeyCode()) && (event.getAction() == KeyEvent.ACTION_DOWN)) {
	
	        	if (MainActivity.isDebug) {
	        		Log.e("gray", "RemoteControlReceive.java:onReceive, " + "KEYCODE_MEDIA_PREVIOUS");
	    		}
	        	PlayVideoService.getBroadcastToPreviious = true;
	        }
        }
    }
}
