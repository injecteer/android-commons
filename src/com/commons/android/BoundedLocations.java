package com.commons.android;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class BoundedLocations {

  private List<LatLng> positions = new ArrayList<LatLng>();
  
  public void add( LatLng pos ){
    positions.add( pos );
  }
  
  public LatLngBounds getBounds() {
    return getBounds( positions.size() );
  }
  
  public LatLngBounds getBounds( int to ) {
    double swLat = 90, swLng = 180, neLat = -90, neLng = -180;
    for( int ix = 0; ix < Math.min( to + 1, positions.size() ); ix++ ){
      LatLng ll = positions.get( ix );
      swLat = Math.min( swLat, ll.latitude );
      swLng = Math.min( swLng, ll.longitude );
      neLat = Math.max( neLat, ll.latitude );
      neLng = Math.max( neLng, ll.longitude );
    }
    return new LatLngBounds( new LatLng( swLat, swLng ), new LatLng( neLat, neLng ) );
  }

  public void clear() {
    positions.clear();
  }
  
}
