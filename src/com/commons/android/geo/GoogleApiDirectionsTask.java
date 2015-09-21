package com.commons.android.geo;

import java.util.List;

import org.json.JSONObject;

import android.location.Location;
import android.os.AsyncTask;

import com.commons.android.BaseUtils;
import com.commons.android.Logg;
import com.commons.android.ResponseTuple;
import com.commons.android.SingletonApplicationBase;
import com.google.android.gms.maps.model.LatLng;

public class GoogleApiDirectionsTask extends AsyncTask<Location, Void, JSONObject> {

  private SingletonApplicationBase app;
  
  private Location startLoc;
  
  private List<LatLng> points;
  
  Long start = System.currentTimeMillis();

  private int minStep = 50;
  
  private Runnable onFinish;

  public GoogleApiDirectionsTask( SingletonApplicationBase app, List<LatLng> points ) {
    this.app = app;
    this.points = points;
  }
  
  public GoogleApiDirectionsTask( SingletonApplicationBase app, List<LatLng> points, Runnable onFinish ) {
    this( app, points );
    this.onFinish = onFinish;
  }
  
  public GoogleApiDirectionsTask( SingletonApplicationBase app, int minStep, List<LatLng> points, Runnable onFinish ) {
    this( app, points, onFinish );
    this.minStep = minStep;
  }

  @Override
  protected JSONObject doInBackground( Location... params ) {
    if( 2 > params.length ) return null;
    
    startLoc = params[ 0 ];
    String origin = BaseUtils.asString( startLoc ), dest = BaseUtils.asString( params[ 1 ] );
    String url = "http://maps.googleapis.com/maps/api/directions/json?sensor=true&origin=" + origin + "&destination=" + dest;
    if( 3 == params.length && null != params[ 2 ] ) url += "&waypoints=" + BaseUtils.asString( params[ 2 ] );
    
    try{
      Logg.i( this, "url " + url );
      ResponseTuple rt = app.doGet( url, 4000 );
      if( 200 == rt.getStatusCode() ) return rt.getJson();
    }catch( Exception e ){
      e.printStackTrace();
    }
    return null;
  }

  @Override
  protected void onPostExecute( JSONObject json ) {
    if( null == json ) return;
    try{
      if( "OK".equals( json.optString( "status" ) ) ){
        decodePoly( json.getJSONArray( "routes" ).getJSONObject( 0 ).getJSONObject( "overview_polyline" ).getString( "points" ) );
        if( null != onFinish ) onFinish.run();
      }
    }catch( Exception e ){
      e.printStackTrace();
    }
    Logg.i( "DirectionsProvider", "time elapsed " + ( System.currentTimeMillis() - start ) + " ms" );
  }

  private void decodePoly( String encoded ) {
    double lat = 0, lng = 0;
    points.clear();
    Location last = startLoc;

    for( int index = 0; index < encoded.length(); ){
      int b, shift = 0, result = 0;
      do{
        b = encoded.charAt( index++ ) - 63;
        result |= ( b & 0x1f ) << shift;
        shift += 5;
      }while( b >= 0x20 );
      int dlat = ( ( result & 1 ) != 0 ? ~( result >> 1 ) : ( result >> 1 ) );
      lat += dlat;

      shift = 0;
      result = 0;
      do{
        b = encoded.charAt( index++ ) - 63;
        result |= ( b & 0x1f ) << shift;
        shift += 5;
      }while( b >= 0x20 );
      int dlng = ( ( result & 1 ) != 0 ? ~( result >> 1 ) : ( result >> 1 ) );
      lng += dlng;

      LatLng curr = new LatLng( lat / 100000, lng / 100000 );
      Location loc = BaseUtils.toLocation( curr );
      if( minStep <= loc.distanceTo( last ) ){
        points.add( curr );
        last = loc;
      }
    }
  }
  
}
