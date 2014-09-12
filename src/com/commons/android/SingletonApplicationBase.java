package com.commons.android;

import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.http.AndroidHttpClient;
import android.preference.PreferenceManager;

public abstract class SingletonApplicationBase extends Application {

  public boolean isInBackground = false;
  
  public int trayIcon;
  
  public int trayTextId;
  
  public int trayTitle;
  
  public String email;
  
  public String authToken;
  
  public String fullName;
  
  private HttpClient httpClient;
  
  protected SQLiteOpenHelper openHelper;

  public boolean loggedIn = false;

  @Override
  public void onCreate() {
    super.onCreate();
    try{ Class.forName( "android.os.AsyncTask" ); }catch( ClassNotFoundException e ){}
  }
  
  public String getId() {
    return null == authToken ? email : authToken;
  }

  public HttpClient getHttpClient() {
    if( null == httpClient ) httpClient = AndroidHttpClient.newInstance( "Android", this );
    return httpClient;
  }

  public int doPostStatus( String action, int timeout, List<NameValuePair> postParams ) throws Exception {
    return doPost( new HttpPost( action ), timeout, postParams ).getStatusCode();
  }

  public ResponseTuple doPost( HttpPost httppost, int timeout, List<NameValuePair> postParams ) throws Exception {
    HttpClient httpClient = getHttpClient();
    HttpConnectionParams.setConnectionTimeout( httpClient.getParams(), timeout );
    httppost.setEntity( new UrlEncodedFormEntity( postParams, HTTP.UTF_8 ) );
    HttpResponse hr = httpClient.execute( httppost );
    return new ResponseTuple( hr.getStatusLine().getStatusCode(), BaseUtils.asString( hr ) );
  }

  public ResponseTuple doPost( String url, int timeout, List<NameValuePair> postParams ) throws Exception {
    return doPost( new HttpPost( url ), timeout, postParams );
  }

  public ResponseTuple doGet( String url, int timeout ) throws Exception {
    HttpClient httpClient = getHttpClient();
    HttpConnectionParams.setConnectionTimeout( httpClient.getParams(), timeout );
    HttpGet httpget = new HttpGet( url );
    HttpResponse hr = httpClient.execute( httpget );
    return new ResponseTuple( hr.getStatusLine().getStatusCode(), BaseUtils.asString( hr ) );
  }

  public boolean showNotification( Intent i, boolean force, int trayId, String fromTray, long[] vibratePattern ) {
    if( !isInBackground && !force ) return false;
    TrayAttr trayAttr = prepareTrayAttr( i );
    BaseUtils.showNotification( this, i, trayAttr, trayId, fromTray, vibratePattern );
    return true;
  }

  public TrayAttr prepareTrayAttr( Intent i ) {
    TrayAttr trayAttr = new TrayAttr( trayIcon, trayTitle, trayTextId );
    if( 0 != i.getIntExtra( "trayIcon", 0 ) ) trayAttr.icon = i.getIntExtra( "trayIcon", 0 );
    if( 0 != i.getIntExtra( "trayTitle", 0 ) ) trayAttr.title = i.getIntExtra( "trayTitle", 0 );
    if( null != i.getStringExtra( "trayText" ) ) trayAttr.text = i.getStringExtra( "trayText" );
    else if( 0 != i.getIntExtra( "trayText", 0 ) ) trayAttr.textId = i.getIntExtra( "trayText", 0 );
    trayAttr.onGoing = i.getBooleanExtra( "trayOnGoing", true );
    return trayAttr;
  }

  public void showAlertMsg( Activity act, int key ) {
    showAlertMsg( act, key, false );
  }

  public void showAlertMsg( Activity act, int key, boolean finish ) {
    new AlertDialog.Builder( act )
    .setTitle( android.R.string.dialog_alert_title )
    .setMessage( key )
    .setCancelable( false )
    .setPositiveButton( android.R.string.ok, finish ? new FinishingListener( act ) : null )
    .create().show();
  }

  public void showAlertMsg( Activity act, String msg ) {
    showAlertMsg( act, msg, false );
  }

  public void showAlertMsg( Activity act, String msg, boolean finish ) {
    new AlertDialog.Builder( act )
    .setTitle( android.R.string.dialog_alert_title )
    .setMessage( msg )
    .setCancelable( false )
    .setPositiveButton( android.R.string.ok, finish ? new FinishingListener( act ) : null )
    .create().show();
  }

  public SQLiteDatabase getWritableDatabase() {
    return openHelper.getWritableDatabase();
  }

  public SQLiteDatabase getReadableDatabase() {
    return openHelper.getReadableDatabase();
  }

  public void clearAuthData() {
    PreferenceManager.getDefaultSharedPreferences( this ).edit()
          .remove( "accountName" )
          .remove( "authType" )
          .remove( "authToken" )
          .remove( "fullName" )
          .remove( "carInfo" )
          .remove( "avatar" )
          .commit();
    authToken = null;
    fullName = null;
    email = null;
  }

  public static class FinishingListener implements DialogInterface.OnClickListener {
    
    private Activity act;
    
    public FinishingListener( Activity act ) { this.act = act; }
    
    @Override public void onClick( DialogInterface dialog, int which ) { act.finish(); }
  }

}