package com.graylin.cnns;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
  
import android.app.Activity;  
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;	
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;  
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;  
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;  
import android.widget.Toast;

public class MainActivity extends Activity {
	 
	public static boolean isDebug = false;
//	public static boolean isDebug = true;

	public static boolean isLoadedToday = false;

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
	
	// HTML page
	public static final String CNNS_URL = "http://edition.cnn.com/US/studentnews/quick.guide/archive/";
	
    // XPath query
	public String XPATH_resultS = "";
	public String resultString = "";
	
	// AD
	public AdView adView;
	
	// ProgressDialog, wait network effort to be done
	public ProgressDialog mProgressDialog;
	
	// SharedPreferences instance
	public static SharedPreferences sharedPrefs;
	public static SharedPreferences.Editor sharedPrefsEditor;
	
	// setting var
	public static boolean isEnableDownload;
	public static int textSize;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (isDebug) {
			Log.e("gray", "MainActivity.java: START ===============");
		}
		
		// load AD
		adView = new AdView(this, AdSize.SMART_BANNER, "a151e4fa6d7cf0e");
		LinearLayout layout = (LinearLayout) findViewById(R.id.ADLayout);
		layout.addView(adView);
		adView.loadAd(new AdRequest());
		
		// get SharedPreferences instance
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		sharedPrefsEditor = sharedPrefs.edit();  
		
		// get initial data
		isEnableDownload = sharedPrefs.getBoolean("pref_download", false);
		isEnableDownload = true;
        textSize = Integer.valueOf(sharedPrefs.getString("pref_textSize", "18"));
        if (textSize < 8) {
        	textSize = 8;
		}
        if (textSize > 50){
        	textSize = 50;
        }
		
		// check if need to update, set isLoadedToday = true / false
		// get current date 
		SimpleDateFormat s = new SimpleDateFormat("ddMMyyyy");
		String currentDate = s.format(new Date());
		
		// get last update date
		String lastUpdateDate = sharedPrefs.getString("lastUpdateDate", "");

		if (currentDate.equalsIgnoreCase(lastUpdateDate)) {
			isLoadedToday = true;
		} else {
			isLoadedToday = false;
			sharedPrefsEditor.putString("lastUpdateDate", currentDate);
		}
		if (isDebug) {
			Log.e("gray", "MainActivity.java: currentDate: " + currentDate);
			Log.e("gray", "MainActivity.java: lastUpdateDate: " + lastUpdateDate);
			Log.e("gray", "MainActivity.java: isLoadedToday: " + isLoadedToday);
		}
		
		
		ConnectivityManager conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networInfo = conManager.getActiveNetworkInfo();
		
		// loaded earlier or no available network, get stored data
		if (isLoadedToday || networInfo == null || !networInfo.isAvailable()) {	
			
			if (networInfo == null || !networInfo.isAvailable()){
				Log.e("gray", "MainActivity.java, NO CONNECTIVITY_SERVICE");
				AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
		        dialog.setTitle("Alert Message");
		        dialog.setMessage("No Availiable Network!!");
		        dialog.show();
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
			
		// never loaded, get data from network
		} else {				
				
			final CharSequence strDialogTitle = "Please Wait...";
			final CharSequence strDialogBody = "Getting Data From CNN Student News...";
			mProgressDialog = ProgressDialog.show(MainActivity.this, strDialogTitle, strDialogBody, true);
			
			new Thread(new Runnable() 
			{ 
				@Override
				public void run() 
				{ 
					try {
						getCNNSTitle();
						
						// send message to show list view
						handler.sendEmptyMessage(0);
					} catch (Exception e) {
						Log.e("gray", "MainActivity.java:run, Exception e = " + e.toString());
						e.printStackTrace();
					}
				} 
			}).start();
		}
		
//		Button btn_go = (Button) findViewById(R.id.button_test);
//		btn_go.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				
//			}
//		});

         if (isDebug) {
			Log.e("gray", "MainActivity.java: END =================");
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (isDebug) {
			Log.e("gray", "PlayActivity.java: pref_download :" + sharedPrefs.getBoolean("pref_download", false) );
			Log.e("gray", "PlayActivity.java: pref_download :" + sharedPrefs.getString("pref_textSize", "") );
		}
		
        isEnableDownload = sharedPrefs.getBoolean("pref_download", false);
        		
        textSize = Integer.valueOf(sharedPrefs.getString("pref_textSize", ""));
        if (textSize < 8) {
        	textSize = 8;
		}
        if (textSize > 50){
        	textSize = 50;
        }
        switch (requestCode) {
		case 0:
			break;

		}
	}
	
	public void showListView() {
		
		// Get ListView object from res
	    listView = (ListView) findViewById(R.id.mainListView);
	    
	    // Define a new Adapter
	    // First parameter - Context
	    // Second parameter - Layout for the row
	    // Third parameter - ID of the TextView to which the data is written
	    // Forth - the Array of data
	    adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, cnnListStringArray);

	    // Assign adapter to ListView
	    listView.setAdapter(adapter); 
	    
	    // ListView Item Click Listener
	    listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
				
				Log.e("gray", "MainActivity.java:onItemClick" + "Position : " + position + ", id : " + id);
				String [] tempSA = new String [20];
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
				day = String.format("%02d", Integer.valueOf(tempSA[3]) + 1);
				
				scriptAddressString =  scriptAddressStringPrefix + year + tempSA[2] + "/" + day + scriptAddressStringPostfix;
				videoAddressString = videoAddressStringPrefix + tempSA[1] + "/" + tempSA[2] + "/" + tempSA[3] + "/sn-" + tempSA[2] + day + year + videoAddressStringPostfix; 

				if (isDebug) {
					Log.e("gray", "MainActivity.java:onItemClick, " + "scriptAddressString:" + scriptAddressString);
					Log.e("gray", "MainActivity.java:onItemClick, " + "vodeoAddressString:" + videoAddressString);
				}
			
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, PlayActivity.class);
				startActivity(intent);
			}
	    }); 
		
	}
	
	Handler handler = new Handler() {  
        @Override  
        public void handleMessage(Message msg) {
        	
        	showListView();
		    mProgressDialog.dismiss();
        }  
    };  
	
	public void getCNNSTitle() throws Exception {
	    String resultS = "";
	    String matchString = "CNN Student News Transcript -";
	
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
	    XPATH_resultS = "//div[@class='cnn_spccovt1cllnk cnn_spccovt1cll2']//h2//a";
	    Object[] resultSNode = root.evaluateXPath(XPATH_resultS);
	    // process data if found any node
	    
	    XPATH_resultS = "//div[@class='archive-item story cnn_skn_spccovstrylst']//h2//a";
	    resultSNode = root.evaluateXPath(XPATH_resultS);
	    // process data if found any node
	    if(resultSNode.length > 0) {
	    	
	    	if (isDebug) {
	    		Log.e("gray", "MainActivity.java:getCNNSTitle, " + "resultSNode.length > 0, resultSNode.length:" + resultSNode.length);
			}
	    	int arrayIndex = 0;
	    	for (int i = 0; arrayIndex < MAX_LIST_ARRAY_SIZE; i++) {
				
	    		TagNode resultNode = (TagNode)resultSNode[i];
	    		resultS = resultNode.getText().toString();
	    		
	    		if (resultS.regionMatches(0, matchString, 0, matchString.length())) {
					
	    			resultS = resultS.replace("CNN Student News Transcript -", ">>>> ");
	    			cnnListStringArray[arrayIndex] = resultS;
	    			cnnScriptAddrStringArray[arrayIndex] = resultNode.getAttributeByName("href");
	    			
	    			sharedPrefsEditor.putString("cnnListString_"+arrayIndex, cnnListStringArray[arrayIndex]);
	    			sharedPrefsEditor.putString("cnnScriptAddrString_"+arrayIndex, cnnScriptAddrStringArray[arrayIndex]);
	    			if (isDebug) {
	    				Log.e("gray", "MainActivity.java:getCNNSTitle, i:" + (i) + ", arrayIndex:" + arrayIndex + ", getAttributeByName = " + resultNode.getAttributeByName("href"));
					}
	    			
	    			arrayIndex++;
				} else {
					Log.e("gray", "MainActivity.java: string not match!!" );
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.e("gray", "MainActivity.java: onCreateOptionsMenu");
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
 
        case R.id.action_settings:
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, 0);
            break;
        }
 
        return true;
    }
	
	@Override
	public void onDestroy() {
		
		Log.e("gray", "MainActivity.java: onDestroy");	
		if (adView != null) {
			adView.destroy();
		}
		super.onDestroy();
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
	
}
