package com.graylin.cnns;

import java.net.URL;

import org.htmlcleaner.*;



import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;


public class MainActivity extends Activity implements OnCompletionListener {

	public static final String CNNS_URL = "http://transcripts.cnn.com/TRANSCRIPTS/1305/21/sn.01.html";
//	public static  String resultString = "";

	// HTML page
//	static final String BLOG_URL = "http://xjaphx.wordpress.com/";
    static final String BLOG_URL = "http://transcripts.cnn.com/TRANSCRIPTS/1305/31/sn.01.html";
    // XPath query
//    static final String XPATH_STATS = "//div[@id='blog-stats-2']/ul/li";
    static final String XPATH_STATS = "//p[@class='cnnTransSubHead']";
    
    public static String webContent = "";
    
	TextView tvWebContent;
	public static String path = Environment.getExternalStorageDirectory().getPath() + "/asdf.mp4";
//	public static String path = "http://rss.cnn.com/~r/services/podcasting/studentnews/rss/~3/MyMIGVnVhPM/sn-060713.cnn.m4v";
	public static VideoView mVideoView;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mVideoView = (VideoView) findViewById(R.id.videoView_CNNS);
		Log.e("gray", "onCreate ");
		

        // get webpage data 
		
		new Thread(new Runnable() 
		{ 
		    @Override
		    public void run() 
		   { 
		    	
		        try {
//		        	webContent = getBlogStats();
					Log.e("gray", webContent);   
					
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					
					Log.e("gray", "Exception e = " + e.toString());    
					e.printStackTrace();
				}
		        
		   } 
		}).start();
	
  
        Log.e("gray", "end~~ " ); 
        
        
		final Button button_keyevent = (Button) findViewById(R.id.button_test);
		button_keyevent.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
 
            	Log.e("gray", "MainActivity.java:onClick, " + "");
            	setResultText(webContent);
            	
            }
        });
		
	      mVideoView.setOnCompletionListener(this);
	        DisplayMetrics dm = new DisplayMetrics();
	        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
//	        MyVideoView.WIDTH=dm.widthPixels;
//	        MyVideoView.HEIGHT=dm.heightPixels;
	        if (path == "") {
	            // Tell the user to provide a media file URL/path.
	            Toast.makeText(
	            		MainActivity.this,
	                    "Please edit VideoViewDemo Activity, and set path"
	                            + " variable to your media file URL/path",
	                    Toast.LENGTH_LONG).show();

	        } else {

	            /*
	             * Alternatively,for streaming media you can use
	             * mVideoView.setVideoURI(Uri.parse(URLstring));
	             */
	           mVideoView.setVideoPath(path);
//	            mVideoView.setVideoURI(Uri.parse("android.resource://ss.ss/"+R.raw.main));
	            
	            //如果你需要添加控制?就取消改注?
	            mVideoView.setMediaController(new MediaController(this));
//	            mVideoView.requestFocus();
	            //1.6中??自?播放的播放代?是不需要.start()
	            //2.1中??自?播放的播放代?
	            //   mVideoView.start();
	            //所以?了兼容性 我???mVideoView.start();保?所有版本都在?始?自?播放     
	            mVideoView.start();
	        }
	
	}


public String getBlogStats() throws Exception {
	    String stats = "";
	
	    Log.e("gray", "MainActivity.java:getBlogStats, " + "");
	    
	    // config cleaner properties
//	    HtmlCleaner htmlCleaner = new HtmlCleaner();
//	    CleanerProperties props = htmlCleaner.getProperties();
//	    props.setAllowHtmlInsideAttributes(false);
//	    props.setAllowMultiWordAttributes(true);
//	    props.setRecognizeUnicodeChars(true);
//	    props.setOmitComments(true);
//	
//	    // create URL object
//	    URL url = new URL(BLOG_URL);
//	    // get HTML page root node
//	    TagNode root = htmlCleaner.clean(url);
//	
//	    // query XPath
//	    Object[] statsNode = root.evaluateXPath(XPATH_STATS);
//	    // process data if found any node
//	    if(statsNode.length > 0) {
//	    	
//	    	Log.e("gray", "MainActivity.java:getBlogStats, " + "statsNode.length > 0");
//	        // I already know there's only one node, so pick index at 0.
//	        TagNode resultNode = (TagNode)statsNode[0];
//	        // get text data from HTML node
//	        stats = resultNode.getText().toString();
//	    }
	
	    // return value
	    return stats;
	}

	public void setResultText(String s){
		
		final EditText editText_result = (EditText) findViewById(R.id.editText_webContent);
		
		// append to end
		editText_result.append(s);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	   @Override
	    protected void onDestroy() {
	        super.onDestroy();
	        System.exit(0);
	    }
	    @Override
	    public void onCompletion(MediaPlayer mp) {
	    System.out.println("播放完成");
	    }
}
