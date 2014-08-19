package com.commons.android;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;

public class AutocompleteHelper implements TextWatcher, OnTouchListener, OnItemClickListener {

  private static String[] FROM = { "name", "lat", "lon", "locality" };
  
  private static final int TEXT_WATCHER_SET_ID = 550555271;
  
  public static final int THRESHOLD = 2;
  
  private Activity ctx;
  
  private Location target;
  
  private String targetString; 
  
  private final SingletonApplicationBase app;
  
  public AutoCompleteTextView input;
  
  ArrayAdapter<LocationTuple> autocompleteAdapter;
  
  private Drawable iconTextClear;
  
  private ProgressBar autoCompleteProgressBar;
  
  private Handler delayedHandler;
  
  private final TextWatcher NO_OP_TEXTWATCHER;
  
  public AutocompleteHelper( SingletonApplicationBase app, Activity activity, int containerId, int clearIconId, int autocompleteId, int progressBarId ) {
    this( app, activity, containerId, 0, clearIconId, autocompleteId, progressBarId );
  }
  
  public AutocompleteHelper( SingletonApplicationBase app, Activity activity, int containerId, int includeId, int clearIconId, int autocompleteId, int progressBarId ) {
    this.app = app;
    this.ctx = activity;
    
    NO_OP_TEXTWATCHER = new NoOpTextWatcher( this );
    
    iconTextClear = ctx.getResources().getDrawable( clearIconId );
    iconTextClear.setBounds( 0, 0, iconTextClear.getIntrinsicWidth(), iconTextClear.getIntrinsicHeight() );
    
    View container = 0 == includeId ? ctx.findViewById( containerId ) : ctx.findViewById( includeId ).findViewById( containerId );
    
    input = (AutoCompleteTextView)container.findViewById( autocompleteId );
    input.setText( "" );
    input.setOnItemClickListener( this );
    input.setOnTouchListener( this );
    input.setCompoundDrawablePadding( 0 );
    setClearIconVisible( false );
    
    autoCompleteProgressBar = (ProgressBar)container.findViewById( progressBarId );
    
    delayedHandler = new DelayedGeocodeHandler( app, this, autoCompleteProgressBar );
  }
  
  @Override
  public void onTextChanged( CharSequence txt, int start, int before, int count ) {
    String text = txt.toString().trim();
    delayedHandler.removeMessages( DelayedGeocodeHandler.MESSAGE_TEXT_CHANGED );
    if( 0 == text.length() ){
      target = null;
      targetString = null;
    }else{
      setClearIconVisible( true );
      if( text.length() >= THRESHOLD && count > before && !text.equals( targetString ) ){
        Message msg = Message.obtain( delayedHandler, DelayedGeocodeHandler.MESSAGE_TEXT_CHANGED, text );
        delayedHandler.sendMessageDelayed( msg, 500 );
      }
    }
  }

  @Override 
  public boolean onTouch( View v, MotionEvent event ) {
    EditText vv = (EditText)v;
    if( MotionEvent.ACTION_UP != event.getAction() ) return false;
    boolean empty = 0 == vv.getText().toString().trim().length();
    if( empty ) 
      showHistorySuggestions();
    else if( null != vv.getCompoundDrawables()[ 2 ] ){
      boolean tappedX = event.getX() > ( vv.getWidth() - vv.getPaddingRight() - iconTextClear.getIntrinsicWidth() );
      if( tappedX ){ 
        vv.setText( "" );
        target = null;
        targetString = null;
        setClearIconVisible( false );
      }
    }
    return false;
  }
  
  @Override
  public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
    removeTextWatcher();
    autoCompleteProgressBar.setVisibility( View.GONE );
    LocationTuple lt = autocompleteAdapter.getItem( position );
    target = lt.getLocation();
    targetString = lt.getName();
  }

  @Override public void afterTextChanged( Editable txt ) {}

  @Override public void beforeTextChanged( CharSequence arg0, int arg1, int arg2, int arg3 ) {}

  public void setClearIconVisible( boolean visible ) {
    input.setCompoundDrawables( null, null, visible ? iconTextClear : null, null );
  }
  
  public void checkTextWatcher() {
    if( null == input.getTag( TEXT_WATCHER_SET_ID ) ){
      input.removeTextChangedListener( NO_OP_TEXTWATCHER );
      input.addTextChangedListener( this );
      input.setTag( TEXT_WATCHER_SET_ID, "notnull" );
    }
  }
  
  public void removeTextWatcher() {
    BaseUtils.hideKeyboard( ctx, input );
    delayedHandler.removeMessages( DelayedGeocodeHandler.MESSAGE_TEXT_CHANGED );
    input.removeTextChangedListener( this );
    input.addTextChangedListener( NO_OP_TEXTWATCHER );
    input.setTag( TEXT_WATCHER_SET_ID, null );
  }
  
  protected void showHistorySuggestions() {
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
  
  public void add( Location loc, String name ) {
    autocompleteAdapter.add( new LocationTuple( loc, name ) );
    autocompleteAdapter.notifyDataSetChanged();
  }

  public void initAdapter( boolean assignToInput ) {
    autocompleteAdapter = new ArrayAdapter<LocationTuple>( ctx, android.R.layout.simple_dropdown_item_1line );
    autocompleteAdapter.setNotifyOnChange( true );
    if( assignToInput ) input.setAdapter( autocompleteAdapter );
  }
  
  public Location getTarget() { return target; }
  
  public void setTarget( Location target ) {
    this.target = target;
  }
  
  public String getTargetString() { return targetString; }

  public void enable() {
    input.setEnabled( true );
    checkTextWatcher();
  }

  public void disable() {
    input.setEnabled( false );
  }

  public void setTargetString( String v ) {
    targetString = v;
    input.setText( v );
  }
  
}