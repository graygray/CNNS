package com.graylin.cnns;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class PlayActivity extends Activity implements OnCompletionListener {

	// HTML page
	public String cnnScriptPath = "";
    // XPath query
	public String XPATH = "";
    
    public String cnnScriptContent = "";
    public String cnnVideoPath = "";
	public String cnnVideoName = "";
	public VideoView mVideoView;
	public ProgressDialog mProgressDialog;
	
	public TextView mTextView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);
		
		cnnVideoPath = MainActivity.getVideoAddress();
		cnnScriptPath = MainActivity.getScriptAddress();
		
		String [] tempSA = new String [20];
		tempSA = cnnVideoPath.split("/");
		
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: arrayLength:" + tempSA.length);
			for (int i = 0; i < tempSA.length; i++) {
				Log.e("gray", "MainActivity.java: " + tempSA[i]);
			}
		}
		
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: START ===============");
			Log.e("gray", "PlayActivity.java: cnnVideoPath : " + cnnVideoPath);
			Log.e("gray", "PlayActivity.java: cnnScriptPath : " + cnnScriptPath);
		}
		
		cnnVideoName = tempSA[tempSA.length - 1];
		if (isFileExist(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName)) {
			cnnVideoPath = Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName;
		}
		
		mVideoView = (VideoView) findViewById(R.id.videoView_CNNS);
		mVideoView.setOnCompletionListener(this);
		DisplayMetrics dm = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(dm);
		// MyVideoView.WIDTH=dm.widthPixels;
		// MyVideoView.HEIGHT=dm.heightPixels;
		if (cnnVideoPath == "") {
			// Tell the user to provide a media file URL/path.
			Toast.makeText(PlayActivity.this, "video URL/path not exist!", Toast.LENGTH_LONG).show();
			Log.e("gray", "PlayActivity.java: " + "video URL/path not exist!");
			
		} else {
			/*
			 * Alternatively,for streaming media you can use
			 * mVideoView.setVideoURI(Uri.parse(URLstring));
			 */
			mVideoView.setVideoPath(cnnVideoPath);
			// mVideoView.setVideoURI(Uri.parse("android.resource://ss.ss/"+R.raw.main));

			// control bar
			mVideoView.setMediaController(new MediaController(this));
			// mVideoView.requestFocus();
			// start play
			mVideoView.start();
		}
		
		if (isFileExist(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName+".txt")) {
			try {
				cnnScriptContent =  readFileAsString( Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName+".txt");
				setResultText(cnnScriptContent);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			
			ConnectivityManager conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo networInfo = conManager.getActiveNetworkInfo();
			if (networInfo == null || !networInfo.isAvailable()){
				
				Log.e("gray", "MainActivity.java, NO CONNECTIVITY_SERVICE");
				AlertDialog.Builder dialog = new AlertDialog.Builder(PlayActivity.this);
		        dialog.setTitle("Alert Message");
		        dialog.setMessage("No Availiable Network!!");
		        dialog.show();
			
			} else {
				
				final CharSequence strDialogTitle = "Please Wait...";
				final CharSequence strDialogBody = "Loading Video & Script...";
				mProgressDialog = ProgressDialog.show(PlayActivity.this, strDialogTitle, strDialogBody, true);
				
				new Thread(new Runnable() 
				{ 
					@Override
					public void run() 
					{ 
						try {
							getScriptContent();
							handler.sendEmptyMessage(0);
							if (MainActivity.isDebug) {
								Log.e("gray", "PlayActivity.java:run, " + cnnScriptContent);
							}
						} catch (Exception e) {
							Log.e("gray", "PlayActivity.java:run, Exception e:" + e.toString());    
							e.printStackTrace();
						}
					} 
				}).start();
			}
		}
		
//		mTextView = (TextView) findViewById(R.id.tv_webContent);
//		mTextView.setOnClickListener(new View.OnClickListener(){
//		    public void onClick(View v){
//		    	TextView tv = (TextView) v;
//                String s = tv
//                        .getText()
//                        .subSequence(tv.getSelectionStart(),
//                                tv.getSelectionEnd()).toString();
//                Log.e("tapped on:", s);
//		    	
//		    	Log.e("gray", "PlayActivity.java: TextView.onClick : " + mTextView.getSelectionStart() + mTextView.getSelectionEnd());
//		    }
//		});
//		
//		mTextView.setOnLongClickListener(new View.OnLongClickListener() {
//			
//			@Override
//			public boolean onLongClick(View v) {
////				Log.e("gray", "PlayActivity.java: TextView.onLongClick : " + mTextView.getSelectionStart() + "--"+ mTextView.getSelectionEnd());
//				Log.e("gray", "PlayActivity.java: TextView.onLongClick : " + mTextView.getText().subSequence(mTextView.getSelectionStart(), mTextView.getSelectionEnd()));
//				
//				
//				
//				return false;
//			}
//		});
		
		// dialog
//		new AlertDialog.Builder(this).setTitle("請輸入").setIcon( 
//				android.R.drawable.ic_dialog_info).setView( 
//				new EditText(this)).setPositiveButton("確定", null) 
//				.setNegativeButton("取消" , null).show();
		
		
//	     Button btn_go=(Button)findViewById(R.id.button_test);
//	     btn_go.setOnClickListener(new OnClickListener() {
//
//	      @Override
//	      public void onClick(View v) {
//	            Log.e("gray", "PlayActivity.java:onClick");
//	      }
//      });
		
		// Gesture
//		final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
//		    public boolean onDoubleTap(MotionEvent e) {
//		    	super.onDoubleTap(e);
//
//		        return true;
//		    }
//		});
//		
//		mTextView = (TextView) findViewById(R.id.tv_webContent);
//		mTextView.setOnTouchListener(new OnTouchListener() {
//		    public boolean onTouch(View v, MotionEvent event) {
//		    	Log.e("gray", "PlayActivity.java: onTouch");
//		        return gestureDetector.onTouchEvent(event);
//		    }
//		});
		

		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: "+ Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName);
		}
		// download video
		ConnectivityManager conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networInfo = conManager.getActiveNetworkInfo();
		
		if (MainActivity.isEnableDownload && 
			!isFileExist(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName) && 
			( networInfo != null || networInfo.isAvailable()) ){
			
			String url = cnnVideoPath;
			DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
			request.setDescription("to /sdcard/download");
			request.setTitle(cnnVideoName);
			// in order for this if to run, you must use the android 3.2 to compile your app
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				request.allowScanningByMediaScanner();
				request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			}
			request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, cnnVideoName);
			
			// get download service and enqueue file
			DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
			manager.enqueue(request);
			
		}
		
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: END =================");
		}
	}
	
	public static boolean isFileExist(String path) {
		File file = new File(path);
		if (!file.exists()) {
			return false;
		} else {
			return true;
		}
	}

	Handler handler = new Handler() {  
        @Override  
        public void handleMessage(Message msg) {
        	setResultText(cnnScriptContent);
        	
        	FileWriter fw;
			try {
				fw = new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName+".txt", false);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(cnnScriptContent);
				bw.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
            
		    mProgressDialog.dismiss();
        }  
    };  
    
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
	    URL url = new URL(cnnScriptPath);
	    // get HTML page root node
	    TagNode root = htmlCleaner.clean(url);
	
	    // query XPath
	    cnnScriptContent = "";
	    
	    XPATH = "//p[@class='cnnTransSubHead']";
	    Object[] statsNode = root.evaluateXPath(XPATH);
	    // process data if found any node
	    if(statsNode.length > 0) {
	    	
	    	if (MainActivity.isDebug) {
	    		Log.e("gray", "MainActivity.java:getScriptContent, statsNode.length:" + statsNode.length);
			}
	    	
	    	for (int i = 0; i < statsNode.length; i++) {
				
	    		TagNode resultNode = (TagNode)statsNode[i];
	    		// get text data from HTML node
	    		cnnScriptContent += resultNode.getText().toString() + "  \n";
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
	
//	    if (MainActivity.isDebug) {
//
//	    	String [] tsa = new String [100]; 
//	    	tsa = cnnScriptContent.split("	");
//	    	for (int i = 0; i < tsa.length; i++) {
//	    		Log.e("gray", "PlayActivity.java: " + tsa[i]);
//	    	}
//		}
	    
	}

	public String readFileAsString(String filePath) throws java.io.IOException
	{
	    BufferedReader reader = new BufferedReader(new FileReader(filePath));
	    String line, results = "";
	    while( ( line = reader.readLine() ) != null)
	    {
	        results += line;
	    }
	    reader.close();
	    return results;
	}
	
	public void setResultText(String s){
		
		mTextView = (TextView) findViewById(R.id.tv_webContent);
		
		s = s.replaceAll("  ", "\n\n");
		mTextView.setText(s);
		mTextView.setTextSize(MainActivity.textSize);
	}
	
	@Override
	protected void onDestroy() {
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: onDestroy");
		}
		super.onDestroy();
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: onCompletion");
		}
	}
	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.play, menu);
//		return true;
//	}

}
