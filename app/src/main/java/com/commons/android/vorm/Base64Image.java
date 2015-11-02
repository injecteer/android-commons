package com.commons.android.vorm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import com.commons.android.BaseUtils;

public class Base64Image {

  public byte[] image;
  
  public Base64Image( String image ) {
    if( !BaseUtils.isEmpty( image ) ) this.image = Base64.decode( image, Base64.DEFAULT );
  }
  
  public Base64Image( InputStream content ) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buff = new byte[ 1024 ];
    while( 0 < content.read( buff ) ) baos.write( buff );
    image = baos.toByteArray();
    baos.close();
    content.close();
  }

  public Bitmap asBitmap() {
    return null == image ? null : BitmapFactory.decodeByteArray( image, 0, image.length );
  }
  
  public Drawable asDrawable( Resources res ){
    Bitmap bitmap = asBitmap();
    return null == bitmap ? null : new BitmapDrawable( res, bitmap );
  }

}
