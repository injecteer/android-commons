package com.commons.android;

import android.util.Log;

public class Logg {

  public static boolean DEBUG = false;

  public static void i( Object thiz, String msg ) {
    if( DEBUG ) Log.i( tag( thiz ), msg );
  }

  public static void i( Object thiz, String msg, Throwable tr ) {
    if( DEBUG ) Log.i( tag( thiz ), msg, tr );
  }
  
  public static void w( Object thiz, String msg ) {
    if( DEBUG ) Log.w( tag( thiz ), msg );
  }
  
  public static void w( Object thiz, String msg, Throwable tr ) {
    if( DEBUG ) Log.w( tag( thiz ), msg, tr );
  }
  
  public static void e( Object thiz, String msg ) {
    if( DEBUG ) Log.e( tag( thiz ), msg );
  }
  
  public static void e( Object thiz, String msg, Throwable tr ) {
    if( DEBUG ) Log.e( tag( thiz ), msg, tr );
  }
  
  @SuppressWarnings("rawtypes")
  public static String tag( Object thiz ) {
    switch( thiz.getClass().getSimpleName() ){
      case "Class": return ((Class)thiz).getSimpleName();
      case "String": return (String)thiz;
      default: return thiz.getClass().getSimpleName();
    }
  }

}
