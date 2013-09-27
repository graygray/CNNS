package com.graylin.cnns;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
  
import android.app.Activity;  
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
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
	 
//	public static boolean isDebug = false;
	public static boolean isDebug = true;

	public static boolean isNeedUpdate = false;
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
	// ProgressDialog, wait network effort to be done
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
	public static int scriptTheme;
	public static String translateLanguage;
	public static boolean isEnableLongPressTranslate;
	public static boolean isEnableSoftButtonTranslate;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (isDebug) {
			Log.e("gray", "MainActivity.java: START ===============");
		}
		
		// init array
		for (int i = 0; i < MAX_LIST_ARRAY_SIZE; i++) {
			cnnListStringArray[i] = "initail string value";
			cnnScriptAddrStringArray[i] = "initail string value";
		}
		
		// get SharedPreferences instance
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		sharedPrefsEditor = sharedPrefs.edit();  
		
		// get initial data
		isEnableDownload = sharedPrefs.getBoolean("pref_download", false);
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
        
        scriptTheme = Integer.valueOf(sharedPrefs.getString("pref_script_theme", "0"));
        translateLanguage = sharedPrefs.getString("pref_translate_language", "zh-TW");
        isEnableLongPressTranslate = sharedPrefs.getBoolean("pref_longpress_translate", false);
        isEnableSoftButtonTranslate = sharedPrefs.getBoolean("pref_soft_button_translate", false);
		
		// check if need to update, set isNeedUpdate = true / false
		// get current date 
		SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd-KK", Locale.US);
		String currentDate = s.format(new Date());
		
		// get last update date
		String lastUpdateDate = sharedPrefs.getString("lastUpdateDate", "");

		if (currentDate.equalsIgnoreCase(lastUpdateDate)) {
			isNeedUpdate = false;
		} else {
			isNeedUpdate = true;
			sharedPrefsEditor.putString("lastUpdateDate", currentDate);
		}
		if (isDebug) {
			Log.e("gray", "MainActivity.java: currentDate: " + currentDate);
			Log.e("gray", "MainActivity.java: lastUpdateDate: " + lastUpdateDate);
			Log.e("gray", "MainActivity.java: isNeedUpdate: " + isNeedUpdate);
//			isNeedUpdate = true;
		}
		
		// check if need update (every hour check right now)
		if (isNeedUpdate) {	
			
			if (isNetworkAvailable()) {
				
				showProcessDialog("Please Wait...", "Getting Data From CNN Student News...");
				
				new Thread(new Runnable() 
				{ 
					@Override
					public void run() 
					{ 
						try {
							getCNNSTitle();
							handler.sendEmptyMessage(0);
						} catch (Exception e) {
							Log.e("gray", "MainActivity.java:run, Exception:" + e.toString());
							e.printStackTrace();
						}
					} 
				}).start();
				
			} else {
				
				if (lastUpdateDate.equalsIgnoreCase("")) {
					//never have cnns data
					showAlertDialog("Error", "Never get data from CNN student news!");
				} else {
					showListView();
				}
			}
			
		// no need to update, imply cnns data ever loaded
		} else {		
			
			for (int i = 0; i < MAX_LIST_ARRAY_SIZE; i++) {
				cnnListStringArray[i] = sharedPrefs.getString("cnnListString_"+i, "");
				cnnScriptAddrStringArray[i] = sharedPrefs.getString("cnnScriptAddrString_"+i, "");
				
				if (isDebug) {
					Log.e("gray", "MainActivity.java: cnnListStringArray[i]:" + cnnListStringArray[i]);
					Log.e("gray", "MainActivity.java: cnnScriptAddrStringArray[i]:" + cnnScriptAddrStringArray[i]);
				}
			}
			showListView();
		}
		
//		Button btn_go = (Button) findViewById(R.id.button_test);
//		btn_go.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				
//			}
//		});

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
			// back from settings page, set settings value to variable
			if (isDebug) {
				Log.e("gray", "PlayActivity.java: pref_download :" + sharedPrefs.getBoolean("pref_download", false) );
				Log.e("gray", "PlayActivity.java: pref_textSize :" + sharedPrefs.getString("pref_textSize", "18") );
				Log.e("gray", "PlayActivity.java: pref_script_theme :" + sharedPrefs.getString("pref_script_theme", "0") );
				Log.e("gray", "PlayActivity.java: pref_translate_language :" + sharedPrefs.getString("pref_translat_language", "zh-TW") );
			}
			
			isEnableDownload = sharedPrefs.getBoolean("pref_download", false);
			
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
			
			scriptTheme = Integer.valueOf(sharedPrefs.getString("pref_script_theme", "0"));

			translateLanguage = sharedPrefs.getString("pref_translate_language", "zh-TW");
					
			isEnableLongPressTranslate = sharedPrefs.getBoolean("pref_longpress_translate", false);
			
			isEnableSoftButtonTranslate = sharedPrefs.getBoolean("pref_soft_button_translate", false);
			
			break;
			
		case 1:
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
        	
        	String [] tempSA = new String [32];
        	String cnnVideoName = "";
    		tempSA = cnnScriptAddrStringArray[i].split("/");
			int year;
			String day;
			
			year = Integer.valueOf(tempSA[1]) - 2000;
			day = String.format(Locale.US, "%02d", Integer.valueOf(tempSA[3]) + 1);
			
    		// check if video file already download, or set path to local dir
    		cnnVideoName = "/sn-" + tempSA[2] + day + year + videoAddressStringPostfix;
    		if (isDebug) {
    			Log.e("gray", "path: " + Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName);
			}
    		
    		// put title
            map.put("ItemTitle", cnnListStringArray[i]);
            
            // put script image
            if (isFileExist(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName+".txt")) {
            	map.put("ItemImage_news", R.drawable.ic_newspaper_o);  
            } else {
            	map.put("ItemImage_news", R.drawable.ic_newspaper_x);  
			}
            
            // put video image
            if (isFileExist(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+cnnVideoName)) {
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
					for (int i = 0; i < tempSA.length; i++) {
						Log.e("gray", "MainActivity.java: " + tempSA[i]);
					}
				}
				
				int year;
//				int month;
				String day;
				
				year = Integer.valueOf(tempSA[1]) - 2000;
//				month = Integer.valueOf(tempSA[2]);
				day = String.format(Locale.US, "%02d", Integer.valueOf(tempSA[3]) + 1);
				
				scriptAddressString =  scriptAddressStringPrefix + year + tempSA[2] + "/" + day + scriptAddressStringPostfix;
				videoAddressString = videoAddressStringPrefix + tempSA[1] + "/" + tempSA[2] + "/" + tempSA[3] + "/sn-" + tempSA[2] + day + year + videoAddressStringPostfix; 

				if (isDebug) {
					Log.e("gray", "MainActivity.java:onItemClick, " + "scriptAddressString:" + scriptAddressString);
					Log.e("gray", "MainActivity.java:onItemClick, " + "vodeoAddressString:" + videoAddressString);
				}
			
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, PlayActivity.class);
				startActivityForResult(intent, 1);
			}
	    }); 
		
	}
	
	Handler handler = new Handler() {  
        @Override  
        public void handleMessage(Message msg) {
        	
			try {
				mProgressDialog.dismiss();
				mProgressDialog = null;
				showListView();
			} catch (Exception e) {
				// nothing
			}
        }  
    };  
	
	public void getCNNSTitle() throws Exception {
	    
		String resultS = "";
		String matchString = "CNN Student News";
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
        			"What's new in this version?\n\n" +
        			"slide left/right to rewind/forward video \n\n" +
        			"Usage:\n\n" +
        			"1. Quick translate : ( 3 method )\n" +
        			"    a. by double click a word. \n" +
        			"    some devices can't perform \"a\" method, try b or c; \n" +
        			"    method b / c need to be enabled first at \"settings\" page; \n" +
        			"    b. Long press to select a word, then click \"MENU\" key. \n" +
        			"    if your device don't have \"MENU\" key, try c. \n" +
        			"    c. Long press to select a word, then click \"Translate\" button. \n" +
        			"    all need network support.\n\n" +
        			"2. Script & video download status : \n" +
        			"If the downloaded file exist at \"/sdcard/download\", the icon will be different obviously.\n" +
        			"The script will be downloaded automatically but the video download need to be enabled at \"settings\" page.\n" +
        			"You can perform offline jobs (without network) after downloading video / script files.\n\n" +
        			"3. Click on video or click \"MENU\" key to suspend / resume video \n\n" +
        			"4. Quick Note : \n" +
        			"After you look up a word and translate it which will be automatically saved to a file ( also store at /sdcard/download ); " +
        			"then you can check what you note at \"Quick Note\" page.\n\n" +
        			"*********************\n" +
        			"If you like this app or think it's useful, please help to rank it at Google Play, thanks~^^\n" +
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
        			"and it's archive:\n" +
        			"http://edition.cnn.com/US/studentnews/quick.guide/archive/\n\n" +
        			"2. Any suggestion or bug just:\n" +
        			"mail to : llkkqq@gmail.com\n"
        			);
            break;
            
        case R.id.action_notelist:
        	if (isDebug) {
        		Log.e("gray", "MainActivity.java:onOptionsItemSelected, case R.id.action_notelist");
        	}
            Intent intent = new Intent();
			intent.setClass(MainActivity.this, NoteListActivity.class);
			startActivityForResult(intent, 2);
            break;
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
		super.onDestroy();
	}

}
