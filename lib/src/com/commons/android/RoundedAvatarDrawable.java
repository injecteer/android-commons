package com.commons.android;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public class RoundedAvatarDrawable extends Drawable {
  
  private final Bitmap bitmap;
  
  private final Paint paint, borderPaint;
  
  private final RectF rectF;
  
  private final int diameter;

  public RoundedAvatarDrawable( Bitmap bitmap, int color, float border ) {
    this.bitmap = bitmap;
    
    borderPaint = new Paint();
    borderPaint.setAntiAlias( true );
    borderPaint.setColor( color );
    borderPaint.setStyle( Paint.Style.STROKE );
    borderPaint.setStrokeWidth( border );
    
    paint = new Paint();
    paint.setAntiAlias( true );
    paint.setDither( true );
    rectF = new RectF();
    paint.setShader( new BitmapShader( bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP ) );

    diameter = bitmap.getWidth();
  }

  @Override
  public void draw( Canvas canvas ) {
    canvas.drawOval( rectF, paint );
    float radius = diameter / 2;
    float d = borderPaint.getStrokeWidth() / 2;
    canvas.drawCircle( radius, radius, radius - d, borderPaint );
  }

  @Override
  protected void onBoundsChange( Rect bounds ) {
    super.onBoundsChange( bounds );
    rectF.set( bounds );
  }

  @Override
  public void setAlpha( int alpha ) {
    if( paint.getAlpha() != alpha ){
      paint.setAlpha( alpha );
      invalidateSelf();
    }
  }

  @Override
  public void setColorFilter( ColorFilter cf ) {
    paint.setColorFilter( cf );
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  @Override
  public int getIntrinsicWidth() {
    return diameter;
  }

  @Override
  public int getIntrinsicHeight() {
    return diameter;
  }

  public void setAntiAlias( boolean aa ) {
    paint.setAntiAlias( aa );
    invalidateSelf();
  }

  @Override
  public void setFilterBitmap( boolean filter ) {
    paint.setFilterBitmap( filter );
    invalidateSelf();
  }

  @Override
  public void setDither( boolean dither ) {
    paint.setDither( dither );
    invalidateSelf();
  }

  public Bitmap getBitmap() {
    return bitmap;
  }
}
