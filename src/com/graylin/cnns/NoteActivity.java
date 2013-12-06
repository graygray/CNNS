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
		
		s = s.replaceAll("\n\n\n\n\n", "\n");
		s = s.replaceAll("\n\n\n\n", "\n");
		mTextView.setText(s+"\n\n");
		mTextView.setTextSize(MainActivity.textSize);
		
		switch (MainActivity.scriptTheme) {
		case 0:	// Black  -  White
			mTextView.setTextColor(0xff000000);
			mTextView.setBackgroundColor(0xffffffff);
			break;
		case 1:	// White  -  Black
			mTextView.setTextColor(0xffffffff);
			mTextView.setBackgroundColor(0xff000000);
			break;
		case 2: // Red - White
			mTextView.setTextColor(0xffDC143C);
			mTextView.setBackgroundColor(0xffffffff);
			break;
		case 3: // White  -  Red
			mTextView.setTextColor(0xffffffff);
			mTextView.setBackgroundColor(0xffA8050A);
			break;
		case 4: // Orange  -  Black
			mTextView.setTextColor(0xFFFFA500);
			mTextView.setBackgroundColor(0xff000000);
			break;
		case 5: // White  -  Orange
			mTextView.setTextColor(0xffffffff);
			mTextView.setBackgroundColor(0xFFFFA500);
			break;
		case 6: // Black  -  Orange
			mTextView.setTextColor(0xff000000);
			mTextView.setBackgroundColor(0xFFFFA500);
			break;
		case 7: // Black  -  Yellow
			mTextView.setTextColor(0xff000000);
			mTextView.setBackgroundColor(0xffFFF396);
			break;
		case 8: // Green - White
			mTextView.setTextColor(0xff00C22E);
			mTextView.setBackgroundColor(0xffffffff);
			break;
		case 9: // Green - Black
			mTextView.setTextColor(0xff00C22E);
			mTextView.setBackgroundColor(0xff000000);
			break;
		case 10: // White - Green
			mTextView.setTextColor(0xffffffff);
			mTextView.setBackgroundColor(0xff00C22E);
			break;
		case 11: // Black - Green
			mTextView.setTextColor(0xff000000);
			mTextView.setBackgroundColor(0xff00C22E);
			break;
		case 12: // LightBlue  -  White
			mTextView.setTextColor(0xFF4169E1);
			mTextView.setBackgroundColor(0xffffffff);
			break;
		case 13: // White - LightBlue
			mTextView.setTextColor(0xffffffff);
			mTextView.setBackgroundColor(0xFF4169E1);
			break;
		case 14: // Black - LightBlue
			mTextView.setTextColor(0xff000000);
			mTextView.setBackgroundColor(0xFF4169E1);
			break;
		case 15: // White  -  Blue
			mTextView.setTextColor(0xffffffff);
			mTextView.setBackgroundColor(0xff1038AA);
			break;
		case 16: // Pink  -  White
			mTextView.setTextColor(0xFFFF1493);
			mTextView.setBackgroundColor(0xffffffff);
			break;
		case 17: // LightPurple - White
			mTextView.setTextColor(0xffC71585);
			mTextView.setBackgroundColor(0xffffffff);
			break;
		case 18: // White - LightPurple
			mTextView.setTextColor(0xffffffff);
			mTextView.setBackgroundColor(0xffC71585);
			break;
		case 19: // White  -  Purple
			mTextView.setTextColor(0xffffffff);
			mTextView.setBackgroundColor(0xff4D0D2A);
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
