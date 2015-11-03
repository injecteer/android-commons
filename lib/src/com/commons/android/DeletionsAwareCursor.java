package com.commons.android;

import android.database.AbstractCursor;
import android.database.Cursor;

public class DeletionsAwareCursor extends AbstractCursor {
  
  private Cursor cursor;
  
  private int posToIgnore;

  public DeletionsAwareCursor( Cursor cursor, int posToRemove ) {
    this.cursor = cursor;
    this.posToIgnore = posToRemove;
  }
  
  @Override
  public boolean onMove( int oldPosition, int newPosition ) {
    cursor.moveToPosition( newPosition + ( newPosition < posToIgnore ? 0 : 1 ) );
    return true;
  }
  
  public Cursor getCursor() {
    return cursor;
  }

  @Override
  public int getCount() { return cursor.getCount() - 1; }

  @Override public String[] getColumnNames() { return cursor.getColumnNames(); }
  @Override public String getString( int column ) { return cursor.getString( column ); }
  @Override public short getShort( int column ) { return cursor.getShort( column ); }
  @Override public int getInt( int column ) { return cursor.getInt( column ); }
  @Override public long getLong( int column ) { return cursor.getLong( column ); }
  @Override public float getFloat( int column ) { return cursor.getFloat( column ); }
  @Override public double getDouble( int column ) { return cursor.getDouble( column ); }
  @Override public boolean isNull( int column ) { return cursor.isNull( column ); }

}