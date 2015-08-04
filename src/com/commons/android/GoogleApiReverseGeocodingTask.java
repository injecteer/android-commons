package com.commons.android;

import java.net.URLEncoder;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;

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
      ResponseTuple rt = app.doGet( "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latlng + "&language=" + lang, 3000 );
      if( 200 != rt.getStatusCode() ) return null;
      JSONObject json = rt.getJson();
      if( null == json || !"ok".equalsIgnoreCase( json.optString( "status" ) ) ) return null;
      JSONArray array = json.getJSONArray( "results" );
      String name = null, country = null, postalCode = null;

      for( int ix = 0; ix < array.length() && BaseUtils.anyEmpty( country, name, postalCode ); ix++ ){
        JSONObject comp = (JSONObject)array.get( ix );
        String types = comp.getString( "types" );
        
        if( BaseUtils.isEmpty( country ) && "[\"country\",\"political\"]".equals( types ) ){
          JSONArray addComps = comp.getJSONArray( "address_components" );
          for( int ixx = 0; ixx < addComps.length() && BaseUtils.anyEmpty( country ); ixx++ ){
            JSONObject addComp = addComps.getJSONObject( ixx );
            if( "[\"country\",\"political\"]".equals( addComp.getString( "types" ) ) ){
              locTuple.getLocation().setProvider( addComp.getString( "short_name" ) );
              country = addComp.getString( "long_name" );
            }
          }
          
        }else if( BaseUtils.isEmpty( postalCode ) && "[\"postal_code\"]".equals( types ) ){
          JSONArray addComps = comp.getJSONArray( "address_components" );
          for( int ixx = 0; ixx < addComps.length() && BaseUtils.anyEmpty( postalCode ); ixx++ ){
            JSONObject addComp = addComps.getJSONObject( ixx );
            if( "[\"postal_code\"]".equals( addComp.getString( "types" ) ) )
              postalCode = addComp.optString( "short_name" ) + ",?\\s?";
          }
        
        }else if( BaseUtils.anyEmpty( name ) && ( types.contains( "\"street_address\"" ) || types.contains( "\"route\"" ) ) )
          name = comp.getString( "formatted_address" );
      }
      
      if( BaseUtils.allNotEmpty( country, name ) ) name = name.substring( 0, name.lastIndexOf( country ) - 2 );
      if( BaseUtils.allNotEmpty( postalCode, name ) ) name = name.replaceFirst( postalCode, "" );
      locTuple.setName( name );
    }catch( Exception e ){
      Logg.e( "GoogleApiReverseGeocodingTask", "", e );
    }
    return null;
  }
  
  @Override
  protected void onPostExecute( Void result ) {
    if( null != action ) action.run();
  };

}
