package com.commons.android;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Injecteer on 11.12.2015.
 */
public class NotSwipeableViewPager extends ViewPager {

  public NotSwipeableViewPager( Context context ) {
    super( context );
  }

  public NotSwipeableViewPager( Context context, AttributeSet attrs ) {
    super( context, attrs );
  }

  @Override
  public boolean onInterceptTouchEvent( MotionEvent ev ) {
    return false;
  }

  @Override
  public boolean onTouchEvent( MotionEvent ev ) {
    return false;
  }
}
