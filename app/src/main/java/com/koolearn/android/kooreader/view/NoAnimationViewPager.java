package com.koolearn.android.kooreader.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * @author yuf
 * create at 2020-01-23(Thursday) 11:34
 * @version 1.0
 */
public class NoAnimationViewPager extends ViewPager {
    
    public NoAnimationViewPager(Context context) {
        super(context);
    }
    
    public NoAnimationViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
    
    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        super.setCurrentItem(item, smoothScroll);
    }
    
    @Override
    public void setCurrentItem(int item) {
        //去除页面切换时的滑动翻页效果
        super.setCurrentItem(item, false);
    }
    
}
