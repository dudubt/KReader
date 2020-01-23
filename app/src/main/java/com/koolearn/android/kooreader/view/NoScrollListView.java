package com.koolearn.android.kooreader.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

/**
 * @author yuf
 * create at 2020-01-23(Thursday) 14:21
 * @version 1.0
 */
public class NoScrollListView extends ListView {
    
    public NoScrollListView(Context context) {
        super(context);
    }
    
    public NoScrollListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public NoScrollListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    
    public boolean dispatchTouchEvent(MotionEvent ev) {
//        if (ev.getAction() == MotionEvent.ACTION_MOVE) {
//            return true; // 禁止滑动
//        }
        return super.dispatchTouchEvent(ev);
        
    }
    
}
