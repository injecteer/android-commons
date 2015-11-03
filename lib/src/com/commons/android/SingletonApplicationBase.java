package com.commons.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.http.AndroidHttpClient;
import android.preference.PreferenceManager;

public abstract class SingletonApplicationBase extends Application {

  public boolean isInBackground = false;
  
  public long lastForegroundTransition = 0;
  
  protected HttpClient httpClient;
  
  protected SQLiteOpenHelper openHelper;

  public boolean loggedIn = false;

  public SharedPreferences prefs;

  @Override
  public void onCreate() {
    super.onCreate();
    try{ Class.forName( "android.os.AsyncTask" ); }catch( ClassNotFoundException e ){}
    prefs = PreferenceManager.getDefaultSharedPreferences( this );
    restoreUserData();
  }
  
  public void restoreUserData() {}  
  
  public HttpClient getHttpClient() {
    if( null == httpClient ){
      httpClient = AndroidHttpClient.newInstance( "Android", this );
      httpClient.getParams().setBooleanParameter( ClientPNames.HANDLE_REDIRECTS, true );
    }
    return httpClient;
  }

  public boolean isAuthenticated() {
    return false;
  }
  
  public int doPostStatus( String action, int timeout, List<NameValuePair> postParams ) throws Exception {
    return doPost( new HttpPost( action ), timeout, postParams ).getStatusCode();
  }

  public ResponseTuple doPost( HttpPost httppost, int timeout, List<NameValuePair> postParams, HttpClient... hc ) throws Exception {
    HttpClient httpClient = 1 == hc.length ? hc[ 0 ] : getHttpClient();
    HttpConnectionParams.setConnectionTimeout( httpClient.getParams(), timeout );
    httppost.setEntity( new UrlEncodedFormEntity( postParams, HTTP.UTF_8 ) );
    HttpResponse hr = httpClient.execute( httppost );
    return new ResponseTuple( hr.getStatusLine().getStatusCode(), BaseUtils.asString( hr ) );
  }

  public ResponseTuple doPost( String url, int timeout, List<NameValuePair> postParams, HttpClient... hc ) throws Exception {
    return doPost( new HttpPost( url ), timeout, postParams, hc );
  }
  
  public ResponseTuple doPostStreaming( String url, List<NameValuePair> postParams, int timeout ) throws ClientProtocolException, IOException {
    HttpClient httpClient = getHttpClient();
    HttpConnectionParams.setConnectionTimeout( httpClient.getParams(), timeout );
    HttpPost httppost = new HttpPost( url );
    httppost.setEntity( new UrlEncodedFormEntity( postParams, HTTP.UTF_8 ) );
    HttpResponse hr = httpClient.execute( httppost );
    return new ResponseTuple( hr.getStatusLine().getStatusCode(), hr.getEntity().getContent() );
  }
  
  public ResponseTuple doGet( String url, int timeout, HttpClient... hc ) throws Exception {
    HttpResponse hr = sendGetRequest( url, timeout );
    return new ResponseTuple( hr.getStatusLine().getStatusCode(), BaseUtils.asString( hr ), hr.getAllHeaders() );
  }
  
  public ResponseTuple doGetStreaming( String url, int timeout ) throws ClientProtocolException, IOException {
    HttpResponse hr = sendGetRequest( url, timeout );
    InputStream stream = 200 == hr.getStatusLine().getStatusCode() ? AndroidHttpClient.getUngzippedContent( hr.getEntity() ) : null;
    return new ResponseTuple( hr.getStatusLine().getStatusCode(), stream, hr.getAllHeaders() );
  }

  protected HttpResponse sendGetRequest( String url, int timeout, Header... headers ) throws IOException, ClientProtocolException {
    HttpClient httpClient = getHttpClient();
    HttpConnectionParams.setConnectionTimeout( httpClient.getParams(), timeout );
    HttpGet httpget = new HttpGet( url );
    AndroidHttpClient.modifyRequestToAcceptGzipResponse( httpget );
    for( Header h : headers ) httpget.addHeader( h );
    return httpClient.execute( httpget );
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