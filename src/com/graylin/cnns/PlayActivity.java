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
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

public class PlayActivity extends Activity implements OnCompletionListener, OnPreparedListener{

	public String cnnVideoPath = "";
	public String cnnVideoName = "";
	public String cnnScriptPath = "";
	public String cnnScriptContent = "";
    
	public String XPATH = "";
    
	public VideoView mVideoView;
	public TextView mTextView;
	public ProgressDialog mProgressDialogScript;
	public ProgressDialog mProgressDialogVideo;
	public ProgressDialog mProgressDialogTranslate;
	public RelativeLayout.LayoutParams videoviewlp;
	
	public CharSequence srcText = "";
	public String srcString = "";
	public String translatedText = "";
	
	// video variables
	public boolean isVideoPlaying;
	public int stopPosition;
	public boolean isVideoTouchMove;
	public float cutrrentX = 0, previousX = 0;
	public float cutrrentY = 0, previousY = 0;
	public int videoWidth;
	public int videoHeight;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);
		
		mVideoView = (VideoView) findViewById(R.id.videoView_CNNS);
		mVideoView.setOnCompletionListener(this);
		mVideoView.setOnPreparedListener(this);
//		mVideoView.setOnTouchListener(new OnTouchListener() {
//		    public boolean onTouch(View v, MotionEvent event) {
//		    	int action = MotionEventCompat.getActionMasked(event);
//		        
//		        switch(action) {
//		            case (MotionEvent.ACTION_DOWN) :
//		            	Log.e("gray", "PlayActivity.java: onTouch , Action was DOWN");
//		                return true;
//		            case (MotionEvent.ACTION_MOVE) :
//		            	Log.e("gray", "PlayActivity.java: onTouch , Action was MOVE");
//		                return true;
//		            case (MotionEvent.ACTION_UP) :
//		            	Log.e("gray", "PlayActivity.java: onTouch , Action was UP");
//		                return true;
//		            case (MotionEvent.ACTION_CANCEL) :
//		            	Log.e("gray", "PlayActivity.java: onTouch , Action was CANCEL");
//		                return true;
//		            case (MotionEvent.ACTION_OUTSIDE) :
//		            	Log.e("gray", "PlayActivity.java: onTouch , Movement occurred outside bounds " +
//		                        "of current screen element");
//		                return true;      
//		            default : 
//		            	Log.e("gray", "PlayActivity.java: onTouch , default");
//		                return true;
//		        }      
//		    }
//		});
		
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
			cnnVideoPath = Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName;
			playVideo();
			
		} else {
			// video not exist, play from CNNS
			
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
				
				showAlertDialog("Alert Message : video", "No Availiable Network!!");
				
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
							Log.e("gray", "PlayActivity.java:run, Exception:" + e.toString());  
							e.printStackTrace();
						}
					} 
				}).start();
			
			} else {
				
				if (MainActivity.isDebug) {
					Log.e("gray", "PlayActivity.java, NO Available Network!!");
				}
				showAlertDialog("Alert Message - script", "No Availiable Network!!");
			}
		}
		
		mTextView = (TextView) findViewById(R.id.tv_webContent);
		registerForContextMenu(mTextView);
		
		mTextView.setOnClickListener(new View.OnClickListener(){
		    public void onClick(View v){
		    	
		    	// double click, translate function
		    	if (mTextView.getSelectionStart() != mTextView.getSelectionEnd()) {
		    		
		    		srcText = mTextView.getText().subSequence(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
					if (isNetworkAvailable()){
						
						if (MainActivity.isDebug) {
							Log.e("gray", "PlayActivity.java: TextView.onClick : " + mTextView.getSelectionStart() + "--"+ mTextView.getSelectionEnd());
							Log.e("gray", "PlayActivity.java: TextView.onClick : " + srcText);
						}
						
						showProcessDialog(2, "Please Wait...", "Translate...");
						
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
		
		mTextView.setOnLongClickListener(new View.OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				if (MainActivity.isDebug) {
					Log.e("gray", "PlayActivity.java: TextView.onLongClick : " + mTextView.getSelectionStart() + "--"+ mTextView.getSelectionEnd());
					Log.e("gray", "PlayActivity.java: TextView.onLongClick : " + mTextView.getText().subSequence(mTextView.getSelectionStart(), mTextView.getSelectionEnd()));
				}
				
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
		    		if (mTextView.getSelectionStart() != mTextView.getSelectionEnd()) {
		    			
		    			srcText = mTextView.getText().subSequence(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
		    			if (isNetworkAvailable()){
		    				
		    				if (MainActivity.isDebug) {
		    					Log.e("gray", "PlayActivity.java: TextView.onClick : " + mTextView.getSelectionStart() + "--"+ mTextView.getSelectionEnd());
		    					Log.e("gray", "PlayActivity.java: TextView.onClick : " + srcText);
		    				}
		    				
		    				showProcessDialog(2, "Please Wait...", "Translate...");
		    				
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
		mVideoView.setMediaController(new MediaController(this));
		// mVideoView.requestFocus();
		// start play
		mVideoView.start();
		
		mVideoView.setOnTouchListener(new OnTouchListener() {
			
			public boolean onTouch(View v, MotionEvent event) {
				if (MainActivity.isDebug) {
					Log.e("gray", "PlayActivity.java: onTouch");
				}
		    	int action = MotionEventCompat.getActionMasked(event);
		    	
		        switch(action) {
		            case (MotionEvent.ACTION_DOWN) :
		            	if (MainActivity.isDebug) {
		            		Log.e("gray", "PlayActivity.java: onTouch , Action was DOWN, " + event.getRawX());
		            	}
		            	previousX = event.getRawX();
		                return true;
		            case (MotionEvent.ACTION_MOVE) :
		            	if (MainActivity.isDebug) {
		            		Log.e("gray", "PlayActivity.java: onTouch , Action was MOVE, " + event.getRawX());
		            	}
		            	cutrrentX = event.getRawX();
		            	cutrrentY = event.getRawY();
		            	if (cutrrentX - previousX > 5) {
							// move to right
		            		if (MainActivity.isDebug) {
		            			Log.e("gray", "PlayActivity.java: playVideo , move to right.");
		            		}
		            		stopPosition = mVideoView.getCurrentPosition(); //stopPosition is an int
		            		mVideoView.seekTo(stopPosition + 2000);
		            		previousX = cutrrentX;
		            		isVideoTouchMove = true;
						} else if (cutrrentX - previousX < -5) {
							// move to left
							if (MainActivity.isDebug) {
								Log.e("gray", "PlayActivity.java: playVideo , move to left.");
							}
							stopPosition = mVideoView.getCurrentPosition(); //stopPosition is an int
							mVideoView.seekTo(stopPosition - 2000);
							previousX = cutrrentX;
							isVideoTouchMove = true;
							
						} else {
							if (MainActivity.isDebug) {
								Log.e("gray", "PlayActivity.java: playVideo, X not over the threshold ");
							}
						}
		            	
		            	if (cutrrentY - previousY > 10) {
		            		videoWidth = videoWidth + (int)(videoWidth * 0.1);
		            		videoHeight = videoHeight + (int)(videoHeight * 0.1);
		            		
		            		RelativeLayout.LayoutParams videoviewlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
				            videoviewlp.width = videoWidth;
				            videoviewlp.height = videoHeight;
				            mVideoView.setLayoutParams(videoviewlp);
				            mVideoView.requestLayout();
				            mVideoView.invalidate();     // very important, so that onMeasure will be triggered
				          
				            previousY = cutrrentY;
							isVideoTouchMove = true;
							
						} else if (cutrrentY - previousY < -10) {
							videoWidth = videoWidth - (int)(videoWidth * 0.1);
		            		videoHeight = videoHeight - (int)(videoHeight * 0.1);
		            		
		            		RelativeLayout.LayoutParams videoviewlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
				            videoviewlp.width = videoWidth;
				            videoviewlp.height = videoHeight;
				            mVideoView.setLayoutParams(videoviewlp);
				            mVideoView.requestLayout();
				            mVideoView.invalidate();     // very important, so that onMeasure will be triggered
				          
				            previousY = cutrrentY;
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
			            if (!isVideoTouchMove) {
			            	if (mVideoView.isPlaying()) {
			            		mVideoView.pause();
			            	} else {
			            		mVideoView.start();
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
		
		srcString = srcCS.toString();
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
			Log.e("gray", "PlayActivity.java: " + "statsNode.length < 0");
		}
	    
	    // save translate word to note
		FileWriter fw;
		try {
			fw = new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName+".cnnsNote.txt", true);
			BufferedWriter bw = new BufferedWriter(fw);
			translatedText = translatedText.replaceAll("\n\n", "\n");
			bw.write(srcCS + translatedText + "\n");
			bw.close();
		} catch (IOException e) {
			Log.e("gray", "PlayActivity.java:run, save script error, Exception e:" + e.toString()); 
			e.printStackTrace();
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
				
				try {
					mProgressDialogScript.dismiss();
					mProgressDialogScript = null;
				} catch (Exception e) {
					// nothing
				}
				
				break;

			case 1:
				
				ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);;
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
		s = s.replaceAll("\n\n\n\n\n", "\n");
		s = s.replaceAll("\n\n\n\n", "\n");
//		s = s.replaceAll("\n\n\n", "\n");
		mTextView.setText(s);
		mTextView.setTextSize(MainActivity.textSize);
		
		switch (MainActivity.scriptTheme) {
		case 0:
			mTextView.setTextColor(0xff000000);
			mTextView.setBackgroundColor(0xffffffff);
			break;
		case 1:
			mTextView.setTextColor(0xffffffff);
			mTextView.setBackgroundColor(0xff000000);
			break;
		case 2:
			mTextView.setTextColor(0xffffffff);
			mTextView.setBackgroundColor(0xff4D0D2A);
			break;
		case 3:
			mTextView.setTextColor(0xff000000);
			mTextView.setBackgroundColor(0xffFFF396);
			break;
		case 4:
			mTextView.setTextColor(0xff00C22E);
			mTextView.setBackgroundColor(0xff000000);
			break;
		case 5:
			mTextView.setTextColor(0xffffffff);
			mTextView.setBackgroundColor(0xff008312);
			break;
		case 6:
			mTextView.setTextColor(0xffffffff);
			mTextView.setBackgroundColor(0xff1038AA);
			break;
		case 7:
			mTextView.setTextColor(0xffffffff);
			mTextView.setBackgroundColor(0xffA8050A);
			break;
		default:
			break;
		}
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
	
	@Override
    public void onPrepared(MediaPlayer mp) {
		videoWidth = mp.getVideoWidth();
		videoHeight = mp.getVideoHeight();

		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: onPrepared");
			Log.e("gray", "PlayActivity.java: videoWidth : " + videoWidth);
			Log.e("gray", "PlayActivity.java: videoHeight : " + videoHeight);
		}
		
		// change layout to WRAP_CONTENT
        RelativeLayout.LayoutParams videoviewlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        mVideoView.setLayoutParams(videoviewlp);
        mVideoView.invalidate();
        
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

        if (MainActivity.isDebug) {
        	Log.e("gray", "PlayActivity.java: onPause ");
		}
        stopPosition = mVideoView.getCurrentPosition(); //stopPosition is an int
        isVideoPlaying = mVideoView.isPlaying();
        mVideoView.pause();
	}
	
	protected void onResume() {
		super.onResume();
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java: onResume ");
		}
		mVideoView.seekTo(stopPosition);

		if (isVideoPlaying) {
			mVideoView.start();
		} else {
			mVideoView.resume();
		}
	}
	
//	@Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//		
//		if (MainActivity.isDebug) {
//			Log.e("gray", "PlayActivity.java:onOptionsItemSelected, " + "");
//		}
//		
//		return true;
//	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (MainActivity.isDebug) {
			Log.e("gray", "PlayActivity.java:onKeyDown, " + "");
		}
	    if (keyCode == KeyEvent.KEYCODE_MENU) {
	    	if (MainActivity.isDebug) {
	    		Log.e("gray", "PlayActivity.java:onKeyDown, " + "KeyEvent.KEYCODE_MENU");
			}
	    	
	    	if (mVideoView.isPlaying()) {
				mVideoView.pause();
			} else {
				mVideoView.start();
			}
	    	
	    	if (MainActivity.isEnableLongPressTranslate) {
				
	    		//translate
	    		if (mTextView.getSelectionStart() != mTextView.getSelectionEnd()) {
	    			
	    			srcText = mTextView.getText().subSequence(mTextView.getSelectionStart(), mTextView.getSelectionEnd());
	    			if (isNetworkAvailable()){
	    				
	    				if (MainActivity.isDebug) {
	    					Log.e("gray", "PlayActivity.java: TextView.onClick : " + mTextView.getSelectionStart() + "--"+ mTextView.getSelectionEnd());
	    					Log.e("gray", "PlayActivity.java: TextView.onClick : " + srcText);
	    				}
	    				
	    				showProcessDialog(2, "Please Wait...", "Translate...");
	    				
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
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
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

	// do't show settings at this page
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.play, menu);
//		return true;
//	}
	
}