package com.graylin.cnns;

import java.net.URL;

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;  
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;  
import android.widget.LinearLayout;
import android.widget.ListView;  
import android.widget.Toast;

public class MainActivity extends Activity {
	 
//	public static boolean isDebug = false;
	public static boolean isDebug = true;

	public static final int MAX_LIST_ARRAY_SIZE = 20;
	
	public static String[] videoListStringArray = new String [MAX_LIST_ARRAY_SIZE];
	public static String[] scriptAddressStringArray = new String [MAX_LIST_ARRAY_SIZE];
	
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
	public static String XPATH_resultS = "";
	public static String resultString = "";
	
	// AD here
	public AdView adView;
	
	public ProgressDialog mProgressDialog;
	
	public static boolean isdone = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (isDebug) {
			Log.e("gray", "MainActivity.java: START ===============");
		}
	
		ConnectivityManager conManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo networInfo = conManager.getActiveNetworkInfo();  
		if (networInfo == null || !networInfo.isAvailable()){
			
			Log.e("gray", "MainActivity.java, NO CONNECTIVITY_SERVICE");
			AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
	        dialog.setTitle("Alert Message");
	        dialog.setMessage("No Availiable Network!!");
	        dialog.show();
	        
		}else{
			
			adView = new AdView(this, AdSize.BANNER, "a151e4fa6d7cf0e");
			LinearLayout layout = (LinearLayout) findViewById(R.id.ADLayout);
			layout.addView(adView);
			adView.loadAd(new AdRequest());
			
			if (! isdone) {
				
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
				        	handler.sendEmptyMessage(0);
						} catch (Exception e) {
							Log.e("gray", "MainActivity.java:run, Exception e = " + e.toString());
							e.printStackTrace();
						}
				   } 
				}).start();
			}
		}
		
		if (isDebug) {
			Log.e("gray", "MainActivity.java: END =================");
		}
	}

	Handler handler = new Handler() {  
        @Override  
        public void handleMessage(Message msg) {
        	
        	// Get ListView object from res
		    listView = (ListView) findViewById(R.id.mainListView);
		    
		    // Define a new Adapter
		    // First parameter - Context
		    // Second parameter - Layout for the row
		    // Third parameter - ID of the TextView to which the data is written
		    // Forth - the Array of data
		    adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, videoListStringArray);

		    // Assign adapter to ListView
		    listView.setAdapter(adapter); 
		    
		    // ListView Item Click Listener
		    listView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
					
					Log.e("gray", "MainActivity.java:onItemClick" + "Position : " + position + ", id : " + id);
					String [] tempSA = new String [20];
					tempSA = scriptAddressStringArray[position].split("/");
					
					if (isDebug) {
						for (int i = 0; i < tempSA.length; i++) {
							Log.e("gray", "MainActivity.java: " + tempSA[i]);
						}
					}
					
					int year, month;
					String day;
					
					year = Integer.valueOf(tempSA[1]) - 2000;
//					month = Integer.valueOf(tempSA[2]);
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
	    
//	    int dummy = 0;
//	    if(resultSNode.length > 0) {
//	    	
////	    	Log.e("gray", "MainActivity.java:getCNNSTitle, " + "resultSNode.length > 0, resultSNode.length:" + resultSNode.length);
//	    	for (int i = 0; i < resultSNode.length; i++) {
//				
//	    		TagNode resultNode = (TagNode)resultSNode[i];
//	    		resultS = resultNode.getText().toString();
//	    		
//	    		resultS = resultS.replace("CNN Student News Transcript -", ">>>>");
//	    		videoListStringArray[i + dummy] = resultS;
////	    		Log.e("gray", "MainActivity.java:getCNNSTitle, i = " + (i + dummy) + ", string = " + resultS);
//
//	    		scriptAddressStringArray[i + dummy] = resultNode.getAttributeByName("href");
//	    		Log.e("gray", "MainActivity.java:getCNNSTitle, i = " + (i + dummy) + ", getAttributeByName = " + resultNode.getAttributeByName("href"));
//			}
//	        
//	    } else {
//	    	Log.e("gray", "resultSNode.length <= 0, err!!");
//		}
//	    dummy += resultSNode.length;
//	    
//	    // query XPath
//	    XPATH_resultS = "//div[@class='cnn_mtt1imghtitle']//span//a";
//	    resultSNode = root.evaluateXPath(XPATH_resultS);
//	    // process data if found any node
//	    
//	    if(resultSNode.length > 0) {
//	    	
////	    	Log.e("gray", "MainActivity.java:getCNNSTitle, " + "resultSNode.length > 0, resultSNode.length:" + resultSNode.length);
//	    	for (int i = 0; i < resultSNode.length; i++) {
//				
//	    		TagNode resultNode = (TagNode)resultSNode[i];
//	    		resultS = resultNode.getText().toString();
//	    		
//	    		if (resultS.regionMatches(0, matchString, 0, 30)) {
//	    			
//	    			resultS = resultS.replace("CNN Student News Transcript -", ">>>>");
//	    			videoListStringArray[i + dummy] = resultS;
////	    			Log.e("gray", "MainActivity.java:getCNNSTitle, i = " + (i + dummy) + ", string = " + resultS);
//	    			
//	    			scriptAddressStringArray[i + dummy] = resultNode.getAttributeByName("href");
//	    			Log.e("gray", "MainActivity.java:getCNNSTitle, i = " + (i + dummy) + ", getAttributeByName = " + resultNode.getAttributeByName("href"));
//	    		} else {
//	    			Log.e("gray", "MainActivity.java: string not match!!" );
//				}
//	    		
//	    	}
//	        
//	    } else {
//	    	Log.e("gray", "resultSNode.length <= 0, err!!");
//		}
//	    dummy += resultSNode.length;
	    
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
					
	    			resultS = resultS.replace("CNN Student News Transcript -", ">>>>");
	    			videoListStringArray[arrayIndex] = resultS;
	    			
	    			scriptAddressStringArray[arrayIndex] = resultNode.getAttributeByName("href");
	    			if (isDebug) {
//		    			Log.e("gray", "MainActivity.java:getCNNSTitle, i = " + (i + dummy) + ", string = " + resultS);
	    				Log.e("gray", "MainActivity.java:getCNNSTitle, i:" + (i) + ", arrayIndex:" + arrayIndex + ", getAttributeByName = " + resultNode.getAttributeByName("href"));
					}
	    			
	    			arrayIndex++;
				} else {
					Log.e("gray", "MainActivity.java: string not match!!" );
				}
	    	}
	        
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
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onDestroy() {
		
		if (adView != null) {
			adView.destroy();
		}
		super.onDestroy();
	}

}
