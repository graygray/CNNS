package com.graylin.cnns;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
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
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class PlayActivity extends Activity implements OnCompletionListener {

	public String cnnVideoPath = "";
	public String cnnVideoName = "";
	public String cnnScriptPath = "";
	public String cnnScriptContent = "";
    
	public String XPATH = "";					// XPath query
    
	public VideoView mVideoView;
	public TextView mTextView;
	public ProgressDialog mProgressDialog;
	public RelativeLayout.LayoutParams videoviewlp;
	
	public CharSequence srcText = "";
	public String translatedText = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);
		
		// network status
		ConnectivityManager conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networInfo = conManager.getActiveNetworkInfo();
		
		cnnVideoPath = MainActivity.getVideoAddress();
		cnnScriptPath = MainActivity.getScriptAddress();
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: START ===============");
			Log.e("gray", "PlayActivity.java: cnnVideoPath : " + cnnVideoPath);
			Log.e("gray", "PlayActivity.java: cnnScriptPath : " + cnnScriptPath);
		}
		
		String [] tempSA = new String [20];
		tempSA = cnnVideoPath.split("/");
		
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: cnnVideoPath.split(\"/\"), arrayLength:" + tempSA.length);
			for (int i = 0; i < tempSA.length; i++) {
				Log.e("gray", "PlayActivity.java: cnnVideoPath.split(\"/\"), " + i + ":" + tempSA[i]);
			}
		}
		cnnVideoName = tempSA[tempSA.length - 1];
		
		// check if video file already download, or set path to local dir
		if (isFileExist(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName)) {
			
			// video already download
			cnnVideoPath = Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName;
			playVideo();
			
		} else {
			// video not exist, play from CNNS
			
			if (networInfo == null || !networInfo.isAvailable()){
				
				if (MainActivity.isDebug) {
					Log.e("gray", "PlayActivity.java, NO Available Network!!");
				}
				AlertDialog.Builder dialog = new AlertDialog.Builder(PlayActivity.this);
		        dialog.setTitle("Alert Message : video");
		        dialog.setMessage("No Availiable Network!!");
		        dialog.show();
		        
		        // change layout to avoid blocking all screen
		        mVideoView = (VideoView) findViewById(R.id.videoView_CNNS);
		        RelativeLayout.LayoutParams videoviewlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 10);
		        mVideoView.setLayoutParams(videoviewlp);
		        mVideoView.invalidate();
		        
			} else {
				
				// download video
				// if Enable Download && video file not exist, download it
				if (MainActivity.isEnableDownload && isDownloadManagerAvailable(getApplicationContext()) ){
					
					Log.e("gray", "PlayActivity.java:onCreate, start to download video...");
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
				
				playVideo();

			}
		}
		
		// check if script file already download, if Y, open saved file, or download from network
		if (isFileExist(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName+".txt")) {
			
			try {
				cnnScriptContent =  readFileAsString( Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName+".txt");
				setResultText(cnnScriptContent);
			} catch (IOException e) {
				Log.e("gray", "PlayActivity.java:run, read script file error, Exception e:" + e.toString()); 
				e.printStackTrace();
			}
			
		} else {
			// script not exist, download from CNNS
			
			if (networInfo == null || !networInfo.isAvailable()){
				
				if (MainActivity.isDebug) {
					Log.e("gray", "PlayActivity.java, NO Available Network!!");
				}
				AlertDialog.Builder dialog = new AlertDialog.Builder(PlayActivity.this);
		        dialog.setTitle("Alert Message - script");
		        dialog.setMessage("No Availiable Network!!");
		        dialog.show();
			
			} else {
				
				showProcessDialog("Please Wait...", "Loading Video & Script...");
				
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
							Log.e("gray", "PlayActivity.java:run, Exception:" + e.toString());  
							e.printStackTrace();
						}
					} 
				}).start();
			}
		}
		
		mTextView = (TextView) findViewById(R.id.tv_webContent);
		
		mTextView.setOnClickListener(new View.OnClickListener(){
		    public void onClick(View v){
		    	
		    	// double click
		    	if (mTextView.getSelectionStart() != mTextView.getSelectionEnd()) {
		    		
		    		ConnectivityManager conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		    		NetworkInfo networInfo = conManager.getActiveNetworkInfo();
					if (networInfo == null || !networInfo.isAvailable()){
						
						srcText = mTextView.getText().subSequence(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
						if (MainActivity.isDebug) {
							Log.e("gray", "PlayActivity.java: TextView.onClick : " + mTextView.getSelectionStart() + "--"+ mTextView.getSelectionEnd());
							Log.e("gray", "PlayActivity.java: TextView.onClick : " + srcText);
						}
						
						showProcessDialog("Please Wait...", "Translate...");
						
						new Thread(new Runnable() 
						{ 
							@Override
							public void run() 
							{ 
								try {
									getTranslateString(srcText);
									handler.sendEmptyMessage(1);
									if (MainActivity.isDebug) {
										Log.e("gray", "PlayActivity.java:run, translatedText:" + translatedText);
									}
								} catch (Exception e) {
									Log.e("gray", "PlayActivity.java:run, Exception:" + e.toString());  
									e.printStackTrace();
								}
							} 
						}).start();
					}
				}
		    }
		});
		
		mTextView.setOnLongClickListener(new View.OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				Log.e("gray", "PlayActivity.java: TextView.onLongClick : " + mTextView.getSelectionStart() + "--"+ mTextView.getSelectionEnd());
				Log.e("gray", "PlayActivity.java: TextView.onLongClick : " + mTextView.getText().subSequence(mTextView.getSelectionStart(), mTextView.getSelectionEnd()));
				
				return false;
			}
		});
		
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
			Log.e("gray", "PlayActivity.java: END =================");
		}
	}
	
	public boolean isFileExist(String path) {
		File file = new File(path);
		if (!file.exists()) {
			return false;
		} else {
			return true;
		}
	}

	public void playVideo(){
		
		mVideoView = (VideoView) findViewById(R.id.videoView_CNNS);
		mVideoView.setOnCompletionListener(this);
		DisplayMetrics dm = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(dm);
		// MyVideoView.WIDTH=dm.widthPixels;
		// MyVideoView.HEIGHT=dm.heightPixels;
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
	
	public void getTranslateString(CharSequence srcString) throws Exception {

		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java:getTranslateString, " + "");
		}
		translatedText = "";
		
		// simplified chinese
//		String queryURL = "http://www.iciba.com/";
//		queryURL += srcString;
//		XPATH = "//span[@class='label_list']/label";
		
		String queryURL = "http://translate.reference.com/translate?query=";
		queryURL = queryURL + srcString + "&src=en&dst=zh-TW";
		XPATH = "//div[@class='definition']";
		
		if (MainActivity.isDebug) {
			Log.e("gray", "MainActivity.java:getTranslateString, queryURL:" + queryURL);
		}
	    
	    // config cleaner properties
	    HtmlCleaner htmlCleaner = new HtmlCleaner();
	    CleanerProperties props = htmlCleaner.getProperties();
	    props.setAllowHtmlInsideAttributes(false);
	    props.setAllowMultiWordAttributes(true);
	    props.setRecognizeUnicodeChars(true);
	    props.setOmitComments(true);
	    
	    // create URL object
	    URL url;
	    TagNode root;
	    url = new URL(queryURL);
	    // get HTML page root node
	    root = htmlCleaner.clean(url);
	
	    Object[] statsNode = root.evaluateXPath(XPATH);
	    // process data if found any node
	    if(statsNode.length > 0) {
	    	
	    	if (MainActivity.isDebug) {
	    		Log.e("gray", "MainActivity.java:getTranslateString, statsNode.length:" + statsNode.length);
			}
	    	
	    	for (int i = 0; i < statsNode.length; i++) {
				
	    		TagNode resultNode = (TagNode)statsNode[i];
	    		// get text data from HTML node
	    		translatedText += resultNode.getText().toString() + "\n";
			}
	        
	    } else {
			Log.e("gray", "PlayActivity.java: " + "statsNode.length < 0");
		}
	}
	
	Handler handler = new Handler() {  
        @Override  
        public void handleMessage(Message msg) {
        	
        	switch (msg.what) {
			case 0:
				
				setResultText(cnnScriptContent);
				// save script content to local
				FileWriter fw;
				try {
					fw = new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName+".txt", false);
					BufferedWriter bw = new BufferedWriter(fw);
					bw.write(cnnScriptContent);
					bw.close();
				} catch (IOException e) {
					Log.e("gray", "PlayActivity.java:run, save script error, Exception e:" + e.toString()); 
					e.printStackTrace();
				}
				break;

			case 1:
				
				Log.e("gray", "PlayActivity.java: translatedText:" + translatedText);
				new AlertDialog.Builder(PlayActivity.this).setTitle(srcText).setIcon( 
						android.R.drawable.ic_dialog_info).setMessage(translatedText)
						.show();
				break;
			}
            
		    mProgressDialog.dismiss();
        }  
    };  
    
    public void showProcessDialog(CharSequence title, CharSequence message){
    	
		mProgressDialog = ProgressDialog.show(PlayActivity.this, title, message, true);
		mProgressDialog.setCancelable(true); 
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
	
	/**
	 * @param context used to check the device version and DownloadManager information
	 * @return true if the download manager is available
	 */
	public static boolean isDownloadManagerAvailable(Context context) {
	    try {
	        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
	            return false;
	        }
	        Intent intent = new Intent(Intent.ACTION_MAIN);
	        intent.addCategory(Intent.CATEGORY_LAUNCHER);
	        intent.setClassName("com.android.providers.downloads.ui", "com.android.providers.downloads.ui.DownloadList");
	        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
	                PackageManager.MATCH_DEFAULT_ONLY);
	        return list.size() > 0;
	    } catch (Exception e) {
	        return false;
	    }
	}
	
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		Log.e("gray", "PlayActivity.java:onKeyDown, " + "");
//	    if (keyCode == KeyEvent.KEYCODE_BACK) {
//	    	Log.e("gray", "PlayActivity.java:onKeyDown, " + "KeyEvent.KEYCODE_BACK");
//	        return true;
//	    }
//	    return super.onKeyDown(keyCode, event);
//	}
	
	// do't show settings at this page
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.play, menu);
//		return true;
//	}
	
}