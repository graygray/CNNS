package com.graylin.cnns;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

public class NoteListActivity extends Activity {

	public static String NoteFileName;
	public AdView adView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note_list);
		
		ListView lv;
	    final ArrayList<String> FilesInFolder = GetFiles(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS);
	    Collections.reverse(FilesInFolder);
	    
	    lv = (ListView)findViewById(R.id.noteListView);

	    lv.setAdapter(new ArrayAdapter<String>(this,
	        android.R.layout.simple_list_item_1, FilesInFolder));

	    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
				if (MainActivity.isDebug) {
					Log.e("gray", "MainActivity.java:onItemClick" + "Position : " + position + ", id : " + id);
					Log.e("gray", "MainActivity.java:onItemClick" + "FilesInFolder : " + position + " = " + FilesInFolder.get(position));
				}
				NoteFileName = FilesInFolder.get(position);
				
				Intent intent = new Intent();
				intent.setClass(NoteListActivity.this, NoteActivity.class);
				startActivityForResult(intent, 0);
			}
	    });
	    
		// load AD
	    adView = new AdView(this, AdSize.SMART_BANNER, "a151e4fa6d7cf0e");
		LinearLayout layout = (LinearLayout) findViewById(R.id.ADLayoutNoteList);
		layout.addView(adView);
		adView.loadAd(new AdRequest());
	}

	// get note file
	public ArrayList<String> GetFiles(String DirectoryPath) {
	    ArrayList<String> MyFiles = new ArrayList<String>();
	    File f = new File(DirectoryPath);

	    f.mkdirs();
	    File[] files = f.listFiles();
	    if (files.length == 0){
	        return null;
	    } else {
	    	int noteNumber = 0;
	        for (int i=0; i<files.length; i++){
	        	if (files[i].getName().contains("cnnsNote")) {
	        		MyFiles.add(files[i].getName());
	        		noteNumber++;
	        	}
	        }
	        
	        if (noteNumber == 0) {
	        	showAlertDialog("Information", "You don't have any note!!");
			}
	    }
	    return MyFiles;
	}
	
	public void showAlertDialog(String title, String message) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(NoteListActivity.this);
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.show();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (MainActivity.isDebug) {
			Log.e("gray", "MainActivity.java: onActivityResult, requestCode: " + requestCode);
			Log.e("gray", "MainActivity.java: onActivityResult, resultCode: " + resultCode);
		}
		
		// reload AD
		adView.loadAd(new AdRequest());
		
        switch (requestCode) {
		case 0:
	
			break;

		}
	}
	
	@Override
	public void onDestroy() {
		
		if (MainActivity.isDebug) {
			Log.e("gray", "MainActivity.java: onDestroy");	
		}
		if (adView != null) {
			adView.destroy();
		}
		super.onDestroy();
	}
	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.note_list, menu);
//		return true;
//	}
}
