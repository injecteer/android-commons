package com.commons.android.geo;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.commons.android.BaseUtils;
import com.commons.android.Logg;
import com.commons.android.ResponseTuple;
import com.commons.android.SingletonApplicationBase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class DelayedGeocodeHandler extends Handler {
  
  public static final int MESSAGE_TEXT_CHANGED = 0;
  
  public static final Map<String, String> COMPONENTS = new HashMap<>();
  
  static{
    COMPONENTS.put( "locality", "" );
    COMPONENTS.put( "sublocality", "" );
    COMPONENTS.put( "street_address", "" );
    COMPONENTS.put( "street_number", "" );
    COMPONENTS.put( "route", "" );
    COMPONENTS.put( "postal_code", "" );
    COMPONENTS.put( "country", "" );
  }
  
  private SingletonApplicationBase app;
  
  private BasicAutocompleteHelper helper;
  
  private View progressBar;

  private GoogleApiGeocodingTask task;

  public DelayedGeocodeHandler( SingletonApplicationBase app, BasicAutocompleteHelper helper, View progressBar ) {
    super();
    this.app = app;
    this.helper = helper;
    this.progressBar = progressBar;
  }

  @Override
  public void handleMessage( Message msg ) {
    if( msg.what != MESSAGE_TEXT_CHANGED ) return;
    String addr = (String)msg.obj;
    if( null != progressBar ) progressBar.setVisibility( View.VISIBLE );
    if( null != task && !task.isCancelled() ) task.cancel( true );
    task = new GoogleApiGeocodingTask();
    task.execute( addr );
  }

  public static String prettyAddress( JSONObject obj ) {
    JSONArray components = obj.optJSONArray( "address_components" );
    if( null == components ) return null;
    Map<String,String> m = new HashMap<>( COMPONENTS );
    for( int ixx = 0; ixx < components.length(); ixx++ ){
      JSONObject comp = (JSONObject)components.opt( ixx );
      String types = comp.optString( "types" );
      for( Entry<String, String> e : m.entrySet() ){
        if( types.contains( "\"" + e.getKey() + "\"" ) ) e.setValue( comp.optString( "long_name" ) );
      }
    }

    String addr = m.get( "street_address" ) + " " + m.get( "route" ) + " " + m.get( "street_number" );
    addr = addr.trim();
    String cityZip = m.get( "postal_code" );
    if( BaseUtils.isEmpty( addr ) ) cityZip += " " + m.get( "sublocality" );
    cityZip += " " + m.get( "locality" );
    cityZip = cityZip.trim();

    if( !BaseUtils.isEmpty( addr ) ) return addr + ", " + cityZip;
    else return cityZip + ", " + m.get( "country" );
  }

  class GoogleApiGeocodingTask extends AsyncTask<String, Void, JSONObject>{

    Locale defaultLoc = Locale.getDefault();
    
    long start = System.currentTimeMillis();
    
    GoogleApiGeocodingTask() {}

    @Override
    protected JSONObject doInBackground( String... params ) {
      try{
        String addr = URLEncoder.encode( params[ 0 ], "UTF-8" );
        String url = "http://maps.google.com/maps/api/geocode/json?sensor=true&address=" + addr + "&language=" + defaultLoc.getLanguage();
        ResponseTuple rt = app.doGet( url, 1000, false );
        Logg.i( this, "url=" + url + " code -> " + rt.getStatusCode() );
        if( 200 == rt.getStatusCode() ) return rt.getJson();
      }catch( Exception e ){
        Logg.e( this, "", e );
      }
      return null;
    }

    @Override
    protected void onPostExecute( JSONObject json ) {
      try{
        if( null == json || !"OK".equals( json.optString( "status" ) ) ) return;
        helper.initAdapter( true );
        
        Set<CharSequence> uniques = new HashSet<>();
        JSONArray array = json.getJSONArray( "results" ); 
        for( int ix = 0; ix < array.length(); ix++ ){
          JSONObject obj = array.optJSONObject( ix );
          JSONObject loc = obj.getJSONObject( "geometry" ).getJSONObject( "location" );
          Location l = new Location( "" );
          l.setLatitude( BaseUtils.FLOAT_FORMATTER.parse( loc.getString( "lat" ) ).doubleValue() );
          l.setLongitude( BaseUtils.FLOAT_FORMATTER.parse( loc.getString( "lng" ) ).doubleValue() ); 
          String addr = prettyAddress( obj );
          if( !BaseUtils.isEmpty( addr ) && uniques.add( addr ) ) helper.add( l, addr );
        }
        helper.show();
        Logg.i( this, "got " + uniques + " results in " + ( System.currentTimeMillis() - start ) + " ms" );
      }catch( Exception e ){
        Logg.e( this, "", e );
      }finally{
        if( null != progressBar ) progressBar.setVisibility( View.GONE );
      }
    }
  }
  
}