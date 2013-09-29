package com.graylin.cnns;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.VideoView;

public class MyVideoView extends VideoView {

	private int mForceHeight = 0;
    private int mForceWidth = 0;
    public MyVideoView(Context context) {
        super(context);
    }

    public MyVideoView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setDimensions(int w, int h) {
        this.mForceHeight = h;
        this.mForceWidth = w;
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	
    	if (MainActivity.isDebug) {
    		Log.e("gray", "MyVideoView.java: onMeasure , width : " + mForceWidth);
    		Log.e("gray", "MyVideoView.java: onMeasure , height : " + mForceHeight);
		}
        setMeasuredDimension(mForceWidth, mForceHeight);
    }

}
