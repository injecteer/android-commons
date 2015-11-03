package com.commons.android.geo;

import android.location.Location;

public class LocationTuple {

  private Location location;
  
  private CharSequence name;
  
  public LocationTuple( Location l, CharSequence n ){
    location = l;
    name = n;
  }
  
  @Override
  public String toString() { return name.toString(); }
  
  public void setLocation( Location location ) { this.location = location;  }
  
  public Location getLocation() { return location; }
  
  public void setName( CharSequence name ) { this.name = name; }
  
  public CharSequence getName() { return name; }
 
  public void clear() {
    location = null;
    name = "";
  }
  
}
