package com.commons.android;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class BoundedLocations {

  private List<LatLng> positions = new ArrayList<LatLng>();
  
  public void add( LatLng pos ){
    if( !Double.isNaN( pos.latitude ) && !Double.isNaN( pos.longitude ) ) positions.add( pos );
  }
  
  public void addAll( Collection<LatLng> col ){
    for( LatLng ll : col ) add( ll );
  }
  
  public int size() {
    return positions.size();
  }
  
  public LatLngBounds getBounds() {
    return getBounds( positions.size() );
  }
  
  public LatLngBounds getBounds( int to ) {
    double swLat = 90, swLng = 180, neLat = -90, neLng = -180;
    for( int ix = 0; ix < Math.min( to, positions.size() ); ix++ ){
      LatLng ll = positions.get( ix );
      if( Double.isNaN( ll.latitude ) || Double.isNaN( ll.longitude ) ) continue;
      swLat = Math.min( swLat, ll.latitude );
      swLng = Math.min( swLng, ll.longitude );
      neLat = Math.max( neLat, ll.latitude );
      neLng = Math.max( neLng, ll.longitude );
    }
    try{
      return new LatLngBounds( new LatLng( swLat, swLng ), new LatLng( neLat, neLng ) );
    }catch( IllegalArgumentException e ) {
      Log.e( "BoundedLocations", positions.toString() );
      return null;
    }
  }

  public void clear() {
    positions.clear();
  }
  
}
