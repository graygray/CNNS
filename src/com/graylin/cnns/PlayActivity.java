package com.graylin.cnns;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;


import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class PlayActivity extends Activity implements OnCompletionListener, OnPreparedListener{

	public static String cnnVideoPath = "";
	public String cnnVideoName = "";
	public String cnnScriptPath = "";
	public String cnnScriptContent = "";
    
	public String XPATH = "";
    
	public MyVideoView mVideoView;
//	public VideoView mVideoView;
	public EditText mEditText;
	public TextView mTV_videoTime;
	public ProgressDialog mProgressDialogScript;
	public ProgressDialog mProgressDialogVideo;
	public ProgressDialog mProgressDialogTranslate;
	public RelativeLayout.LayoutParams videoviewlp;
	
	public CharSequence srcText = "";
	public String srcString = "";
	public String translatedText = "";
	public static int reTranstaleCount;
	
	// video variables
	public static boolean isVideoPlaying;
	public static int stopPosition;
	public boolean isVideoTouchMove;
	public float currentX = 0, previousX = 0;
	public float currentY = 0, previousY = 0;
	public int videoWidth;
	public int videoHeight;
	public int displayWidth;
	public int displayHeight;
	public int videoThresholdX;
	public int videoThresholdY;
	public boolean isVideoFileExit;
	public Intent playVideoServiceIntent;
	public static boolean isStopService;
	public static boolean flagOnPause;
	public static boolean isRotate;			// manual rotate
	public static boolean isRotate2;		// auto rotate
	public boolean clickZoneRight;
	public boolean clickZoneCenter;
	public boolean clickZoneLeft;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);
		
		mVideoView = (MyVideoView) findViewById(R.id.videoView_CNNS);
//		mVideoView = (VideoView) findViewById(R.id.videoView_CNNS);
		videoThresholdX = 5;
		videoThresholdY = 50;
		
		mTV_videoTime = (TextView) findViewById(R.id.video_time);
		
        if (!isRotate && getResources().getConfiguration().orientation != MainActivity.originOrientation) {
			// auto rotate
        	isRotate2 = true;
        	MainActivity.originOrientation = getResources().getConfiguration().orientation;
		} 
		if (isRotate || isRotate2) {
			isRotate = false;
			isRotate2 = false;
		} else {
			stopPosition = 0;
		}
		videoWidth = 0;
		videoHeight = 0;
		mVideoView.setOnCompletionListener(this);
		mVideoView.setOnPreparedListener(this);
		
		// change layout to avoid blocking all screen
        RelativeLayout.LayoutParams videoviewlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 10);
        mVideoView.setLayoutParams(videoviewlp);
        mVideoView.invalidate();
        
		cnnVideoPath = MainActivity.getVideoAddress();
		cnnScriptPath = MainActivity.getScriptAddress();
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: START ===============");
			Log.e("gray", "PlayActivity.java: cnnVideoPath : " + cnnVideoPath);
			Log.e("gray", "PlayActivity.java: cnnScriptPath : " + cnnScriptPath);
		}
		
		String [] tempSA = new String [32];
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
			isVideoFileExit = true;
			cnnVideoPath = Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName;
			playVideo();
			
		} else {
			// video not exist, play from CNNS
			isVideoFileExit = false;
			if (isNetworkAvailable()){
				
				// download video
				// if Enable Download && video file not exist, download it
				if (MainActivity.isEnableDownload && isDownloadManagerAvailable(getApplicationContext()) ){
					
					if (MainActivity.isDebug) {
						Log.e("gray", "PlayActivity.java:onCreate, start to download video...");
					}
					String url = cnnVideoPath;
					DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
					request.setDescription("to /sdcard/download");
					request.setTitle(cnnVideoName);
					// in order for this if to run, you must use the android 3.2 to compile your app
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						
						if (MainActivity.isDebug) {
							Log.e("gray", "PlayActivity.java: Build.VERSION.SDK_INT:" + Build.VERSION.SDK_INT);
							Log.e("gray", "PlayActivity.java: Build.VERSION_CODES.HONEYCOMB:" + Build.VERSION_CODES.HONEYCOMB);
						}
						request.allowScanningByMediaScanner();
						request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
//						request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
					} else {
						request.setShowRunningNotification(true);
					}
					
					request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, cnnVideoName);
					
					// get download service and enqueue file
					DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
					manager.enqueue(request);
				}
				
				playVideo();
		        
			} else {
				
				if (MainActivity.isDebug) {
					Log.e("gray", "PlayActivity.java, NO Available Network!!");
				}
				
				showAlertDialog("Alert Message - video", "* No Availiable Network!!\n* Video File Not Exist!!");
				
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
			
			if (isNetworkAvailable()){
				
				showProcessDialog(0, "Please Wait...", "Loading Script...");
				
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
							Log.e("gray", "PlayActivity.java:run, Exception1:" + e.toString());  
							e.printStackTrace();
						}
					} 
				}).start();
			
			} else {
				
				if (MainActivity.isDebug) {
					Log.e("gray", "PlayActivity.java, NO Available Network!!");
				}
				showAlertDialog("Alert Message - script", "* No Availiable Network!!\n* Script File Not Exist!!");
			}
		}
		
		mEditText = (EditText) findViewById(R.id.tv_webContent);
		registerForContextMenu(mEditText);
		mEditText.setKeyListener(null);
		
		mEditText.setOnClickListener(new View.OnClickListener(){
		    public void onClick(View v){
		    	
		    	// double click, translate function
		    	if (mEditText.getSelectionStart() != mEditText.getSelectionEnd()) {
		    		
		    		srcText = mEditText.getText().subSequence(mEditText.getSelectionStart(), mEditText.getSelectionEnd());
					if (isNetworkAvailable()){
						
						if (MainActivity.isDebug) {
							Log.e("gray", "PlayActivity.java: EditText.onClick : " + mEditText.getSelectionStart() + "--"+ mEditText.getSelectionEnd());
							Log.e("gray", "PlayActivity.java: EditText.onClick : " + srcText);
						}
						
						showProcessDialog(2, "Please Wait...", "Translate...");
						
						new Thread(new Runnable() 
						{ 
							@Override
							public void run() 
							{ 
								try {
									reTranstaleCount = 0;
									getTranslateString(srcText);
									handler.sendEmptyMessage(1);
									if (MainActivity.isDebug) {
										Log.e("gray", "PlayActivity.java:run, translatedText:" + translatedText);
									}
								} catch (Exception e) {
									Log.e("gray", "PlayActivity.java:run, Exception2:" + e.toString());  
									e.printStackTrace();
								}
							} 
						}).start();
				        
					} else {
					    // save translate word to note
						FileWriter fw;
						try {
							fw = new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName+".cnnsNote.txt", true);
							BufferedWriter bw = new BufferedWriter(fw);
							bw.write(srcText+"\n\n");
							bw.close();
						} catch (IOException e) {
							Log.e("gray", "PlayActivity.java:run, save script error, Exception e:" + e.toString()); 
							e.printStackTrace();
						}
						showAlertDialog("Alert Message - translate", "No Availiable Network!!");
					}
				}
		    }
		});
		
		mEditText.setOnLongClickListener(new View.OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				if (MainActivity.isDebug) {
					Log.e("gray", "PlayActivity.java: EditText.onLongClick : " + mEditText.getSelectionStart() + "--"+ mEditText.getSelectionEnd());
					Log.e("gray", "PlayActivity.java: EditText.onLongClick : " + mEditText.getText().subSequence(mEditText.getSelectionStart(), mEditText.getSelectionEnd()));
				}
				
				return false;
			}
		});
		
		final Button translateButton = (Button) findViewById(R.id.translate_button);
		if (MainActivity.isEnableSoftButtonTranslate) {
			translateButton.setVisibility(View.VISIBLE);
		} else {
			translateButton.setVisibility(View.GONE);
		}
		translateButton.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {

				if (MainActivity.isDebug) {
					Log.e("gray", "PlayActivity.java:onClick, translateButton");
				}
				if (MainActivity.isEnableSoftButtonTranslate) {
					
		    		//translate
		    		if (mEditText.getSelectionStart() != mEditText.getSelectionEnd()) {
		    			
		    			srcText = mEditText.getText().subSequence(mEditText.getSelectionStart(), mEditText.getSelectionEnd());
		    			if (isNetworkAvailable()){
		    				
		    				if (MainActivity.isDebug) {
		    					Log.e("gray", "PlayActivity.java: EditText.onClick : " + mEditText.getSelectionStart() + "--"+ mEditText.getSelectionEnd());
		    					Log.e("gray", "PlayActivity.java: EditText.onClick : " + srcText);
		    				}
		    				
		    				showProcessDialog(2, "Please Wait...", "Translate...");
		    				
		    				new Thread(new Runnable() 
		    				{ 
		    					@Override
		    					public void run() 
		    					{ 
		    						try {
		    							reTranstaleCount = 0;
		    							getTranslateString(srcText);
		    							handler.sendEmptyMessage(1);
		    							if (MainActivity.isDebug) {
		    								Log.e("gray", "PlayActivity.java:run, translatedText:" + translatedText);
		    							}
		    						} catch (Exception e) {
		    							Log.e("gray", "PlayActivity.java:run, Exception3:" + e.toString());  
		    							e.printStackTrace();
		    						}
		    					} 
		    				}).start();
		    				
		    			} else {
		    				 // save translate word to note
							FileWriter fw;
							try {
								fw = new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName+".cnnsNote.txt", true);
								BufferedWriter bw = new BufferedWriter(fw);
								bw.write(srcText+"\n\n");
								bw.close();
							} catch (IOException e) {
								Log.e("gray", "PlayActivity.java:run, save script error, Exception e:" + e.toString()); 
								e.printStackTrace();
							}
		    				showAlertDialog("Alert Message - translate", "No Availiable Network!!");
		    			}
		    		}
				}
			}

		});

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
		
		showProcessDialog(1, "Please wait", "Loading video...");
        
		/*
		 * Alternatively,for streaming media you can use
		 * mVideoView.setVideoURI(Uri.parse(URLstring));
		 */
		mVideoView.setVideoPath(cnnVideoPath);
		// mVideoView.setVideoURI(Uri.parse("android.resource://ss.ss/"+R.raw.main));

		// control bar
		if (MainActivity.isVideoControlBar) {
			mVideoView.setMediaController(new MediaController(this));
		}
		// mVideoView.requestFocus();
		// start play
		mVideoView.start();
		
		mVideoView.setOnTouchListener(new OnTouchListener() {
			
			public boolean onTouch(View v, MotionEvent event) {
				if (MainActivity.isDebug) {
					Log.e("gray", "PlayActivity.java: onTouch");
				}
		    	int action = MotionEventCompat.getActionMasked(event);
		    	
		    	// show video timestamp
		    	if (!MainActivity.isVideoControlBar) {
		    		mTV_videoTime.setVisibility(View.VISIBLE);
		    		mTV_videoTime.setText(milliSecondsToTimer(mVideoView.getCurrentPosition()));
		    	}
            	
		        switch(action) {
		            case (MotionEvent.ACTION_DOWN) :
		            	if (MainActivity.isDebug) {
		            		Log.e("gray", "PlayActivity.java: onTouch , Action was DOWN, " + event.getRawX());
		            	}
		            	previousX = event.getRawX();
		            	previousY = event.getRawY();
		            	
		            	if ( previousX <= (videoWidth/6) ) {
		            		if (MainActivity.isDebug) {
								Log.e("gray", "PlayActivity.java:playVideo, " + "Left");
							}
		            		clickZoneRight = false;
		            		clickZoneCenter = false;
		            		clickZoneLeft = true;
						} else if ( previousX > (videoWidth/6) && previousX < (videoWidth*5/6) ) {
							if (MainActivity.isDebug) {
								Log.e("gray", "PlayActivity.java:playVideo, " + "Center");
							}
							clickZoneRight = false;
		            		clickZoneCenter = true;
		            		clickZoneLeft = false;
						} else {
							if (MainActivity.isDebug) {
								Log.e("gray", "PlayActivity.java:playVideo, " + "Right");
							}
							clickZoneRight = true;
		            		clickZoneCenter = false;
		            		clickZoneLeft = false;
						}
		            	
		                return true;
		            case (MotionEvent.ACTION_MOVE) :
		            	if (MainActivity.isDebug) {
		            		Log.e("gray", "PlayActivity.java: onTouch , Action was MOVE, " + event.getRawX());
		            	}
		            	currentX = event.getRawX();
		            	currentY = event.getRawY();
		            	
		            	if (currentX - previousX > videoThresholdX) {
							// move to right
		            		if (MainActivity.isDebug) {
		            			Log.e("gray", "PlayActivity.java: playVideo , move to right.");
		            		}
		            		stopPosition = mVideoView.getCurrentPosition();
		            		mVideoView.seekTo(stopPosition + MainActivity.swipeTime*1000);
		            		previousX = currentX;
		            		isVideoTouchMove = true;
						} else if (currentX - previousX < -videoThresholdX) {
							// move to left
							if (MainActivity.isDebug) {
								Log.e("gray", "PlayActivity.java: playVideo , move to left.");
							}
							stopPosition = mVideoView.getCurrentPosition();
							mVideoView.seekTo(stopPosition - MainActivity.swipeTime*1000);
							previousX = currentX;
							isVideoTouchMove = true;
							
						} else {
							if (MainActivity.isDebug) {
								Log.e("gray", "PlayActivity.java: playVideo, X not over the threshold ");
							}
						}
		            	
		            	if (currentY - previousY > videoThresholdY) {
		            		videoWidth = videoWidth + (int)(videoWidth * 0.1);
		            		videoHeight = videoHeight + (int)(videoHeight * 0.1);
		            		
		            		mVideoView.setDimensions(videoWidth, videoHeight);
		            		mVideoView.getHolder().setFixedSize(videoWidth, videoHeight);
		            		
				            previousY = currentY;
							isVideoTouchMove = true;
							
						} else if (currentY - previousY < -videoThresholdY) {
							videoWidth = videoWidth - (int)(videoWidth * 0.1);
		            		videoHeight = videoHeight - (int)(videoHeight * 0.1);
		            		
				        	mVideoView.setDimensions(videoWidth, videoHeight);
		            		mVideoView.getHolder().setFixedSize(videoWidth, videoHeight);
		            		
				            previousY = currentY;
							isVideoTouchMove = true;
							
						} else {
							if (MainActivity.isDebug) {
								Log.e("gray", "PlayActivity.java: playVideo, Y not over the threshold ");
							}
						}
		            	
		                return true;
		            case (MotionEvent.ACTION_UP) :
		            	if (MainActivity.isDebug) {
		            		Log.e("gray", "PlayActivity.java: onTouch , Action was UP");
		            	}
			            // reset variable
			            currentX = 0;
			            currentY = 0;
			            previousX = 0;
			            previousY = 0;
			            
			            if (!MainActivity.isVideoControlBar) {
			            	mTV_videoTime.setVisibility(View.GONE);
			            }
			            
			            if (!isVideoTouchMove) {
			            	if (clickZoneLeft) {
			            		// click at video left
			            		stopPosition = mVideoView.getCurrentPosition();
								mVideoView.seekTo(stopPosition - MainActivity.swipeTime*1000);
							} else if (clickZoneCenter) {
								// click at video center
								if (mVideoView.isPlaying()) {
									mVideoView.pause();
								} else {
									if(!flagOnPause){
										stopPosition = mVideoView.getCurrentPosition();
									}
									flagOnPause = false;
									mVideoView.seekTo(stopPosition);
									mVideoView.start();
								}
							} else {
								// click at video right
								stopPosition = mVideoView.getCurrentPosition();
			            		mVideoView.seekTo(stopPosition + MainActivity.swipeTime*1000);
							}
			            	
				            return false;
						} else {
							isVideoTouchMove = false;
							return true;
						}
			            
		            case (MotionEvent.ACTION_CANCEL) :
		            	if (MainActivity.isDebug) {
		            		Log.e("gray", "PlayActivity.java: onTouch , Action was CANCEL");
		            	}
		                return true;
		            case (MotionEvent.ACTION_OUTSIDE) :
		            	if (MainActivity.isDebug) {
		            		Log.e("gray", "PlayActivity.java: onTouch , Movement occurred outside bounds " +
		            				"of current screen element");
		            	}
		                return true;      
		            default : 
		            	if (MainActivity.isDebug) {
		            		Log.e("gray", "PlayActivity.java: onTouch , default");
		            	}
		                return true;
		        }      
		    }
		});
	}
	
	public void getTranslateString(CharSequence srcCS) throws Exception {

		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java:getTranslateString, " + "");
		}
		
		boolean isSentance = false;
		boolean noResultFlag = false;
		
		// ignore source text's length <= 1
		if (srcCS.length() <= 1) {
			return;
		}
		
		srcString = srcCS.toString();
//		srcString = srcString.toLowerCase(Locale.US);
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java:getTranslateString, srcString.length()" + srcString.length());
			Log.e("gray", "PlayActivity.java:getTranslateString, srcString.indexOf(\" \")" + srcString.indexOf(" "));
		}
		if (srcString.contains(" ")) {
			if (srcString.length() - 1 == srcString.indexOf(" ")) {
				// blank at end of string
			} else {
				isSentance = true;
			}
			srcString = srcString.replaceAll(" ", "%20");
		}
		
		translatedText = "";
		
		String queryURL = "http://translate.reference.com/translate?query=";
		queryURL = queryURL + srcString + "&src=en&dst=" + MainActivity.translateLanguage;
		
		if (MainActivity.translateLanguage.equalsIgnoreCase("tl") ||
			MainActivity.translateLanguage.equalsIgnoreCase("ms") ||
			MainActivity.translateLanguage.equalsIgnoreCase("id") ||
			MainActivity.translateLanguage.equalsIgnoreCase("th") ||
			MainActivity.translateLanguage.equalsIgnoreCase("vi") ||
			MainActivity.translateLanguage.equalsIgnoreCase("bg") ||	
			MainActivity.translateLanguage.equalsIgnoreCase("fi") ||	
			MainActivity.translateLanguage.equalsIgnoreCase("hi") ||	
			MainActivity.translateLanguage.equalsIgnoreCase("hu") ||
			MainActivity.translateLanguage.equalsIgnoreCase("fa") ||	
			MainActivity.translateLanguage.equalsIgnoreCase("pt") ||	
			MainActivity.translateLanguage.equalsIgnoreCase("uk") ||
			isSentance
																		) {
			XPATH = "//div[@class='translateTxt']";
		} else if (MainActivity.translateLanguage.equalsIgnoreCase("es")) {
			XPATH = "//div[@id='tabr1']";
		} else if (MainActivity.translateLanguage.equalsIgnoreCase("eng")) {
			queryURL = "http://www.merriam-webster.com/dictionary/" + srcString;
			XPATH = "//div[@class='ld_on_collegiate']//p";
			translatedText = "\n" + translatedText;
		} else if (MainActivity.translateLanguage.equalsIgnoreCase("zh-TW")) {
			queryURL = "http://dict.dreye.com/ews/dict.php?w=" + srcString;
			XPATH = "//div[@class='dict_cont']//div";
			translatedText = "\n" + translatedText;
//		} else if (MainActivity.translateLanguage.equalsIgnoreCase("zh-CN")) {
//			queryURL = "http://dict.youdao.com/search?q=" + srcString;
//			XPATH = "//p[@class='collapse-content']";
		} else {
			XPATH = "//div[@class='definition']";
		}
		
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
	        
	    	if (MainActivity.translateLanguage.equalsIgnoreCase("vi")) {
	    		translatedText = htmlUnicodeToJavaUnicode(translatedText);
			}
	    	
	    } else {
	    	// not found, try -s at end of word
			Log.e("gray", "PlayActivity.java: " + "statsNode.length < 0");
			noResultFlag = true;
		}
	    
	    if (noResultFlag && (reTranstaleCount<2) ) {
			// search again with srcString - end s
	    	reTranstaleCount++;
	    	srcString = srcString.substring(0, srcString.length()-1);
	    	Log.e("gray", "PlayActivity.java:getTranslateString, srcString:" + srcString);
	    	getTranslateString(srcString);
		} else {
			// save translate word to note
			FileWriter fw;
			try {
				fw = new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName+".cnnsNote.txt", true);
				BufferedWriter bw = new BufferedWriter(fw);
				translatedText = translatedText.replaceAll("\n\n", "\n");
				bw.write(srcText + translatedText + "\n");
				bw.close();
			} catch (IOException e) {
				Log.e("gray", "PlayActivity.java:run, save script error, Exception e:" + e.toString()); 
				e.printStackTrace();
			}
		}
	}
	
	@SuppressLint("HandlerLeak")
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
				
				try {
					mProgressDialogScript.dismiss();
					mProgressDialogScript = null;
				} catch (Exception e) {
					// nothing
				}
				
				break;

			case 1:
				
				ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
				String name = manager.getRunningTasks(1).get(0).topActivity.getClassName();
				if (MainActivity.isDebug) {
					Log.e("gray", "PlayActivity.java: translatedText:" + translatedText);
					Log.e("gray", "PlayActivity.java:showAlertDialog, topActivity:" + name);
				}
		        if(name.equals("com.graylin.cnns.PlayActivity")){
		        	new AlertDialog.Builder(PlayActivity.this).setTitle(srcText).setIcon( 
		        			android.R.drawable.ic_dialog_info).setMessage(translatedText)
		        			.show();
		        }
				
				try {
					mProgressDialogTranslate.dismiss();
					mProgressDialogTranslate = null;
				} catch (Exception e) {
					// nothing
				}
				
				break;
			}
            
		    
        }  
    };  
    
    public void showProcessDialog(int what, CharSequence title, CharSequence message){
    	
    	if (what == 0) {
    		mProgressDialogScript = ProgressDialog.show(PlayActivity.this, title, message, true);
    		mProgressDialogScript.setCancelable(true); 
		} else if (what == 1) {
			mProgressDialogVideo = ProgressDialog.show(PlayActivity.this, title, message, true);
			mProgressDialogVideo.setCancelable(true); 
		} else if (what == 2) {
			mProgressDialogTranslate = ProgressDialog.show(PlayActivity.this, title, message, true);
			mProgressDialogTranslate.setCancelable(true); 
		} else {
			Log.e("gray", "PlayActivity.java:showProcessDialog, not a case!");
		}
    	
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
	    
	    Log.e("gray", "PlayActivity.java:getScriptContent, cnnScriptPath:" + cnnScriptPath.toString());
	    
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
		
		mEditText = (EditText) findViewById(R.id.tv_webContent);
		
		s = s.replaceAll("  ", "\n\n");
		s = s.replaceAll("\n\n\n\n\n", "\n");
		s = s.replaceAll("\n\n\n\n", "\n");
//		s = s.replaceAll("\n\n\n", "\n");
		mEditText.setText(s);
		mEditText.setTextSize(MainActivity.textSize);
		
		switch (MainActivity.scriptTheme) {
		case 0:	// Black  -  White
			mEditText.setTextColor(0xff000000);
			mEditText.setBackgroundColor(0xffffffff);
			break;
		case 1:	// White  -  Black
			mEditText.setTextColor(0xffffffff);
			mEditText.setBackgroundColor(0xff000000);
			break;
		case 2: // Red - White
			mEditText.setTextColor(0xffDC143C);
			mEditText.setBackgroundColor(0xffffffff);
			break;
		case 3: // White  -  Red
			mEditText.setTextColor(0xffffffff);
			mEditText.setBackgroundColor(0xffA8050A);
			break;
		case 4: // Orange  -  Black
			mEditText.setTextColor(0xFFFFA500);
			mEditText.setBackgroundColor(0xff000000);
			break;
		case 5: // White  -  Orange
			mEditText.setTextColor(0xffffffff);
			mEditText.setBackgroundColor(0xFFFFA500);
			break;
		case 6: // Black  -  Orange
			mEditText.setTextColor(0xff000000);
			mEditText.setBackgroundColor(0xFFFFA500);
			break;
		case 7: // Black  -  Yellow
			mEditText.setTextColor(0xff000000);
			mEditText.setBackgroundColor(0xffFFF396);
			break;
		case 8: // Green - White
			mEditText.setTextColor(0xff00C22E);
			mEditText.setBackgroundColor(0xffffffff);
			break;
		case 9: // Green - Black
			mEditText.setTextColor(0xff00C22E);
			mEditText.setBackgroundColor(0xff000000);
			break;
		case 10: // White - Green
			mEditText.setTextColor(0xffffffff);
			mEditText.setBackgroundColor(0xff00C22E);
			break;
		case 11: // Black - Green
			mEditText.setTextColor(0xff000000);
			mEditText.setBackgroundColor(0xff00C22E);
			break;
		case 12: // LightBlue  -  White
			mEditText.setTextColor(0xFF4169E1);
			mEditText.setBackgroundColor(0xffffffff);
			break;
		case 13: // White - LightBlue
			mEditText.setTextColor(0xffffffff);
			mEditText.setBackgroundColor(0xFF4169E1);
			break;
		case 14: // Black - LightBlue
			mEditText.setTextColor(0xff000000);
			mEditText.setBackgroundColor(0xFF4169E1);
			break;
		case 15: // White  -  Blue
			mEditText.setTextColor(0xffffffff);
			mEditText.setBackgroundColor(0xff1038AA);
			break;
		case 16: // Pink  -  White
			mEditText.setTextColor(0xFFFF1493);
			mEditText.setBackgroundColor(0xffffffff);
			break;
		case 17: // LightPurple - White
			mEditText.setTextColor(0xffC71585);
			mEditText.setBackgroundColor(0xffffffff);
			break;
		case 18: // White - LightPurple
			mEditText.setTextColor(0xffffffff);
			mEditText.setBackgroundColor(0xffC71585);
			break;
		case 19: // White  -  Purple
			mEditText.setTextColor(0xffffffff);
			mEditText.setBackgroundColor(0xff4D0D2A);
			break;
		default:
			break;
		}
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: onCompletion");
		}
	}
	
	@Override
    public void onPrepared(MediaPlayer mp) {
		
		videoWidth = mp.getVideoWidth();
		videoHeight = mp.getVideoHeight();
		float aspectRatio = (float)videoWidth / (float)videoHeight;
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: onPrepared");
			Log.e("gray", "PlayActivity.java: media aspectRatio : " + aspectRatio);
			Log.e("gray", "PlayActivity.java: videoWidth : " + videoWidth);
			Log.e("gray", "PlayActivity.java: videoHeight : " + videoHeight);
		}
		
		videoWidth = displayWidth;
		videoHeight =  (int)( ((float)displayWidth / (float)mp.getVideoWidth() ) * videoHeight );
		aspectRatio = (float)videoWidth / (float)videoHeight;
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: final aspectRatio : " + aspectRatio);
			Log.e("gray", "PlayActivity.java: videoWidth : " + videoWidth);
			Log.e("gray", "PlayActivity.java: videoHeight : " + videoHeight);
		}
		
		// change layout to WRAP_CONTENT
        RelativeLayout.LayoutParams videoviewlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        mVideoView.setLayoutParams(videoviewlp);
        mVideoView.invalidate();
        
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mVideoView.setDimensions(videoWidth, videoHeight);
		mVideoView.getHolder().setFixedSize(videoWidth, videoHeight);
		
		mVideoView.seekTo(stopPosition);
		
        try {
        	mProgressDialogVideo.dismiss();
			mProgressDialogVideo = null;
		} catch (Exception e) {
			// nothing
		}
    }
	
	public boolean isNetworkAvailable() {
		ConnectivityManager conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networInfo = conManager.getActiveNetworkInfo();
		if (networInfo == null || !networInfo.isAvailable()){
			return false;
		} else {
			return true;
		}
	}
	
	public void showAlertDialog(String title, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(PlayActivity.this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
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
	
	protected void onPause() {
        super.onPause();
        stopPosition = mVideoView.getCurrentPosition();

        if (MainActivity.isDebug) {
        	Log.e("gray", "PlayActivity.java: onPause ");
        	Log.e("gray", "PlayActivity.java:onPause, stopPosition:" + stopPosition);
		}
        
        flagOnPause = true;
        
        isVideoPlaying = mVideoView.isPlaying();
        
        if (isVideoPlaying) {
        	// if video is playing & app goes to background, then start background service
        	playVideoServiceIntent = new Intent(this, PlayVideoService.class);
        	startService(playVideoServiceIntent);
        	isStopService = false;
		}
        mVideoView.pause();
	}
	
	@SuppressWarnings("deprecation")
	protected void onResume() {
		super.onResume();
		
		// stop background service
		if (playVideoServiceIntent != null) {
			isStopService = true;
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			stopService(playVideoServiceIntent);
			playVideoServiceIntent = null;
		}
		
		WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
		android.view.Display display = wm.getDefaultDisplay();
		Point size = new Point();
		
		if (videoWidth == 0 && videoHeight == 0) {
			
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2){   //API LEVEL 13
				display.getSize(size);
				displayWidth = size.x;
				displayHeight = size.y;
			}else{    
				// for older device
				displayWidth = display.getWidth();
				displayHeight = display.getHeight();
			}
		} else {
			displayWidth = videoWidth;
			displayHeight = videoHeight;
		}
		
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: onResume ");
			Log.e("gray", "PlayActivity.java: default width : " + displayWidth);
			Log.e("gray", "PlayActivity.java: default height : " + displayHeight);
			Log.e("gray", "PlayActivity.java:onResume, stopPosition:" + stopPosition);
		}
		
		if (isVideoFileExit) {
			mVideoView.setDimensions(displayWidth, displayHeight);
		} else {
			mVideoView.setDimensions(1, 1);
		}
		mVideoView.getHolder().setFixedSize(videoWidth, videoHeight);
		
		mVideoView.seekTo(stopPosition);

		if (isVideoPlaying) {
			mVideoView.start();
		} else {
			mVideoView.resume();
		}
	}
	
	@Override
	protected void onDestroy() {
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: onDestroy");
		}
		
		// set stop service flag anyway
		isStopService = true;
		
		super.onDestroy();
	}
	
//	@Override
//	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		
//		if (MainActivity.isDebug) {
//			Log.e("gray", "PlayActivity.java:onKeyDown, " + "");
//		}
//	    if (keyCode == KeyEvent.KEYCODE_MENU) {
//	    	if (MainActivity.isDebug) {
//	    		Log.e("gray", "PlayActivity.java:onKeyDown, " + "KeyEvent.KEYCODE_MENU");
//			}
//	    	
//	    	if (mVideoView.isPlaying()) {
//				mVideoView.pause();
//			} else {
//				mVideoView.start();
//			}
//	    	
//	    	if (MainActivity.isEnableLongPressTranslate) {
//				
//	    		//translate
//	    		if (mEditText.getSelectionStart() != mEditText.getSelectionEnd()) {
//	    			
//	    			srcText = mEditText.getText().subSequence(mEditText.getSelectionStart(), mEditText.getSelectionEnd());
//	    			if (isNetworkAvailable()){
//	    				
//	    				if (MainActivity.isDebug) {
//	    					Log.e("gray", "PlayActivity.java: EditText.onClick : " + mEditText.getSelectionStart() + "--"+ mEditText.getSelectionEnd());
//	    					Log.e("gray", "PlayActivity.java: EditText.onClick : " + srcText);
//	    				}
//	    				
//	    				showProcessDialog(2, "Please Wait...", "Translate...");
//	    				
//	    				new Thread(new Runnable() 
//	    				{ 
//	    					@Override
//	    					public void run() 
//	    					{ 
//	    						try {
//	    							getTranslateString(srcText);
//	    							handler.sendEmptyMessage(1);
//	    							if (MainActivity.isDebug) {
//	    								Log.e("gray", "PlayActivity.java:run, translatedText:" + translatedText);
//	    							}
//	    						} catch (Exception e) {
//	    							Log.e("gray", "PlayActivity.java:run, Exception4:" + e.toString());  
//	    							e.printStackTrace();
//	    						}
//	    					} 
//	    				}).start();
//	    				
//	    			} else {
//	    				 // save translate word to note
//						FileWriter fw;
//						try {
//							fw = new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName+".cnnsNote.txt", true);
//							BufferedWriter bw = new BufferedWriter(fw);
//							bw.write(srcText+"\n\n");
//							bw.close();
//						} catch (IOException e) {
//							Log.e("gray", "PlayActivity.java:run, save script error, Exception e:" + e.toString()); 
//							e.printStackTrace();
//						}
//	    				showAlertDialog("Alert Message - translate", "No Availiable Network!!");
//	    			}
//	    		}
//			}
//	        return true;
//	    }
//	    return super.onKeyDown(keyCode, event);
//	}
	
	public String htmlUnicodeToJavaUnicode(String inputs) {
		StringBuffer result = new StringBuffer("");
		int position = inputs.indexOf("&#");
		int position2 = inputs.indexOf(";", position + 2);
		if (position >= 0 && position2 >= 0) {
			String befores = inputs.substring(0, position);
			String afters = inputs.substring(position2 + 1, inputs.length());
			String middles = inputs.substring(position + 2, position2);

			String hexString = Integer.toHexString(Integer.parseInt(middles));
			if (hexString.length() % 2 != 0) {
				hexString = "0".concat(hexString);
			}

			int hl = hexString.length() / 2;
			byte[] p = { -2, -1, 0, 0 };
			hl = 3;
			for (int i = 0; i < hexString.length(); i += 2) {
				p[hl - 1] = (byte) Integer.parseInt(
						hexString.substring(i, i + 2), 16);
				hl++;
			}
			try {
				result = result.append(befores).append(new String(p, "UTF-16"))
						.append(htmlUnicodeToJavaUnicode(afters));
			} catch (Exception e) {
				//
			}
		} else {
			result = result.append(inputs);
		}
		return result.toString();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java:onCreateContextMenu");
		}
	    super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java:onOptionsItemSelected, " + "");
		}

		switch (item.getItemId()) {

		case R.id.action_rotate_screen:
			if (MainActivity.isDebug) {
				Log.e("gray", "PlayActivity.java:onOptionsItemSelected, case R.id.action_rotate_screen");
			}
			
			isRotate = true;
			// Get current screen orientation
			int orientation = getResources().getConfiguration().orientation;
			switch (orientation) {
			case Configuration.ORIENTATION_PORTRAIT:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				break;
			case Configuration.ORIENTATION_LANDSCAPE:
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				break;
			}

			break;
			
        case R.id.action_note:
        	if (MainActivity.isDebug) {
        		Log.e("gray", "MainActivity.java:onOptionsItemSelected, case R.id.action_note");
        	}
        	
        	NoteListActivity.NoteFileName = cnnVideoName + ".cnnsNote.txt";
        	
            Intent intent = new Intent();
			intent.setClass(PlayActivity.this, NoteActivity.class);
			startActivityForResult(intent, 2);
            break;
            
        case R.id.action_rough_position:

        	double roughPosition = 0.0;
        	int totalHight;
        	int totalVideoLength, currentVideoPosition;
        	
        	totalHight = mEditText.getHeight();
        	totalVideoLength = mVideoView.getDuration();
        	currentVideoPosition = mVideoView.getCurrentPosition();
        	roughPosition = (double)currentVideoPosition * (double)totalHight / (double)totalVideoLength;
        	
        	ScrollView sv = (ScrollView) findViewById(R.id.scrollView1);
        	if (MainActivity.isDebug) {
        		Log.e("gray", "MainActivity.java:onOptionsItemSelected, case R.id.action_rough_position");
        		Log.e("gray", "PlayActivity.java:onOptionsItemSelected, totalHight:" + totalHight);
        		Log.e("gray", "PlayActivity.java:onOptionsItemSelected, totalVideoLength:" + totalVideoLength);
        		Log.e("gray", "PlayActivity.java:onOptionsItemSelected, currentVideoPosition:" + currentVideoPosition);
        		Log.e("gray", "PlayActivity.java:onOptionsItemSelected, roughPosition:" + roughPosition);
        	}
        	
        	sv.scrollTo(0, (int)roughPosition);
            break;
            
        }
		return true;
	}

	public String milliSecondsToTimer(long milliseconds) {
		String finalTimerString = "";
		String secondsString = "";

		// Convert total duration into time
		int hours = (int) (milliseconds / (1000 * 60 * 60));
		int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
		int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
		// Add hours if there
		if (hours > 0) {
			finalTimerString = hours + ":";
		}

		// Prepending 0 to seconds if it is one digit
		if (seconds < 10) {
			secondsString = "0" + seconds;
		} else {
			secondsString = "" + seconds;
		}

		finalTimerString = finalTimerString + minutes + ":" + secondsString;

		// return timer string
		return finalTimerString;
	}
    
	// do't show settings at this page
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.play, menu);
		return true;
	}

}