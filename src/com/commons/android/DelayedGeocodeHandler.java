package com.commons.android;

import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ProgressBar;

public class DelayedGeocodeHandler extends Handler {
  
  public static final int MESSAGE_TEXT_CHANGED = 0;
  
  private SingletonApplicationBase app;
  
  private BasicAutocompleteHelper helper;
  
  private ProgressBar progressBar;
  
  public DelayedGeocodeHandler( SingletonApplicationBase app, BasicAutocompleteHelper helper, ProgressBar progressBar ) {
    super();
    this.app = app;
    this.helper = helper;
    this.progressBar = progressBar;
  }
  
  @Override
  public void handleMessage( Message msg ) {
    if( msg.what != MESSAGE_TEXT_CHANGED ) return;
    String addr = (String)msg.obj;
    progressBar.setVisibility( View.VISIBLE );
    new GoogleApiGeocodingTask().execute( addr );
  }

  class GoogleApiGeocodingTask extends AsyncTask<String, Void, JSONObject>{

    Locale defaultLoc = Locale.getDefault();
    
    Set<String> uniques = new HashSet<String>();
    
    long start = System.currentTimeMillis();
    
    GoogleApiGeocodingTask() {}

    @Override
    protected JSONObject doInBackground( String... params ) {
      try{
        String addr = URLEncoder.encode( params[ 0 ], "UTF-8" );
        ResponseTuple rt = app.doGet( "http://maps.google.com/maps/api/geocode/json?sensor=true&address=" + addr + "&language=" + defaultLoc.getLanguage(), 3000 );
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
        
        JSONArray array = json.getJSONArray( "results" ); 
        for( int ix = 0; ix < array.length(); ix++ ){
          JSONObject obj = array.optJSONObject( ix );
          String addr = obj.getString( "formatted_address" );
          if( !uniques.add( addr ) ) continue;
          JSONObject loc = obj.getJSONObject( "geometry" ).getJSONObject( "location" );
          Location l = new Location( "" );
          l.setLatitude( BaseUtils.FLOAT_FORMATTER.parse( loc.getString( "lat" ) ).doubleValue() );
          l.setLongitude( BaseUtils.FLOAT_FORMATTER.parse( loc.getString( "lng" ) ).doubleValue() ); 
          JSONArray components = obj.getJSONArray( "address_components" );
          String country = null;
          for( int ixx = 0; ixx < components.length(); ixx++ ){
            JSONObject comp = (JSONObject)components.get( ixx );
            String types = comp.getString( "types" );
            if( -1 != types.indexOf( "\"country\"" ) ) country = comp.getString( "short_name" );
          }
          l.setProvider( country );
          helper.add( l, addr );
        }
//        Logg.e( this, "got " + uniques + " results in " + ( System.currentTimeMillis() - start ) + " ms" );
      }catch( Exception e ){
        Logg.e( this, "", e );
      }finally{
        progressBar.setVisibility( View.GONE );
      }
    }
  }
  
}