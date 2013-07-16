package com.example.cnns;

import java.net.URL;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CleanerTransformations;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagTransformation;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class PlayActivity extends Activity implements OnCompletionListener {

	// HTML page
//	public static String CNNS_SCRIPT_URL = "http://transcripts.cnn.com/TRANSCRIPTS/1305/21/sn.01.html";
	public static String CNNS_SCRIPT_URL = "";
//	public static  String resultString = "";

    // XPath query
//    static final String XPATH = "//div[@id='blog-stats-2']/ul/li";
//    static final String XPATH = "//p[@class='cnnTransSubHead']";
	public static String XPATH = "";
    
    public static String webContent = "";
    
	TextView tvWebContent;
//	public static String vedioPath = Environment.getExternalStorageDirectory().getPath() + "/asdf.mp4";
//	public static String vedioPath = "http://podcasts.cnn.net/cnn/big/podcasts/studentnews/video/2013/06/06/sn-060713.cnn.m4v";
	public static String vedioPath = "";
	public static VideoView mVideoView;
	
	boolean isgetCNNSTitleOK = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);
		
		Log.e("gray", "onCreate ");
		
		vedioPath = MainActivity.getVideoAddress();
		CNNS_SCRIPT_URL = MainActivity.getScriptAddress();
		
		Log.e("gray", "PlayActivity.java: vedioPath : " + vedioPath);
		Log.e("gray", "PlayActivity.java: CNNS_SCRIPT_URL : " + CNNS_SCRIPT_URL);
		
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
					Log.e("gray", webContent);   
					
				} catch (Exception e) {
					Log.e("gray", "Exception e = " + e.toString());    
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
		setResultText(webContent);
		
//		final Button button_keyevent = (Button) findViewById(R.id.button_test);
//		button_keyevent.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
// 
//            	Log.e("gray", "MainActivity.java:onClick, " + "");
//            	setResultText(webContent);
//            	
//            }
//        });
		
		Log.e("gray", "end~~ " ); 
	}

	public void getScriptContent() throws Exception {
		
	    Log.e("gray", "MainActivity.java:getScriptContent");
	    
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
	    webContent = "";
	    
	    XPATH = "//p[@class='cnnTransSubHead']";
	    Object[] statsNode = root.evaluateXPath(XPATH);
	    // process data if found any node
	    if(statsNode.length > 0) {
	    	
	    	Log.e("gray", "MainActivity.java:getScriptContent, statsNode.length:" + statsNode.length);
	    	
	    	for (int i = 0; i < statsNode.length; i++) {
				
	    		TagNode resultNode = (TagNode)statsNode[i];
	    		// get text data from HTML node
	    		webContent += resultNode.getText().toString() + "\n\n";
			}
	        
	    } else {
			Log.e("gray", "PlayActivity.java: " + "statsNode.length < 0");
		}

	    
	    XPATH = "//p[@class='cnnBodyText']";
	    statsNode = root.evaluateXPath(XPATH);
	    // process data if found any node
	    if(statsNode.length > 0) {
	    	
	    	Log.e("gray", "MainActivity.java:getScriptContent, statsNode.length:" + statsNode.length);
	    	
	    	for (int i = 0; i < statsNode.length; i++) {
				
	    		TagNode resultNode = (TagNode)statsNode[i];
	    		// get text data from HTML node
	    		webContent += resultNode.getText().toString() + "\n\n";
			}
	        
	    } else {
			Log.e("gray", "PlayActivity.java: " + "statsNode.length < 0");
		}
	
	    
	    
	    String [] tsa = new String [100]; 
	    tsa = webContent.split("	");
	    
	    for (int i = 0; i < tsa.length; i++) {
			Log.e("gray", "PlayActivity.java: " + tsa[i]);
		}
	    
	    isgetCNNSTitleOK = true;
	    
	}

	public void setResultText(String s){
		
//		final EditText editText_result = (EditText) findViewById(R.id.editText_webContent);
		final TextView editText_result = (TextView) findViewById(R.id.tv_webContent);
		
		// append to end
//		editText_result.append(s);
		editText_result.setText(s);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.exit(0);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		System.out.println("play finished!");
	}
	
	public boolean onKeyUp(int keyCode, KeyEvent event) {  
	    if(keyCode==4){
	    	
	    	if (mVideoView.isPlaying()) {
	    		mVideoView.stopPlayback();
            }
	    	
	    	Log.e("gray", "PlayActivity.java: " + "Back key code is 4");
//	    	finish();
	    	PlayActivity.this.finish();
	    	Intent intent = new Intent();
	    	intent.setClass(PlayActivity.this, MainActivity.class);
	    	startActivity(intent);
	    }
		
	    return super.onKeyDown(keyCode, event);  
//	    return true; 
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.play, menu);
		return true;
	}

}
