package com.graylin.cnns;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
  
import android.annotation.SuppressLint;
import android.app.Activity;  
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;  
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;  
import android.widget.LinearLayout;
import android.widget.ListView;  
import android.widget.SimpleAdapter;

public class MainActivity extends Activity {
	
	public static boolean isDebug = false;
//	public static boolean isDebug = true;

	public static boolean isNeedUpdate = false;
	public static boolean waitFlag = false;
	public static boolean isEverLoaded = false;
	
	public static final int MAX_LIST_ARRAY_SIZE = 20;
	public static String[] cnnListStringArray = new String [MAX_LIST_ARRAY_SIZE];
	public static String[] cnnScriptAddrStringArray = new String [MAX_LIST_ARRAY_SIZE];
	
	public final String scriptAddressStringPrefix = "http://transcripts.cnn.com/TRANSCRIPTS/";
	public final String scriptAddressStringPostfix = "/sn.01.html";
	public static String scriptAddressString = "";
	
	public final String videoAddressStringPrefix = "http://podcasts.cnn.net/cnn/big/podcasts/studentnews/video/";
	public final String videoAddressStringPostfix = ".cnn.m4v";
	public static String videoAddressString = "";
	
	public ListView listView;
	public ArrayAdapter<String> adapter;
	public AdView adView;
	// ProgressDialog, wait network work to be done
	public ProgressDialog mProgressDialog;
	
	// HTML page
	public static final String CNNS_URL = "http://edition.cnn.com/US/studentnews/quick.guide/archive/";
	
    // XPath query
	public String XPATH = "";
	
	// SharedPreferences instance
	public static SharedPreferences sharedPrefs;
	public static SharedPreferences.Editor sharedPrefsEditor;
	
	// settings variable
	public static boolean isEnableDownload;
	public static int textSize;
	public static int swipeTime;
	public static int scriptTheme;
	public static int autoDelete;
	public static String translateLanguage;
	public static boolean isEnableLongPressTranslate;
	public static boolean isEnableSoftButtonTranslate;
	public static boolean isVideoControlBar;
	
	// broadcast receiver
	public AudioManager audioManager;
	public ComponentName componentName = null;
	
	// orientation
	public static int originOrientation;
	
	// AdBuddiz ad
	public static boolean isAdBuddizEverLoad;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (isDebug) {
			Log.e("gray", "MainActivity.java: START ===============");
		}
		
		// register broadcast event
		audioManager =(AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
		componentName = new ComponentName(this, RemoteControlReceive.class);
		audioManager.registerMediaButtonEventReceiver(componentName);  
		
		// get SharedPreferences instance
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		sharedPrefsEditor = sharedPrefs.edit();  
		
		// get initial data
		isAdBuddizEverLoad = false;
		isEnableDownload = sharedPrefs.getBoolean("pref_download", false);
		isVideoControlBar = sharedPrefs.getBoolean("pref_videoControlBar", true);
		try {
			textSize = Integer.valueOf(sharedPrefs.getString("pref_textSize", "18"));
		} catch (Exception e) {
			Log.e("gray", "MainActivity.java: onCreate, Exception : " + e.toString() );
		}
        if (textSize < 8) {
        	textSize = 8;
		} else if (textSize > 50){
        	textSize = 50;
        }
        
        try {
			swipeTime = Integer.valueOf(sharedPrefs.getString("pref_swipeTime", "3"));
		} catch (Exception e) {
			Log.e("gray", "MainActivity.java: onCreate, Exception : " + e.toString() );
		}
        if (swipeTime < 1) {
        	swipeTime = 1;
		} else if (swipeTime > 20){
			swipeTime = 20;
        }
        
        isEverLoaded = sharedPrefs.getBoolean("isEverLoaded", false);
        if (isEverLoaded) {
        	
        	if (isDebug) {
        		Log.e("gray", "MainActivity.java: isEverLoaded !!");
			}
        	for (int i = 0; i < MAX_LIST_ARRAY_SIZE; i++) {
				cnnListStringArray[i] = sharedPrefs.getString("cnnListString_"+i, "");
				cnnScriptAddrStringArray[i] = sharedPrefs.getString("cnnScriptAddrString_"+i, "");
				
				if (isDebug) {
					Log.e("gray", "MainActivity.java: cnnListStringArray[i]:" + cnnListStringArray[i]);
					Log.e("gray", "MainActivity.java: cnnScriptAddrStringArray[i]:" + cnnScriptAddrStringArray[i]);
				}
			}
        	showListView();
        	
		} else {
			
			showAlertDialog("First Use Message", 
					"How to use this app please have a look at \"Information\" page; " +
					"errors, bugs or questions please check \"Q & A\" page first.\n\n"
					);
			
			// initial array
			for (int i = 0; i < MAX_LIST_ARRAY_SIZE; i++) {
				cnnListStringArray[i] = "initail string value";
				cnnScriptAddrStringArray[i] = "initail string value";
			}
		}
        
        scriptTheme = Integer.valueOf( sharedPrefs.getString("pref_script_theme", "0") );
        translateLanguage = sharedPrefs.getString("pref_translate_language", "zh-TW");
//        isEnableLongPressTranslate = sharedPrefs.getBoolean("pref_longpress_translate", false);
        isEnableSoftButtonTranslate = sharedPrefs.getBoolean("pref_soft_button_translate", false);
        autoDelete = Integer.valueOf( sharedPrefs.getString("pref_auto_delete_file", "0") );
		
        // check if need to update, set isNeedUpdate = true / false
        // if there is new video, then update
        
        // check if video of cnns website update
		// get string of last video source (every hour check right now) 
        isNeedUpdate = false;
        waitFlag = false;
        String lastVideosource = sharedPrefs.getString("lastVideosource", "");
        if (isNetworkAvailable()) {
        	
        	SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd-kk", Locale.US);
        	String currentTime = s.format(new Date());
        	boolean isCheckVidoeUpdate = false;
        	
        	// get last update time
        	String lastUpdateTime = sharedPrefs.getString("lastUpdateTime", "");
        	
        	if (currentTime.equalsIgnoreCase(lastUpdateTime)) {
        		isCheckVidoeUpdate = false;
        	} else {
        		isCheckVidoeUpdate = true;
        		sharedPrefsEditor.putString("lastUpdateTime", currentTime);
        	}
        	if (isDebug) {
        		Log.e("gray", "MainActivity.java: currentTime: " + currentTime);
        		Log.e("gray", "MainActivity.java: lastUpdateTime: " + lastUpdateTime);
        		Log.e("gray", "MainActivity.java: isCheckVidoeUpdate: " + isCheckVidoeUpdate);
        	}
        	
        	if (isCheckVidoeUpdate) {
				
        		new Thread(new Runnable() 
        		{ 
        			@Override
        			public void run() 
        			{ 
        				try {
        					isVideoUpdate();
        				} catch (Exception e) {
        					Log.e("gray", "MainActivity.java:onCreate, " + "isVideoUpdate, Exception : " + e.toString());
        					e.printStackTrace();
        				}
        			} 
        		}).start();
        		
        		// wait variable "isNeedUpdate" to be set or timeout
        		int tempCounter = 0;
        		while ( !(waitFlag || tempCounter > 35) ) {	// timeout 7s
        			try {
        				Thread.sleep(200);
        				tempCounter++;
        			} catch (InterruptedException e) {
        				e.printStackTrace();
        			}
        		}
        		if (isDebug) {
        			Log.e("gray", "MainActivity.java:onCreate, tempCounter: " + tempCounter);
				}
			}
        }
		
		// check if need to update, set isNeedUpdate = true / false
		// if current date string different, then update
		SimpleDateFormat s1 = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		String currentDate = s1.format(new Date());
		boolean isTooLongNoUpdate = false;
		
		// get last update date
		String lastUpdateDate = sharedPrefs.getString("lastUpdateDate", "");
		
		if (currentDate.equalsIgnoreCase(lastUpdateDate)) {
			isTooLongNoUpdate = false;
		} else {
			isTooLongNoUpdate = true;
			sharedPrefsEditor.putString("lastUpdateDate", currentDate);
		}
		if (isDebug) {
			Log.e("gray", "MainActivity.java: currentDate: " + currentDate);
			Log.e("gray", "MainActivity.java: lastUpdateDate: " + lastUpdateDate);
			Log.e("gray", "MainActivity.java: isTooLongNoupdate: " + isTooLongNoUpdate);
		}
		
		if (isTooLongNoUpdate) {
			isNeedUpdate = true;

			// manage file (auto delete)
			if ( autoDelete != 0 ) {
				
				new Thread(new Runnable() 
				{ 
					@Override
					public void run() 
					{ 
						deleteCNNSFiles(autoDelete);
					} 
				}).start();
				showListView();
			} 
		}	
		
		// for debug, force update
		if (isDebug) {
			isNeedUpdate = true;
		}
		
		// check if need update (there is new video && too long(1 day) no update, then update) 
		if (isNeedUpdate) {	
			
			if (isNetworkAvailable()) {
				
				showProcessDialog("Please Wait...", "Update Data From CNN Student News...");
				
				new Thread(new Runnable() 
				{ 
					@Override
					public void run() 
					{ 
						try {
							getCNNSTitle();
							handler.sendEmptyMessage(0);
						} catch (Exception e) {
							Log.e("gray", "MainActivity.java:getCNNSTitle, Exception:" + e.toString());
							e.printStackTrace();
						}
					} 
				}).start();
				
			} else {
				
				if (lastVideosource.equalsIgnoreCase("")) {
					//never have cnns data
					showAlertDialog("Error", "Never get data from CNN student news! Please enable network and try again.");
				} else {
						
					if (isDebug) {
						for (int i = 0; i < MAX_LIST_ARRAY_SIZE; i++) {
						Log.e("gray", "MainActivity.java: cnnListStringArray[i]:" + cnnListStringArray[i]);
						Log.e("gray", "MainActivity.java: cnnScriptAddrStringArray[i]:" + cnnScriptAddrStringArray[i]);
						}
					}
					showListView();
				}
			}
			
		// no need to update, imply cnns data ever loaded
		} else {		
			
			if (isDebug) {
				for (int i = 0; i < MAX_LIST_ARRAY_SIZE; i++) {
				Log.e("gray", "MainActivity.java: cnnListStringArray[i]:" + cnnListStringArray[i]);
				Log.e("gray", "MainActivity.java: cnnScriptAddrStringArray[i]:" + cnnScriptAddrStringArray[i]);
				}
			}
			showListView();
		}
		
		// load AD
		adView = new AdView(this, AdSize.SMART_BANNER, "a151e4fa6d7cf0e");
		LinearLayout layout = (LinearLayout) findViewById(R.id.ADLayout);
		layout.addView(adView);
		adView.loadAd(new AdRequest());
		
         if (isDebug) {
			Log.e("gray", "MainActivity.java: END =================");
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (isDebug) {
			Log.e("gray", "MainActivity.java: onActivityResult, requestCode: " + requestCode);
			Log.e("gray", "MainActivity.java: onActivityResult, resultCode: " + resultCode);
		}
		
		// reload AD
		adView.loadAd(new AdRequest());
		
        switch (requestCode) {
		case 0:
			// back to main page from settings page, set settings value to variable
			if (isDebug) {
				Log.e("gray", "MainActivity.java: pref_download :" + sharedPrefs.getBoolean("pref_download", false) );
				Log.e("gray", "MainActivity.java: pref_videoControlBar :" + sharedPrefs.getBoolean("pref_videoControlBar", false) );
				Log.e("gray", "MainActivity.java: pref_textSize :" + sharedPrefs.getString("pref_textSize", "18") );
				Log.e("gray", "MainActivity.java: pref_swipeTime :" + sharedPrefs.getString("pref_swipeTime", "3") );
				Log.e("gray", "MainActivity.java: pref_script_theme :" + sharedPrefs.getString("pref_script_theme", "0") );
				Log.e("gray", "MainActivity.java: pref_translate_language :" + sharedPrefs.getString("pref_translat_language", "zh-TW") );
				Log.e("gray", "MainActivity.java: pref_auto_delete_file :" + sharedPrefs.getString("pref_auto_delete_file", "0") );
			}
			
			isEnableDownload = sharedPrefs.getBoolean("pref_download", false);
			isVideoControlBar = sharedPrefs.getBoolean("pref_videoControlBar", true);
			
			try {
				textSize = Integer.valueOf(sharedPrefs.getString("pref_textSize", "18"));
			} catch (Exception e) {
				Log.e("gray", "MainActivity.java: onCreate, Exception : " + e.toString() );
			}
	        if (textSize < 8) {
	        	textSize = 8;
			} else if (textSize > 50){
	        	textSize = 50;
	        }
	        
	        try {
				swipeTime = Integer.valueOf(sharedPrefs.getString("pref_swipeTime", "3"));
			} catch (Exception e) {
				Log.e("gray", "MainActivity.java: onCreate, Exception : " + e.toString() );
			}
	        if (swipeTime < 1) {
	        	swipeTime = 1;
			} else if (swipeTime > 20){
				swipeTime = 20;
	        }
			
	        scriptTheme = Integer.valueOf( sharedPrefs.getString("pref_script_theme", "0") );
			translateLanguage = sharedPrefs.getString("pref_translate_language", "zh-TW");
//			isEnableLongPressTranslate = sharedPrefs.getBoolean("pref_longpress_translate", false);
			isEnableSoftButtonTranslate = sharedPrefs.getBoolean("pref_soft_button_translate", false);
			autoDelete = Integer.valueOf( sharedPrefs.getString("pref_auto_delete_file", "0") );
			
			if (autoDelete != 0) {

				new Thread(new Runnable() {
					@Override
					public void run() {
						deleteCNNSFiles(autoDelete);
						handler.sendEmptyMessage(1);
					}
				}).start();
			}
			
			break;
			
		case 1:
			setRequestedOrientation(originOrientation);
			showListView();
			
			break;
			
		case 2:
			showListView();
			break;

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
	
	public void showListView() {
		
		// Get ListView object from res
	    listView = (ListView) findViewById(R.id.mainListView);
	    
	    ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();  
        for(int i = 0; i < MAX_LIST_ARRAY_SIZE; i++) {
        	
        	HashMap<String, Object> map = new HashMap<String, Object>();  
        	String cnnVideoName = "";
        	
    		String [] tempSA = new String [32];
            tempSA = cnnScriptAddrStringArray[i].split("/");
            if (isDebug) {
                Log.e("gray", "MainActivity.java:cnnListStringArray, length : " + tempSA.length);
                for (int j = 0; j < tempSA.length; j++) {
                    Log.e("gray", "MainActivity.java:showListView, " + j + " : " + tempSA[j]);
                }
            }
            
            int archiveYear = 0, archiveMonth, archiveDay, realYear = 0, realMonth = 0, realDay = 0;
            String archiveMonthS = null, archiveDayS = null, realMonthS = null, realDayS = null;
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            try {
//                Date date = df.parse("2013-12-31");
                Date date = df.parse(tempSA[1] + "-" + tempSA[2] + "-" + tempSA[3]);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
 
                archiveYear = cal.get(Calendar.YEAR);
                archiveMonth = cal.get(Calendar.MONTH) + 1;
                archiveDay = cal.get(Calendar.DAY_OF_MONTH);
                archiveMonthS = String.format(Locale.US, "%02d", archiveMonth);
                archiveDayS = String.format(Locale.US, "%02d", archiveDay);
                
                cal.add(Calendar.DAY_OF_MONTH, 1);
                realYear = cal.get(Calendar.YEAR);
                realMonth = cal.get(Calendar.MONTH) + 1;
                realDay = cal.get(Calendar.DAY_OF_MONTH);
                realMonthS = String.format(Locale.US, "%02d", realMonth);
                realDayS = String.format(Locale.US, "%02d", realDay);
                
            } catch (ParseException e) {
                Log.e("gray", "MainActivity.java:showListView, ParseException, " + e.toString());
                e.printStackTrace();
            }
    		
    		if (isDebug) {
    			Log.e("gray", "MainActivity.java: archive date : " + archiveYear + archiveMonthS + archiveDayS);
    			Log.e("gray", "MainActivity.java: real date : " + realYear + realMonthS + realDayS);
			}
    		
    		// check if video file already download, or set path to local dir
    		cnnVideoName = "/sn-" + realMonthS + realDayS + (realYear-2000) + videoAddressStringPostfix;
    		if (isDebug) {
    			Log.e("gray", "path: " + Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+cnnVideoName);
			}
    		
    		// put title
            map.put("ItemTitle", cnnListStringArray[i]);
            
            // put script image
            if (isFileExist(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+cnnVideoName+".txt")) {
            	map.put("ItemImage_news", R.drawable.ic_newspaper_o);  
            } else {
            	map.put("ItemImage_news", R.drawable.ic_newspaper_x);  
			}
            
            // put video image
            if (isFileExist(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+cnnVideoName)) {
            	map.put("ItemImage_video", R.drawable.ic_video_o);  
            } else {
            	map.put("ItemImage_video", R.drawable.ic_video_x);  
			}
            listItem.add(map);  
        }  
	    
        SimpleAdapter listItemAdapter = new SimpleAdapter(this,listItem, R.layout.cnn_listview, new String[] {"ItemTitle","ItemImage_news", "ItemImage_video"}, new int[] {R.id.ItemTitle,R.id.ItemImage_news,R.id.ItemImage_video});  
            
        listView.setAdapter(listItemAdapter);  
        
	    listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
				
				if (isDebug) {
					Log.e("gray", "MainActivity.java:onItemClick" + "Position : " + position + ", id : " + id);
				}
				
				String [] tempSA = new String [32];
	            tempSA = cnnScriptAddrStringArray[position].split("/");
	            if (isDebug) {
	                Log.e("gray", "MainActivity.java:cnnListStringArray, length : " + tempSA.length);
	                for (int j = 0; j < tempSA.length; j++) {
	                    Log.e("gray", "MainActivity.java:showListView, " + j + " : " + tempSA[j]);
	                }
	            }
	            
	            int archiveYear = 0, archiveMonth, archiveDay, realYear = 0, realMonth = 0, realDay = 0;
	            String archiveMonthS = null, archiveDayS = null, realMonthS = null, realDayS = null;
	            DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	            try {
//	                Date date = df.parse("2013-12-31");
	                Date date = df.parse(tempSA[1] + "-" + tempSA[2] + "-" + tempSA[3]);
	                Calendar cal = Calendar.getInstance();
	                cal.setTime(date);
	 
	                archiveYear = cal.get(Calendar.YEAR);
	                archiveMonth = cal.get(Calendar.MONTH) + 1;
	                archiveDay = cal.get(Calendar.DAY_OF_MONTH);
	                archiveMonthS = String.format(Locale.US, "%02d", archiveMonth);
	                archiveDayS = String.format(Locale.US, "%02d", archiveDay);
	                
	                cal.add(Calendar.DAY_OF_MONTH, 1);
	                realYear = cal.get(Calendar.YEAR);
	                realMonth = cal.get(Calendar.MONTH) + 1;
	                realDay = cal.get(Calendar.DAY_OF_MONTH);
	                realMonthS = String.format(Locale.US, "%02d", realMonth);
	                realDayS = String.format(Locale.US, "%02d", realDay);
	                
	            } catch (ParseException e) {
	                Log.e("gray", "MainActivity.java:showListView, ParseException, " + e.toString());
	                e.printStackTrace();
	            }
	    		
				scriptAddressString =  scriptAddressStringPrefix + (realYear-2000) + realMonthS + "/" + realDayS + scriptAddressStringPostfix;
				videoAddressString = videoAddressStringPrefix + archiveYear + "/" + archiveMonthS + "/" + archiveDayS + "/sn-" + realMonthS + realDayS + (realYear-2000) + videoAddressStringPostfix; 

				if (isDebug) {
					Log.e("gray", "MainActivity.java:onItemClick, " + "scriptAddressString:" + scriptAddressString);
					Log.e("gray", "MainActivity.java:onItemClick, " + "vodeoAddressString:" + videoAddressString);
				}
			
				originOrientation = getResources().getConfiguration().orientation;
				
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, PlayActivity.class);
				startActivityForResult(intent, 1);
			}
	    }); 
	}
	
	@SuppressLint("HandlerLeak")
	Handler handler = new Handler() {  
        @Override  
        public void handleMessage(Message msg) {
        	
           	switch (msg.what) {
    			case 0:
    				
    				try {
    					mProgressDialog.dismiss();
    					mProgressDialog = null;
    					showListView();
    				} catch (Exception e) {
    					// nothing
    				}
    				
    				break;

    			case 1:
    				showListView();
    				break;
    					
           	}
        }  
    };  
	
    public void isVideoUpdate() throws Exception {
    	
    	String resultS = "";
    	String newestVideoSource = "";
	    Object[] resultSNode;
	
	    // config cleaner properties
	    HtmlCleaner htmlCleaner = new HtmlCleaner();
	    CleanerProperties props = htmlCleaner.getProperties();
	    props.setAllowHtmlInsideAttributes(false);
	    props.setAllowMultiWordAttributes(true);
	    props.setRecognizeUnicodeChars(true);
	    props.setOmitComments(true);
	    
	    // create URL object
	    URL url = new URL("http://rss.cnn.com/services/podcasting/studentnews/rss.xml");
	    // get HTML page root node
	    TagNode root = htmlCleaner.clean(url);

	    // query XPath
	    XPATH = "//source";
	    resultSNode = root.evaluateXPath(XPATH);

	    // process data if found any node
	    if(resultSNode.length > 0) {
	    	
	    	if (isDebug) {
	    		Log.e("gray", "MainActivity.java:isVideoUpdate, resultSNode.length:" + resultSNode.length);
	    		for (int i = 0; i < resultSNode.length; i++) {
	    			
	    			TagNode resultNode = (TagNode)resultSNode[i];
	    			resultS = resultNode.getText().toString();
	    			Log.e("gray", "MainActivity.java:isVideoUpdate, " + resultS);
	    		}
			}
	    	
	    	TagNode resultNode = (TagNode)resultSNode[0];
	    	newestVideoSource = resultNode.getText().toString();
	    	
	    } else {
	    	Log.e("gray", "MainActivity.java:isVideoUpdate, resultSNode.length <= 0, err!!");
		}
	    
	    String [] tempSA = new String [32];
        tempSA = newestVideoSource.split(" ");
        if (isDebug) {
            Log.e("gray", "MainActivity.java:isVideoUpdate, length : " + tempSA.length);
            for (int j = 0; j < tempSA.length; j++) {
                Log.e("gray", "MainActivity.java:isVideoUpdate, " + j + " : " + tempSA[j]);
            }
        }
        newestVideoSource = tempSA[5];
        
		// get last video source string
		String lastVideosource = sharedPrefs.getString("lastVideosource", "");
		if (newestVideoSource.equalsIgnoreCase(lastVideosource)) {
			isNeedUpdate = false;
		} else {
			isNeedUpdate = true;
			sharedPrefsEditor.putString("lastVideosource", newestVideoSource);
		}
		if (isDebug) {
			Log.e("gray", "MainActivity.java: newestVideoSource: " + newestVideoSource);
			Log.e("gray", "MainActivity.java: lastVideosource: " + lastVideosource);
			Log.e("gray", "MainActivity.java: isNeedUpdate: " + isNeedUpdate);
		}
			
    	sharedPrefsEditor.commit();
    	
		waitFlag = true;
    }
    
	public void getCNNSTitle() throws Exception {
	    
		String resultS = "";
		String matchString = "CNN Student News";
		String lastVideosource = sharedPrefs.getString("lastVideosource", "");
		String comparedDateS = "";
		String comparedDateS2 = "";
		String comparedDateS3 = "";
	    int arrayIndex = 0;
	    Object[] resultSNode;
	
	    if (isDebug) {
	    	Log.e("gray", "MainActivity.java:getCNNSTitle, " + "");
		}
	    
	    // config cleaner properties
	    HtmlCleaner htmlCleaner = new HtmlCleaner();
	    CleanerProperties props = htmlCleaner.getProperties();
	    props.setAllowHtmlInsideAttributes(false);
	    props.setAllowMultiWordAttributes(true);
	    props.setRecognizeUnicodeChars(true);
	    props.setOmitComments(true);
	
	    // create URL object
	    URL url = new URL(CNNS_URL);
	    // get HTML page root node
	    TagNode root = htmlCleaner.clean(url);

	    // query XPath
	    XPATH = "//div[@class='cnn_spccovt1cllnk cnn_spccovt1cll2']//h2//a";
	    resultSNode = root.evaluateXPath(XPATH);

	    // process data if found any node
    	if(resultSNode.length > 0) {
    		
    		if (isDebug) {
    			Log.e("gray", "MainActivity.java:getCNNSTitle, resultSNode.length:" + resultSNode.length);
    		}
    		for (int i = 0; i < resultSNode.length; i++) {
    			
    			TagNode resultNode = (TagNode)resultSNode[i];
    			resultS = resultNode.getText().toString();
    			
    			String [] tempSA = new String [32];
    	        tempSA = resultS.split(" ");
    	        if (isDebug) {
    	            Log.e("gray", "MainActivity.java:isVideoUpdate, length : " + tempSA.length);
    	            for (int j = 0; j < tempSA.length; j++) {
    	                Log.e("gray", "MainActivity.java:isVideoUpdate, " + j + " : " + tempSA[j]);
    	            }
    	        }
    	        comparedDateS = tempSA[5];
    	        comparedDateS2 = tempSA[4];
    	        comparedDateS3 = tempSA[6];
    			
    			if (resultS.regionMatches(0, matchString, 0, matchString.length())) {
    				if (isDebug) {
    					Log.e("gray", "MainActivity.java:getCNNSTitle, lastVideosource:" + lastVideosource);
    					Log.e("gray", "MainActivity.java:getCNNSTitle, resultS:" + resultS);
    					Log.e("gray", "MainActivity.java:getCNNSTitle, comparedDateS:" + comparedDateS);
					}
					if (lastVideosource.equalsIgnoreCase(comparedDateS)  || 
						lastVideosource.equalsIgnoreCase(comparedDateS2) ||
						lastVideosource.equalsIgnoreCase(comparedDateS3) ) {
						
	    				resultS = resultS.replace("CNN Student News -", "");
	    				cnnListStringArray[arrayIndex] = resultS;
	    				cnnScriptAddrStringArray[arrayIndex] = resultNode.getAttributeByName("href");
	    				
	    				sharedPrefsEditor.putString("cnnListString_"+arrayIndex, cnnListStringArray[arrayIndex]);
	    				sharedPrefsEditor.putString("cnnScriptAddrString_"+arrayIndex, cnnScriptAddrStringArray[arrayIndex]);
	    				if (isDebug) {
	    					Log.e("gray", "MainActivity.java:getCNNSTitle, i:" + (i) + ", arrayIndex:" + arrayIndex + ", getAttributeByName = " + resultNode.getAttributeByName("href"));
	    				}
	    				
	    				arrayIndex++;
					} else {
						break;
					}
    			} else {
    				if (isDebug) {
    					Log.e("gray", "MainActivity.java:getCNNSTitle, string not match!!" );
    				}
    			}
    		}
    		
    	} else {
    		Log.e("gray", "resultSNode.length <= 0, err!!");
    	}
	    
	    // query XPath
	    XPATH = "//div[@class='cnn_mtt1imghtitle']//span//a";
	    resultSNode = root.evaluateXPath(XPATH);

	    // process data if found any node
	    if(resultSNode.length > 0) {
	    	
	    	if (isDebug) {
	    		Log.e("gray", "MainActivity.java:getCNNSTitle, resultSNode.length:" + resultSNode.length);
			}
	    	for (int i = 0; i < resultSNode.length; i++) {
				
	    		TagNode resultNode = (TagNode)resultSNode[i];
	    		resultS = resultNode.getText().toString();
	    		
	    		if (resultS.regionMatches(0, matchString, 0, matchString.length())) {
					
	    			resultS = resultS.replace("CNN Student News -", "");
	    			cnnListStringArray[arrayIndex] = resultS;
	    			cnnScriptAddrStringArray[arrayIndex] = resultNode.getAttributeByName("href");
	    			
	    			sharedPrefsEditor.putString("cnnListString_"+arrayIndex, cnnListStringArray[arrayIndex]);
	    			sharedPrefsEditor.putString("cnnScriptAddrString_"+arrayIndex, cnnScriptAddrStringArray[arrayIndex]);
	    			if (isDebug) {
	    				Log.e("gray", "MainActivity.java:getCNNSTitle, i:" + (i) + ", arrayIndex:" + arrayIndex + ", getAttributeByName = " + resultNode.getAttributeByName("href"));
					}
	    			
	    			arrayIndex++;
				} else {
					if (isDebug) {
						Log.e("gray", "MainActivity.java: string not match!!" );
					}
				}
	    	}
	    	
	    } else {
	    	Log.e("gray", "resultSNode.length <= 0, err!!");
		}
	    
	    // query XPath
	    XPATH = "//div[@class='archive-item story cnn_skn_spccovstrylst']//h2//a";
	    resultSNode = root.evaluateXPath(XPATH);

	    // process data if found any node
	    if(resultSNode.length > 0) {
	    	
	    	if (isDebug) {
	    		Log.e("gray", "MainActivity.java:getCNNSTitle, resultSNode.length:" + resultSNode.length);
			}
	    	for (int i = 0; arrayIndex < MAX_LIST_ARRAY_SIZE; i++) {
				
	    		TagNode resultNode = (TagNode)resultSNode[i];
	    		resultS = resultNode.getText().toString();
	    		
	    		if (resultS.regionMatches(0, matchString, 0, matchString.length())) {
					
	    			resultS = resultS.replace("CNN Student News Transcript -", "");
	    			resultS = resultS.replace("CNN Student News -", "");
	    			cnnListStringArray[arrayIndex] = resultS;
	    			cnnScriptAddrStringArray[arrayIndex] = resultNode.getAttributeByName("href");
	    			
	    			sharedPrefsEditor.putString("cnnListString_"+arrayIndex, cnnListStringArray[arrayIndex]);
	    			sharedPrefsEditor.putString("cnnScriptAddrString_"+arrayIndex, cnnScriptAddrStringArray[arrayIndex]);
	    			if (isDebug) {
	    				Log.e("gray", "MainActivity.java:getCNNSTitle, i:" + (i) + ", arrayIndex:" + arrayIndex + ", getAttributeByName = " + resultNode.getAttributeByName("href"));
					}
	    			
	    			arrayIndex++;
				} else {
					if (isDebug) {
						Log.e("gray", "MainActivity.java: string not match!!" );
					}
				}
	    	}
	    	
	    	sharedPrefsEditor.putBoolean("isEverLoaded", true);
	    	
	    	sharedPrefsEditor.commit();
	        
	    } else {
	    	Log.e("gray", "resultSNode.length <= 0, err!!");
		}
	}
	
	public static String getScriptAddress() {
		return scriptAddressString;
	}
  
	public static String getVideoAddress() {
		return videoAddressString;
	}
	
    public void showProcessDialog(CharSequence title, CharSequence message){
    	
		mProgressDialog = ProgressDialog.show(MainActivity.this, title, message, true);
		mProgressDialog.setCancelable(true); 
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
		AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
	}
    
	public void deleteCNNSFiles(int deleteParameter){
		
		if (isDebug) {
			Log.e("gray", "MainActivity.java:deleteCNNSFiles, " + "");
		}
		
		// Directory path
		String path = Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS;
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();
		int survivalDay = 0;
		
		switch (deleteParameter) {
			case 1:
				survivalDay = 5;
				break;
			case 2:
				survivalDay = 10;
				break;
			case 3:
				survivalDay = 15;
				break;
			case 4:
				survivalDay = 20;
				break;
			case 5:
				survivalDay = 25;
				break;
			case 6:
				survivalDay = 30;
				break;
		}

		if (listOfFiles != null) {
			
			for (int i = 0; i < listOfFiles.length; i++) {
				
				if (listOfFiles[i].isFile()) {
					
					Date currentDate = new Date();
					long lCurrentDate = currentDate.getTime();
					if (listOfFiles[i].getName().contains(".cnn.m4v")) {
						
						Date lastModDate = new Date(listOfFiles[i].lastModified());
						long diff = lCurrentDate - lastModDate.getTime();
						
						if (isDebug) {
							Log.e("gray", "MainActivity.java:manageCNNSFiles, currentDate : " + currentDate);
							Log.e("gray", "MainActivity.java:manageCNNSFiles, file : " + listOfFiles[i].getName() + " : " + lastModDate.toString());
							Log.e("gray", "MainActivity.java:manageCNNSFiles, Difference is : " + (diff/(1000*60*60*24)) + " days.");
						}
						
						if ((diff/(1000*60*60*24)) > survivalDay ) {
							listOfFiles[i].delete();
						}
					}
				}
			}
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		if (isDebug) {
			Log.e("gray", "MainActivity.java: onCreateOptionsMenu");
		}
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		
		if (isDebug) {
			Log.e("gray", "MainActivity.java:onOptionsItemSelected, " + "");
		}
        switch (item.getItemId()) {
 
        case R.id.action_settings:
        	if (isDebug) {
        		Log.e("gray", "MainActivity.java:onOptionsItemSelected, case R.id.action_settings");
        	}
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, 0);
            break;
            
        case R.id.action_info:
        	if (isDebug) {
        		Log.e("gray", "MainActivity.java:onOptionsItemSelected, case R.id.action_info");
        	}
        	showAlertDialog("Information", 
        			"What's new in this version (1.34) ?\n\n" +
					"fix get no srcipt on December 10, 2013\n" +
					"PS : Because CNNS website change the URL of script for no reason, sorry for the inconvient!\n\n" +
        			"Usage & Features:\n\n" +
        			"1. Quick translate : ( 2 method )\n" +
        			"a. By double click a word.\n" +
        			"some devices can't perform this method, try b method;\n" +
        			"b. By long press to select a word, then click \"Translate\" button. (need to be enabled first at \"settings\")\n" +
        			"All need network support.\n\n" +
        			"2. Quick Note :\n" +
        			"After you look up a word and translate it which will be automatically saved to a file ( also store at /sdcard/download ); " +
        			"then you can check what you note at \"Quick Note\" page.\n\n" +
        			"3. Video operation :\n" +
        			"a. Pause/resume by click central area of video view.\n" +
        			"b. Rewind by click left area (1/6 zone) of video.\n" +
					"c. Fast forward by click right area(5/6 zone) of video.\n" +
					"d. Swipe(slide on video) left/right to continuous rewind/fast forward.\n" +
					"e. Swipe up/down to zoom out/in.\n\n" +
        			"4. Script & video download :\n" +
        			"The script will be downloaded automatically; " +
        			"but video need to be enabled first at \"settings\".\n" +
        			"If the downloaded file exist at \"/sdcard/download\", the icon will be different obviously.\n" +
        			"You can perform offline jobs (without network) after downloading video & script files.\n\n" +
        			"5. Playing video at background :\n" +
        			"a. enabled when video is playing.\n" +
        			"b. key play/pause to start/stop.\n" +
        			"c. key previous/rewind to rewind.\n" +
        			"d. key next/fast forward to stop background service.\n" +
        			"e. back to foreground will also stop background service.\n" +
        			"PS : Background service will also comsume battery, remember to use d. or e. to stop it.\n\n" +
        			"6. Auto delete related file :\n" +
        			"Auto delete if modified timestamp of compared file > your setting value; to enable this feature at \"settings\" page, " +
        			"default is disable.\n\n" +
        			"*********************\n" +
        			"If you like this app or think it's useful, please help to rank it at Google Play, thanks~^^~\n" +
        			"*********************\n"
        			);
            break;
            
        case R.id.action_qa:
        	if (isDebug) {
        		Log.e("gray", "MainActivity.java:onOptionsItemSelected, case R.id.action_qa");
        	}
        	showAlertDialog("Q & A", 
        			"1. No update for a long time?\n" +
        			"It's depend on CNN Student News..\n" +
        			"The show is suspended when student is on vacation.\n\n" +
        			"Take a look at CNN Student News:\n" +
        			"http://edition.cnn.com/studentnews/\n" +
        			"and it's archive here:\n" +
        			"http://edition.cnn.com/US/studentnews/quick.guide/archive/\n\n" +
        			"2. When will the list update?\n" +
        			"We get video from here :\n" +
        			"http://rss.cnn.com/services/podcasting/studentnews/rss.xml\n" +
        			"When there is a new video here, we will update the list.\n" +
        			"If video is already on website, but the App's list still not be updated, please try it an hour later.\n\n" +
        			"3. How translation works?\n" +
        			"I just send a translated query to some website, and get the translated result to show, but I don't know if it's appropriate for your language(or none for your language);" +
					"If you have better or suggested website, just feel free to mail me.\n\n" +
        			"4. Any suggestion or bug just:\n" +
        			"mail to : llkkqq@gmail.com\n"
        			);
            break;
            
        // remove quick note list
        /*case R.id.action_notelist:
        	if (isDebug) {
        		Log.e("gray", "MainActivity.java:onOptionsItemSelected, case R.id.action_notelist");
        	}
            Intent intent = new Intent();
			intent.setClass(MainActivity.this, NoteListActivity.class);
			startActivityForResult(intent, 2);
            break;*/
        }
 
        return true;
    }
	
	@Override
	public void onDestroy() {
		
		if (isDebug) {
			Log.e("gray", "MainActivity.java: onDestroy");	
		}
		if (adView != null) {
			adView.destroy();
		}
		
		if (componentName != null ) {
			audioManager.unregisterMediaButtonEventReceiver(componentName);
			componentName = null;
		}
		
		super.onDestroy();
	}

}
