package com.commons.android.vorm.html;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

@SuppressWarnings("deprecation")
public class URLDrawable extends BitmapDrawable {
  
  public Drawable drawable;
  
  public URLDrawable( Context ctx, int id ) {
    if( 0 < id ) drawable = ctx.getResources().getDrawable( id );
  }

  @Override
  public void setBounds( int left, int top, int right, int bottom ) {
    super.setBounds( left, top, right, bottom );
    drawable.setBounds( left, top, right, bottom );
  }
  
  @Override
  public void draw( Canvas canvas ) {
    if( null != drawable ) drawable.draw( canvas );
  }
}
