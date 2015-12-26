package com.commons.android;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings.Secure;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class BaseUtils {

  protected static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols();
  
  public static final NumberFormat FLOAT_FORMATTER;
  
  public static final NumberFormat INT_FORMATTER;

  public static final Random rnd = new Random();

  public static final NumberFormat PRICE_FORMATTER = NumberFormat.getCurrencyInstance( Locale.getDefault() );

  public static int NOTIFICATION_LIGHTS;
  
  static{
    DECIMAL_FORMAT_SYMBOLS.setDecimalSeparator( '.' );
    FLOAT_FORMATTER = new DecimalFormat( "###.######", DECIMAL_FORMAT_SYMBOLS );
    INT_FORMATTER = new DecimalFormat();
    INT_FORMATTER.setMaximumFractionDigits( 0 );
    INT_FORMATTER.setGroupingUsed( false );
    PRICE_FORMATTER.setMaximumFractionDigits( 2 );
    PRICE_FORMATTER.setRoundingMode( RoundingMode.FLOOR );
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
    PRICE_FORMATTER.setCurrency( currency );
    return PRICE_FORMATTER.format( price );
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

  public static JSONObject asJSON( HttpURLConnection huc, boolean... log ) throws Exception {
    return new JSONObject( asString( huc, log ) );
  }

  public static String asString( HttpURLConnection huc, boolean... log ) throws Exception {
    if( null == huc || 200 != huc.getResponseCode() ) return null;
    BufferedReader reader = new BufferedReader( new InputStreamReader( getUngzippedInputStream( huc ) ) );
    try{
      StringBuilder sb = new StringBuilder();
      String line;
      while( null != ( line = reader.readLine() ) ) sb.append( line );
      if( 1 == log.length && log[ 0 ] ) Logg.i( BaseUtils.class, "lenght = " + sb.length() );
      return sb.toString().trim();
    }finally{
      reader.close();
    }
  }

  public static InputStream getUngzippedInputStream( HttpURLConnection huc ) throws IOException {
    if( null == huc || 200 != huc.getResponseCode() ) return null;
    InputStream responseStream = huc.getInputStream();
    if( responseStream == null ) return null;
    String encoding = huc.getContentEncoding();
    if( !isEmpty( encoding ) && encoding.contains( "gzip" ) )
      responseStream = new GZIPInputStream( responseStream );
    return responseStream;
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
      throw new RuntimeException( "Could not get package name: " + e );
    }
  }

  /**
   * @return device id
   */
  public static String getSerial( Context context ) {
    return Secure.getString( context.getContentResolver(), Secure.ANDROID_ID );
  }

  public static Set<String> getAppLanguages( Context ctx, int id ) {
    DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
    Configuration conf = ctx.getResources().getConfiguration();
    Locale originalLocale = conf.locale;
    conf.locale = Locale.ENGLISH;
    final String reference = new Resources( ctx.getAssets(), dm, conf ).getString( id );
    
    Set<String> result = new LinkedHashSet<>();
    result.add( Locale.ENGLISH.getLanguage() );
    
    for( Locale loc : Locale.getAvailableLocales() ){
      conf.locale = loc;
      if( !reference.equals( new Resources( ctx.getAssets(), dm, conf ).getString( id ) ) ) result.add( loc.getLanguage() );
    }
    conf.locale = originalLocale;
    return result; 
  }
  
  public static void hideKeyboard( Activity activity ) {
    InputMethodManager imm = (InputMethodManager)activity.getSystemService( Context.INPUT_METHOD_SERVICE );
    View f = activity.getCurrentFocus();
    if( null != f && null != f.getWindowToken() && EditText.class.isAssignableFrom( f.getClass() ) )
      imm.hideSoftInputFromWindow( f.getWindowToken(), 0 );
    else 
      activity.getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN );
  }

  public static void cancelNotification( Context ctx, int trayId ) {
    NotificationManager nm = (NotificationManager)ctx.getSystemService( Context.NOTIFICATION_SERVICE );
    nm.cancel( trayId );
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

  public static Builder getNotificationBuilder( Context ctx, Intent notificationIntent, TrayAttr trayAttr, String fromTray, long[] vibratePattern ) {
    notificationIntent.putExtra( fromTray, true );
    Builder builder = new Builder( ctx );
    builder.setSmallIcon( trayAttr.icon )
           .setPriority( NotificationCompat.PRIORITY_MAX )
           .setAutoCancel( true )
           .setOngoing( trayAttr.onGoing )
           .setContentTitle( !isEmpty( trayAttr.titleString ) ? trayAttr.titleString : ctx.getString( trayAttr.title ) );
    
    String txt = isEmpty( trayAttr.text ) ? ( 0 != trayAttr.textId ? ctx.getString( trayAttr.textId ) : null ) : trayAttr.text;
    if( null != txt ) builder.setContentText( txt );
    if( 0 != trayAttr.number ) builder.setNumber( trayAttr.number ); 
    
    if( trayAttr.onGoing ) 
      builder.setProgress( 0, 0, true );
    else{ 
      builder.setLights( NOTIFICATION_LIGHTS, 1500, 800 );
      if( null != vibratePattern ) builder.setVibrate( vibratePattern );
      if( trayAttr.sound ){
        Uri uri = null == trayAttr.soundId ? RingtoneManager.getDefaultUri( RingtoneManager.TYPE_NOTIFICATION ) : 
                                             Uri.parse( "android.resource://" + ctx.getPackageName() + "/" + trayAttr.soundId );
        builder.setSound( uri );
      }
    }
    
    PendingIntent contentIntent = PendingIntent.getActivity( ctx, rnd .nextInt(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT );
    builder.setContentIntent( contentIntent );
    return builder;
  }

  public static String[] convoluteStrings( String... strs ) {
    Map<String,Integer> freqs = new HashMap<>();
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

  public static String halfLinebreak( String v, int limit, String... lb ) {
    if( v.length() <= limit ) return v;
    String lineBreak = 1 == lb.length ? lb[ 0 ] : "\n";
    StringBuilder sb = new StringBuilder();
    String[] split = v.split( ", " );
    v = split[ 0 ] + ", " + split[ split.length - 1 ];
    if( v.length() <= limit ) return v;
    int mid = v.length() >> 1;
    for( String s:new String[]{ split[ 0 ], split[ split.length - 1 ] } ){
      int lenBefore = sb.length();
      sb.append( s ).append( ", " );
      if( lenBefore < mid && sb.length() >= mid ) sb.append( lineBreak );
    }
    String res = sb.toString().trim();
    return res.endsWith( "," ) ? res.substring( 0, res.length() - 1 ) : res;
  }
  
  public static Bitmap base64toBitmap( String s ) {
    byte[] avatar = Base64.decode( s, Base64.DEFAULT );
    return BitmapFactory.decodeByteArray( avatar, 0, avatar.length );
  }

  public static boolean anyEmpty( String... ss ) {
    boolean res = false;
    for( String s : ss ) res |= null == s || s.isEmpty() || "null".equals( s );
    return res;
  }
  
  public static boolean isEmpty( String... ss ) {
    for( String s : ss ){
      if( null == s || s.isEmpty() || "null".equals( s ) ) return true;
    }
    return false;
  }
  
  public static boolean isEmpty( CharSequence... ss ) {
    for( CharSequence s : ss ){
      if( null == s || 0 == s.length() || "null".equals( s.toString() ) ) return true;
    }
    return false;
  }
  
  public static boolean anyNotEmpty( String... ss ) {
    for( String s : ss ){
      if( !( null == s || s.isEmpty() || "null".equals( s ) ) ) return true;
    }
    return false;
  }
  
  public static boolean allNotEmpty( String... ss ) {
    for( String s : ss ){
      if( null == s || s.isEmpty() || "null".equals( s ) ) return false;
    }
    return true;
  }

  public static void a2p( List<NameValuePair> res, String key, Object value ) {
    if( null == key || null == value ) return;
    String val;
    if( Number.class.isAssignableFrom( value.getClass() ) ){
      Number n = (Number)value;
      val = n.floatValue() == n.intValue() ? INT_FORMATTER.format( n ) : FLOAT_FORMATTER.format( n );
    }else
      val = String.class.equals( value.getClass() ) ? (String)value : value.toString();
    if( !isEmpty( val ) ) res.add( new NameValuePair( key, val ) );
  }

  public static String toQuery( List<NameValuePair> params ) {
    Uri.Builder b = new Uri.Builder();
    for( NameValuePair p : params ) b.appendQueryParameter( p.getName(), p.getValue() );
    return b.build().getEncodedQuery();
  }


  public static String asString( Location loc ) {
    return FLOAT_FORMATTER.format( loc.getLatitude() ) + "," + FLOAT_FORMATTER.format( loc.getLongitude() );
  }

  public static String asStringWithBearing( Location loc ) {
    return asString( loc ) + "," + INT_FORMATTER.format( loc.getSpeed() ) + "," + INT_FORMATTER.format( loc.getBearing() );
  }
  
  public static Location locationFromString( String s ) {
    if( anyEmpty( s ) ) return null;
    String[] split = s.split( "," );
    if( 2 > split.length ) return null;
    Location loc = new Location( "" );
    try{
      loc.setLatitude( (double)FLOAT_FORMATTER.parse( split[ 0 ] ) );
      loc.setLongitude( (double)FLOAT_FORMATTER.parse( split[ 1 ] ) );
      return loc;
    }catch( ParseException e ){
    }
    return null;
  }

  public static Date clearTime( Date... d ) {
    Calendar c = Calendar.getInstance();
    if( 0 < d.length && null != d[ 0 ] ) c.setTime( d[ 0 ] );
    c.set( Calendar.HOUR_OF_DAY, 0 );
    c.set( Calendar.MINUTE, 0 );
    c.set( Calendar.SECOND, 0 );
    return c.getTime();
  }

  public static Bitmap decodeBitmapFromFile( String filename ) {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFile( filename, options );
  
    options.inJustDecodeBounds = false;
    return BitmapFactory.decodeFile( filename, options );
  }

  public static Bitmap decodeBitmapFromDescriptor( FileDescriptor fileDescriptor ) {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeFileDescriptor( fileDescriptor, null, options );
  
    options.inJustDecodeBounds = false;
    return BitmapFactory.decodeFileDescriptor( fileDescriptor, null, options );
  }

  @NonNull
  public static void setImageSpannable( View v, int imageId, int textId, float... sizeFactor ) {
    TextView tv = (TextView)v;
    int iconSize = (int)( tv.getTextSize() * ( 0 == sizeFactor.length ? 1.15 : sizeFactor[ 0 ] ) );
    tv.setText( getImageSpannable( v.getContext(), imageId, textId, iconSize ) );
  }

  @NonNull
  public static void setImageSpannable( View v, int imageId, CharSequence text, float... sizeFactor ) {
    TextView tv = (TextView)v;
    int iconSize = (int)( tv.getTextSize() * ( 0 == sizeFactor.length ? 1.15 : sizeFactor[ 0 ] ) );
    tv.setText( getImageSpannable( v.getContext(), imageId, text, iconSize ) );
  }

  @NonNull
  public static CharSequence getImageSpannable( Context ctx, int imageId, int textId, int size ) {
    if( null == ctx || 0 >= imageId || 0 >= textId ) return "";
    SpannableStringBuilder sb = new SpannableStringBuilder( "  " );
    Drawable d = ctx.getResources().getDrawable( imageId );
    d.setBounds( 0, 0, size, size );
    sb.setSpan( new ImageSpan( d ), sb.length() - 1, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
    sb.append( " " ).append( ctx.getResources().getText( textId ) );
    return sb;
  }

  @NonNull
  public static CharSequence getImageSpannable( Context ctx, int imageId, CharSequence text, int size ) {
    if( null == ctx || 0 >= imageId ) return "";
    SpannableStringBuilder sb = new SpannableStringBuilder( "  " );
    Drawable d = ctx.getResources().getDrawable( imageId );
    d.setBounds( 0, 0, size, size );
    sb.setSpan( new ImageSpan( d ), sb.length() - 1, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
    if( !isEmpty( text ) ) sb.append( " " ).append( text );
    return sb;
  }
}