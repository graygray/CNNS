package com.graylin.cnns;

import java.net.URL;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class PlayActivity extends Activity implements OnCompletionListener {

	// HTML page
	public String CNNS_SCRIPT_URL = "";

    // XPath query
	public String XPATH = "";
    
    public String cnnScriptContent = "";
	public String vedioPath = "";
	public VideoView mVideoView;
	
	public boolean isgetCNNSTitleOK = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);
		
		Log.e("gray", "PlayActivity.java: START ===============");
		
		vedioPath = MainActivity.getVideoAddress();
		CNNS_SCRIPT_URL = MainActivity.getScriptAddress();
		
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: vedioPath : " + vedioPath);
			Log.e("gray", "PlayActivity.java: CNNS_SCRIPT_URL : " + CNNS_SCRIPT_URL);
		}
		
		mVideoView = (VideoView) findViewById(R.id.videoView_CNNS);
		mVideoView.setOnCompletionListener(this);
		DisplayMetrics dm = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(dm);
		// MyVideoView.WIDTH=dm.widthPixels;
		// MyVideoView.HEIGHT=dm.heightPixels;
		if (vedioPath == "") {
			// Tell the user to provide a media file URL/path.
			Toast.makeText(PlayActivity.this, "video URL/path not exist!", Toast.LENGTH_LONG).show();
			Log.e("gray", "PlayActivity.java: " + "video URL/path not exist!");
			
		} else {

			/*
			 * Alternatively,for streaming media you can use
			 * mVideoView.setVideoURI(Uri.parse(URLstring));
			 */
			mVideoView.setVideoPath(vedioPath);
			// mVideoView.setVideoURI(Uri.parse("android.resource://ss.ss/"+R.raw.main));

			// control bar
			mVideoView.setMediaController(new MediaController(this));
			// mVideoView.requestFocus();
			// start play
			mVideoView.start();
		}
		
		new Thread(new Runnable() 
		{ 
		    @Override
		    public void run() 
		   { 
		        try {
		        	getScriptContent();
		        	if (MainActivity.isDebug) {
		        		Log.e("gray", cnnScriptContent);   
					}
					
				} catch (Exception e) {
					Log.e("gray", "Exception e:" + e.toString());    
					e.printStackTrace();
				}
		   } 
		}).start();
		
		while( !isgetCNNSTitleOK ){
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		setResultText(cnnScriptContent);
		Log.e("gray", "PlayActivity.java: END =================");
	}

	public void getScriptContent() throws Exception {
		
		if (MainActivity.isDebug) {
			Log.e("gray", "MainActivity.java:getScriptContent");
		}
	    
	    // config cleaner properties
	    HtmlCleaner htmlCleaner = new HtmlCleaner();
	    CleanerProperties props = htmlCleaner.getProperties();
	    props.setAllowHtmlInsideAttributes(false);
	    props.setAllowMultiWordAttributes(true);
	    props.setRecognizeUnicodeChars(true);
	    props.setOmitComments(true);
	    
	    // create URL object
	    URL url = new URL(CNNS_SCRIPT_URL);
	    // get HTML page root node
	    TagNode root = htmlCleaner.clean(url);
	
	    // query XPath
	    cnnScriptContent = "";
	    
	    XPATH = "//p[@class='cnnTransSubHead']";
	    Object[] statsNode = root.evaluateXPath(XPATH);
	    // process data if found any node
	    if(statsNode.length > 0) {
	    	
	    	Log.e("gray", "MainActivity.java:getScriptContent, statsNode.length:" + statsNode.length);
	    	
	    	for (int i = 0; i < statsNode.length; i++) {
				
	    		TagNode resultNode = (TagNode)statsNode[i];
	    		// get text data from HTML node
	    		cnnScriptContent += resultNode.getText().toString() + "\n\n";
			}
	        
	    } else {
			Log.e("gray", "PlayActivity.java: " + "statsNode.length < 0");
		}

	    
	    XPATH = "//p[@class='cnnBodyText']";
	    statsNode = root.evaluateXPath(XPATH);
	    // process data if found any node
	    if(statsNode.length > 0) {

	    	if (MainActivity.isDebug) {
	    		Log.e("gray", "MainActivity.java:getScriptContent, statsNode.length:" + statsNode.length);
			}
	    	
	    	for (int i = 0; i < statsNode.length; i++) {
				
	    		TagNode resultNode = (TagNode)statsNode[i];
	    		// get text data from HTML node
	    		cnnScriptContent += resultNode.getText().toString() + "\n\n";
			}
	        
	    } else {
			Log.e("gray", "PlayActivity.java: " + "statsNode.length < 0");
		}
	
	    if (MainActivity.isDebug) {

	    	String [] tsa = new String [100]; 
	    	tsa = cnnScriptContent.split("	");
	    	for (int i = 0; i < tsa.length; i++) {
	    		Log.e("gray", "PlayActivity.java: " + tsa[i]);
	    	}
		}
	    
	    isgetCNNSTitleOK = true;
	    
	}

	public void setResultText(String s){
		
//		final EditText editText_result = (EditText) findViewById(R.id.editText_webContent);
		final TextView editText_result = (TextView) findViewById(R.id.tv_webContent);
		
		// append to end
//		s = s.replaceAll("   ", "\n\n");
		s = s.replaceAll("  ", "\n\n");
		editText_result.setText(s);
//		editText_result.setTextSize(18);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: " + "");
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.play, menu);
		return true;
	}

}
