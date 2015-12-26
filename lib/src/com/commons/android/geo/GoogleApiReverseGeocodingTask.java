package com.commons.android.geo;

import java.net.URLEncoder;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;

import com.commons.android.BaseUtils;
import com.commons.android.Logg;
import com.commons.android.ResponseTuple;
import com.commons.android.SingletonApplicationBase;

public class GoogleApiReverseGeocodingTask extends AsyncTask<Void, Void, Void> {

  private String lang = Locale.getDefault().getLanguage();

  private SingletonApplicationBase app;
  
  private LocationTuple locTuple;
  
  private Runnable action;
  
  public GoogleApiReverseGeocodingTask( SingletonApplicationBase app, LocationTuple locTuple, Runnable action ) {
    this.app = app;
    this.locTuple = locTuple;
    this.action = action;
  }
  
  @Override
  protected Void doInBackground( Void... params ) {
    try{
      String latlng = URLEncoder.encode( BaseUtils.asString( locTuple.getLocation() ), "UTF-8" );
      ResponseTuple rt = app.doGet( "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latlng + "&language=" + lang, 3000, false );
      if( 200 != rt.getStatusCode() ) return null;
      JSONObject json = rt.getJson();
      if( null == json || !"ok".equalsIgnoreCase( json.optString( "status" ) ) ) return null;
      JSONArray array = json.getJSONArray( "results" );

      for( int ix = 0; ix < array.length(); ix++ ){
        JSONObject comp = (JSONObject)array.get( ix );
        String addr = DelayedGeocodeHandler.prettyAddress( comp );
        if( null == locTuple.getName() || locTuple.getName().length() < addr.length() ) locTuple.setName( addr );
      }
      
    }catch( Exception e ){
      Logg.e( this, "", e );
    }
    return null;
  }
  
  @Override
  protected void onPostExecute( Void result ) {
    if( null != action ) action.run();
  }

}
