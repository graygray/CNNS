package com.graylin.cnns;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NoteActivity extends Activity {

	public TextView mTextView;
	public AdView adView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_note);
		
		try {
			setResultText( readFileAsString(Environment.getExternalStorageDirectory().getPath()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+ NoteListActivity.NoteFileName) );
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// load AD
	    adView = new AdView(this, AdSize.SMART_BANNER, "a151e4fa6d7cf0e");
		LinearLayout layout = (LinearLayout) findViewById(R.id.ADLayoutNote);
		layout.addView(adView);
		adView.loadAd(new AdRequest());
	}

	public String readFileAsString(String filePath) throws java.io.IOException
	{
	    BufferedReader reader = new BufferedReader(new FileReader(filePath));
	    String line, results = "";
	    while( ( line = reader.readLine() ) != null)
	    {
	        results += line;
	        results += "\n";
	    }
	    reader.close();
	    return results;
	}
	
	public void setResultText(String s){
		
		mTextView = (TextView) findViewById(R.id.tv_noteContent);
		
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
//		getMenuInflater().inflate(R.menu.note, menu);
//		return true;
//	}

}
