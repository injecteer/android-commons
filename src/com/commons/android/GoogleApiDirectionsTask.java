package com.commons.android;

import java.util.List;

import org.json.JSONObject;

import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

public class GoogleApiDirectionsTask extends AsyncTask<Location, Void, JSONObject> {

  private SingletonApplicationBase app;
  
  private GoogleMap map;
  
  private List<LatLng> points;
  
  Long start = System.currentTimeMillis();

  public GoogleApiDirectionsTask( SingletonApplicationBase app, GoogleMap map, List<LatLng> points ) {
    this.app = app;
    this.map = map;
    this.points = points;
  }

  @Override
  protected JSONObject doInBackground( Location... params ) {
    String url = "http://maps.googleapis.com/maps/api/directions/json?sensor=true&";
    String origin = null, dest = null;
    try{
      switch( params.length ){
        case 2:
          origin = BaseUtils.asString( params[ 0 ] );
          dest = BaseUtils.asString( params[ 1 ] );
          url += "&origin=" + origin + "&destination=" + dest;
          break;
        case 3:
          origin = BaseUtils.asString( params[ 0 ] );
          String via = BaseUtils.asString( params[ 1 ] );
          dest = BaseUtils.asString( params[ 2 ] );
          url += "&origin=" + origin + "&destination=" + dest + "&waypoints=" + via;
          break;
        default:
          return null;
      }
      Log.i( "DirectionsProvider", "url " + url );
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
        if( 1 < decodePoly( json.getJSONArray( "routes" ).getJSONObject( 0 ).getJSONObject( "overview_polyline" ).getString( "points" ) ) )
          map.addPolyline( new PolylineOptions().color( Color.MAGENTA ).visible( true ).addAll( points ) );
      }
    }catch( Exception e ){
      e.printStackTrace();
    }
    Log.i( "DirectionsProvider", "time elapsed " + ( System.currentTimeMillis() - start ) + " ms" );
  }

  private int decodePoly( String encoded ) {
    int index = 0, len = encoded.length();
    double lat = 0, lng = 0;
    points.clear();
    
    while( index < len ){
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

      points.add( new LatLng( lat / 100000, lng / 100000 ) );
    }
    return points.size();
  }
  
}
