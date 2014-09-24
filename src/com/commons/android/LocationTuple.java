package com.commons.android;

import android.location.Location;

public class LocationTuple {

  private Location location;
  
  private String name;
  
  public LocationTuple( Location l, String n ){
    location = l;
    name = n;
  }
  
  @Override
  public String toString() { return name; }
  
  public void setLocation( Location location ) { this.location = location;  }
  
  public Location getLocation() { return location; }
  
  public void setName( String name ) { this.name = name; }
  
  public String getName() { return name; }
 
  public void clear() {
    name = "";
    location = null;
  }
  
}
