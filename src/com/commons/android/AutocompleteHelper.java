package com.commons.android;

import android.app.Activity;
import android.database.Cursor;
import android.location.Location;

public class AutocompleteHelper extends BasicAutocompleteHelper {
  
  private static String[] FROM = { "name", "lat", "lon", "locality" };
  
  public AutocompleteHelper( SingletonApplicationBase app, Activity activity, int containerId, int clearIconId, int autocompleteId, int progressBarId ) {
    this( app, activity, containerId, 0, clearIconId, autocompleteId, progressBarId );
  }
  
  public AutocompleteHelper( SingletonApplicationBase app, Activity activity, int containerId, int includeId, int clearIconId, int autocompleteId, int progressBarId ) {
    super( app, activity, containerId, includeId, clearIconId, autocompleteId, progressBarId );
  }
  
  protected void showPreSuggestions() {
    Cursor cursor = app.getWritableDatabase().query( "location", FROM, null, null, null, null, "_id DESC", "8" );
    initAdapter( false );
    try{
      boolean hasNext = cursor.moveToFirst();
      while( hasNext ){
        Location loc = new Location( cursor.getString( 3 ) );
        loc.setLatitude( cursor.getDouble( 1 ) );
        loc.setLongitude( cursor.getDouble( 2 ) );
        autocompleteAdapter.add( new LocationTuple( loc, cursor.getString( 0 ) ) );
        hasNext = cursor.moveToNext();
      }
      autocompleteAdapter.notifyDataSetChanged();
      input.setAdapter( autocompleteAdapter );
      input.showDropDown();
    }finally{
      cursor.close();
    }
  }
  
}