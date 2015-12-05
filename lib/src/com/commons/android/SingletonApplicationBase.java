package com.commons.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public abstract class SingletonApplicationBase extends Application {

  public boolean isInBackground = false;
  
  public long lastForegroundTransition = 0;
  
  protected SQLiteOpenHelper openHelper;

  public boolean loggedIn = false;

  public SharedPreferences prefs;

  @Override
  public void onCreate() {
    super.onCreate();
    try{ Class.forName( "android.os.AsyncTask" ); }catch( ClassNotFoundException ignored ){}
    prefs = PreferenceManager.getDefaultSharedPreferences( this );
    restoreUserData();
  }
  
  public void restoreUserData() {}  
  
  public boolean isAuthenticated() {
    return false;
  }
  
  public int doPostStatus( String url, int timeout, List<NameValuePair> postParams ) throws Exception {
    HttpURLConnection huc = openConnection( url );
    huc.setRequestMethod( "POST" );
    huc.setConnectTimeout( timeout );
    huc.setDoOutput( false );
    huc.setDoInput( true );

    BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( huc.getOutputStream(), "UTF-8" ) );
    writer.write( BaseUtils.toQuery( postParams ) );
    writer.close();
    try{
      huc.connect();
      return huc.getResponseCode();
    }finally{
      huc.disconnect();
    }
  }

  public ResponseTuple doPost( String url, int timeout, List<NameValuePair> postParams ) throws Exception {
    HttpURLConnection huc = openConnection( url );
    huc.setRequestMethod( "POST" );
    huc.setConnectTimeout( timeout );
    huc.setDoOutput( true );
    huc.setDoInput( true );

    BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( huc.getOutputStream(), "UTF-8" ) );
    writer.write( BaseUtils.toQuery( postParams ) );
    writer.close();

    try{
      huc.connect();
      return new ResponseTuple( huc.getResponseCode(), BaseUtils.asString( huc ) );
    }finally{
      huc.disconnect();
    }
  }

  protected HttpURLConnection openConnection( String url ) throws IOException {
    return (HttpURLConnection)new URL( url ).openConnection();
  }

  public ResponseTuple doPostStreaming( String url, List<NameValuePair> postParams, int timeout ) throws IOException {
    HttpURLConnection huc = openConnection( url );
    huc.setRequestMethod( "POST" );
    huc.setConnectTimeout( timeout );
    huc.setDoOutput( true );
    huc.setDoInput( true );

    BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( huc.getOutputStream(), "UTF-8" ) );
    writer.write( BaseUtils.toQuery( postParams ) );
    writer.close();

    huc.connect();

    return new ResponseTuple( huc.getResponseCode(), BaseUtils.getUngzippedInputStream( huc ) );
  }

  public ResponseTuple doGet( String url, int timeout ) throws Exception {
    HttpURLConnection huc = sendGetRequest( url, timeout );
    try{
      return new ResponseTuple( huc.getResponseCode(), BaseUtils.asString( huc ), huc.getHeaderFields() );
    }finally{
      huc.disconnect();
    }
  }

  public ResponseTuple doGetStreaming( String url, int timeout ) throws IOException {
    HttpURLConnection huc = sendGetRequest( url, timeout );
    return new ResponseTuple( huc.getResponseCode(), BaseUtils.getUngzippedInputStream( huc ), huc.getHeaderFields() );
  }

  protected HttpURLConnection sendGetRequest( String url, int timeout, NameValuePair... headers ) throws IOException {
    HttpURLConnection huc = openConnection( url );
    huc.setRequestMethod( "GET" );
    huc.setConnectTimeout( timeout );
    huc.setDoOutput( true );
    huc.setRequestProperty( "Accept-Encoding", "gzip" );
    for( NameValuePair h : headers ) huc.setRequestProperty( h.getName(), h.getValue() );
    huc.connect();
    return huc;
  }
  
  public boolean showNotification( Intent i, TrayAttr trayAttr, boolean force, int trayId, String fromTray, long[] vibratePattern ) {
    if( !isInBackground && !force ) return false;
    BaseUtils.showNotification( this, i, trayAttr, trayId, fromTray, vibratePattern );
    return true;
  }

  public void showAlertMsg( Activity act, int key, boolean... finish ) {
    new AlertDialog.Builder( act )
      .setTitle( android.R.string.dialog_alert_title )
      .setMessage( key )
      .setCancelable( false )
      .setPositiveButton( android.R.string.ok, 1 == finish.length && finish[ 0 ] ? new FinishingListener( act ) : null )
      .create().show();
  }

  public void showAlertMsg( Activity act, String msg, boolean... finish ) {
    new AlertDialog.Builder( act )
      .setTitle( android.R.string.dialog_alert_title )
      .setMessage( msg )
      .setCancelable( false )
      .setPositiveButton( android.R.string.ok, 1 == finish.length && finish[ 0 ] ? new FinishingListener( act ) : null )
      .create().show();
  }

  public SQLiteDatabase getWritableDatabase() {
    return openHelper.getWritableDatabase();
  }

  public SQLiteDatabase getReadableDatabase() {
    return openHelper.getReadableDatabase();
  }

  public void clearAuthData() {
    prefs.edit().clear().commit();
  }

  public static class FinishingListener implements DialogInterface.OnClickListener {
    
    private Activity act;
    
    public FinishingListener( Activity act ) { this.act = act; }
    
    @Override public void onClick( DialogInterface dialog, int which ) { act.finish(); }
  }

}