package com.commons.android;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class BaseUtils {

  protected static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols();
  
  public static final NumberFormat FLOAT_FORMATTER;
  
  static{
    DECIMAL_FORMAT_SYMBOLS.setDecimalSeparator( '.' );
    FLOAT_FORMATTER = new DecimalFormat( "###.######", DECIMAL_FORMAT_SYMBOLS );
  }
  
  public static String formatPrice( Number price, String currencyStr ) {
    try{
      Currency currency = Currency.getInstance( currencyStr );
      return formatPrice( price, currency );
    }catch( NullPointerException e ){
      return "";
    }
  }

  public static String formatPrice( Number price, Currency currency ) {
    NumberFormat nf = NumberFormat.getCurrencyInstance( Locale.getDefault() );
    nf.setCurrency( currency );
    nf.setMaximumFractionDigits( 0 );
    return nf.format( price );
  }

  public static Location parseLocation( String json ) {
    try{
      return parseLocation( new JSONObject( json ), null );
    }catch( JSONException e ){}
    return null;
  }

  public static Location parseLocation( JSONObject json, String name ) {
    try{
      JSONObject o = null == name ? json : json.getJSONObject( name );
      Location loc = new Location( o.optString( "name", "" ) );
      loc.setLatitude( o.getDouble( "lat" ) );
      loc.setLongitude( o.getDouble( "lon" ) );
      if( o.has( "speed" ) ) loc.setSpeed( o.getInt( "speed" ) );
      if( o.has( "bearing" ) ) loc.setBearing( (float)o.getDouble( "bearing" ) );
      return loc;
    }catch( Exception e ){
      return null;
    }
  }

  public static JSONObject asJSON( HttpResponse response ) throws Exception {
    return asJSON( response, false );
  }

  public static JSONObject asJSON( HttpResponse response, boolean log ) throws Exception {
    return new JSONObject( asString( response, log ) );
  }

  public static String asString( HttpResponse response ) throws Exception {
    return asString( response, false );
  }

  public static String asString( HttpResponse response, boolean log ) throws Exception {
    BufferedReader reader = new BufferedReader( new InputStreamReader( response.getEntity().getContent() ) );
    try{
      StringBuilder sb = new StringBuilder();
      String line = null;
      while( null != ( line = reader.readLine() ) ) sb.append( line );
      if( log ) Log.i( "Utils", "lenght = " + sb.length() );
      return sb.toString().trim();
    }finally{
      reader.close();
    }
  }

  /**
   * @return Application's version code from the {@code PackageManager}.
   */
  public static int getAppVersion( Context context ) {
    try{
      PackageInfo packageInfo = context.getPackageManager().getPackageInfo( context.getPackageName(), 0 );
      return packageInfo.versionCode;
    }catch( NameNotFoundException e ){
      // should never happen
      throw new RuntimeException("Could not get package name: " + e);
    }
  }

  public static void hideKeyboard( Activity activity, EditText et ) {
    InputMethodManager imm = (InputMethodManager)activity.getSystemService( Context.INPUT_METHOD_SERVICE );
    imm.hideSoftInputFromWindow( et.getWindowToken(), 0 );
  }

  public static void hideKeyboard( Activity activity, EditText[] ets ) {
    InputMethodManager imm = (InputMethodManager)activity.getSystemService( Context.INPUT_METHOD_SERVICE );
    for( EditText et : ets ) imm.hideSoftInputFromWindow( et.getWindowToken(), 0 );
  }

  public static void showNotification( Context ctx, Intent i, TrayAttr trayAttr, int trayId, String fromTray, long[] vibratePattern ) {
    NotificationManager nm = (NotificationManager)ctx.getSystemService( Context.NOTIFICATION_SERVICE );
    nm.cancel( trayId );
    Notification notification = getNotification( ctx, i, trayAttr, fromTray, vibratePattern );
    nm.notify( trayId, notification );
  }

  public static Notification getNotification( Context ctx, Intent notificationIntent, TrayAttr trayAttr, String fromTray, long[] vibratePattern ) {
    return getNotificationBuilder( ctx, notificationIntent, trayAttr, fromTray, vibratePattern ).build();
  }

  public static NotificationCompat.Builder getNotificationBuilder( Context ctx, Intent notificationIntent, TrayAttr trayAttr, String fromTray, long[] vibratePattern ) {
    notificationIntent.putExtra( fromTray, true );
    NotificationCompat.Builder builder = new NotificationCompat.Builder( ctx );
    builder.setSmallIcon( trayAttr.icon )
           .setPriority( NotificationCompat.PRIORITY_MAX )
           .setAutoCancel( true )
           .setOngoing( trayAttr.onGoing )
           .setContentTitle( ctx.getString( trayAttr.title ) );
    
    String txt = isEmpty( trayAttr.text ) ? ( 0 != trayAttr.textId ? ctx.getString( trayAttr.textId ) : null ) : trayAttr.text;
    if( null != txt ) builder.setContentText( txt );
    
    if( trayAttr.onGoing ) 
      builder.setProgress( 0, 0, true );
    else{ 
      builder.setLights( 0xFFFFCC00, 1500, 800 );
      if( trayAttr.sound )
        builder.setSound( RingtoneManager.getDefaultUri( RingtoneManager.TYPE_NOTIFICATION ) )
               .setVibrate( vibratePattern );
    }
    
    PendingIntent contentIntent = PendingIntent.getActivity( ctx, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT );
    builder.setContentIntent( contentIntent );
    return builder;
  }

  public static String[] convoluteStrings( String... strs ) {
    Map<String,Integer> freqs = new HashMap<String, Integer>();
    for( String s : strs ){
      for( String p : s.split( "\\s*,\\s*" ) ){
        int freq = freqs.containsKey( p ) ? freqs.get( p ) : 0;
        freqs.put( p, ++freq );
      }
    }
    String[] res = new String[ strs.length ];
    StringBuilder sb = new StringBuilder();
    int ix = 0;
    for( String s : strs ){
      sb.setLength( 0 );
      for( String p : s.split( "\\s*,\\s*" ) ){
        if( 1 == freqs.get( p ) ){
          if( 0 != sb.length() ) sb.append( ", " );
          sb.append( p );
        }
      }
      res[ ix ] = sb.toString();
      ix++;
    }
    
    return res;
  }

  public static Bitmap base64toBitmap( String s ) {
    byte[] avatar = Base64.decode( s, Base64.DEFAULT );
    return BitmapFactory.decodeByteArray( avatar, 0, avatar.length );
  }

  public static boolean isEmpty( String... ss ) {
    for( String s : ss ){
      if( null == s || s.isEmpty() || "null".equals( s ) ) return true;
    }
    return false;
  }

  public static void a2p( List<NameValuePair> res, String key, Object value ) {
    if( null == key || null == value ) return;
    String val = String.class.equals( value.getClass() ) ? (String)value : ( "" + value );
    if( !isEmpty( val ) ) res.add( new BasicNameValuePair( key, val ) );
  }

  public static String asString( Location loc ) {
    return FLOAT_FORMATTER.format( loc.getLatitude() ) + "," + FLOAT_FORMATTER.format( loc.getLongitude() );
  }

  public BaseUtils() {
    super();
  }

}