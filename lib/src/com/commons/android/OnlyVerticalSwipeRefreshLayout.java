package com.commons.android;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import static android.view.MotionEvent.*;
import android.view.ViewConfiguration;

/**
 * Created by Injecteer on 11.12.2015.
 */
public class OnlyVerticalSwipeRefreshLayout extends SwipeRefreshLayout {

  private int touchSlop;
  private float prevX;
  private boolean declined;

  public OnlyVerticalSwipeRefreshLayout( Context context, AttributeSet attrs ) {
    super( context, attrs );
    touchSlop = ViewConfiguration.get( context ).getScaledTouchSlop();
  }

  @Override
  public boolean onInterceptTouchEvent( MotionEvent event ) {
    switch( event.getAction() ){
      case ACTION_DOWN:
        prevX = MotionEvent.obtain( event ).getX();
        declined = false; // New action
        break;

      case ACTION_MOVE:
        final float eventX = event.getX();
        float xDiff = Math.abs( eventX - prevX );
        if( declined || xDiff > touchSlop ){
          declined = true; // Memorize
          return false;
        }
        break;
    }
    return super.onInterceptTouchEvent( event );
  }
}
