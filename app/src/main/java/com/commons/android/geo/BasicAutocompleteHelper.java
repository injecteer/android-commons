package com.commons.android.geo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.commons.android.BaseUtils;
import com.commons.android.NoOpTextWatcher;
import com.commons.android.SingletonApplicationBase;

public class BasicAutocompleteHelper implements TextWatcher, OnTouchListener, OnItemClickListener {

  public static final int THRESHOLD = 2;
  
  protected Activity ctx;
  
  protected View root;
  
  protected final SingletonApplicationBase app;
  
  public AutoCompleteTextView input;
  
  protected ArrayAdapter<LocationTuple> autocompleteAdapter;
  
  protected LocationTuple locationTuple = new LocationTuple( null, "" );
  
  protected Drawable iconTextClear, leftDrawable;
  
  public View loader;
  
  protected Handler delayedHandler;
  
  protected final TextWatcher NO_OP_TEXTWATCHER;
  
  private boolean fireOnlyOnAdd = true;
  
  public BasicAutocompleteHelper( SingletonApplicationBase app, Activity activity, int containerId, int clearIconId, int autocompleteId, int progressBarId ) {
    this( app, activity, null, containerId, clearIconId, autocompleteId, progressBarId );
  }
  
  public BasicAutocompleteHelper( SingletonApplicationBase app, Activity activity, View root, int containerId, int clearIconId, int autocompleteId, int progressBarId ) {
    this( app, activity, root, containerId, autocompleteId, progressBarId );
    if( 0 != clearIconId ){
      iconTextClear = ctx.getResources().getDrawable( clearIconId );
      iconTextClear.setBounds( 0, 0, iconTextClear.getIntrinsicWidth(), iconTextClear.getIntrinsicHeight() );
    }
  }
  
  public BasicAutocompleteHelper( SingletonApplicationBase app, Activity activity, View root, int containerId, int autocompleteId, int progressBarId ) {
    this.app = app;
    this.ctx = activity;
    this.root = root;
    
    View container = null == root ? ctx.findViewById( containerId ) : ( 0 == containerId ? root : root.findViewById( containerId ) );
    
    input = (AutoCompleteTextView)container.findViewById( autocompleteId );
    leftDrawable = input.getCompoundDrawables()[ 0 ];
    input.setText( "" );
    input.setOnItemClickListener( this );
    input.setOnTouchListener( this );
    input.setCompoundDrawablePadding( 0 );
    setClearIconVisible( false );
    
    NO_OP_TEXTWATCHER = new NoOpTextWatcher( this );
    
    if( 0 != progressBarId ) loader = container.findViewById( progressBarId );
    
    delayedHandler = new DelayedGeocodeHandler( app, this, loader );
  }

  @Override
  public void onTextChanged( CharSequence txt, int start, int before, int count ) {
    String text = txt.toString().trim();
    delayedHandler.removeMessages( DelayedGeocodeHandler.MESSAGE_TEXT_CHANGED );
    if( BaseUtils.isEmpty( text ) ){
      fireOnlyOnAdd = true;
      locationTuple = new LocationTuple( null, null );
      setClearIconVisible( false );
    }else{
      setClearIconVisible( true );
      if( text.length() >= THRESHOLD && ( !fireOnlyOnAdd || txt.length() > before ) && !text.equals( locationTuple.getName() ) ){
        Message msg = Message.obtain( delayedHandler, DelayedGeocodeHandler.MESSAGE_TEXT_CHANGED, text );
        delayedHandler.sendMessageDelayed( msg, 500 );
      }
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouch( View v, MotionEvent event ) {
    EditText vv = (EditText)v;
    if( MotionEvent.ACTION_UP != event.getAction() ) return false;
    if( BaseUtils.anyEmpty( vv.getText().toString().trim() ) ){
      showPreSuggestions();
      return null == iconTextClear;
    }else if( null != iconTextClear && null != vv.getCompoundDrawables()[ 2 ] ){
      boolean tappedX = event.getX() > ( vv.getWidth() - vv.getPaddingRight() - iconTextClear.getIntrinsicWidth() );
      if( tappedX ){
        vv.setText( "" );
        locationTuple.clear();
        fireOnlyOnAdd = true;
        setClearIconVisible( false );
        return onClear( input );
      }
    }
    return false;
  }

  public boolean onClear( AutoCompleteTextView input ) {
    return false;
  }
  
  @Override
  public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
    if( android.R.id.text1 == view.getId() ) doItemClick( position );
  }

  /**
   * 
   * @param position
   * @return false, if only the textfield is set
   */
  public boolean doItemClick( int position ) {
    removeTextWatcher();
    if( null != loader ) loader.setVisibility( View.GONE );
    LocationTuple lt = autocompleteAdapter.getItem( position );
    if( null != lt.getLocation() ) setLocationTuple( lt );
    return false;
  }
  
  @Override public void afterTextChanged( Editable txt ) {}
  @Override public void beforeTextChanged( CharSequence arg0, int arg1, int arg2, int arg3 ) {}

  public void setClearIconVisible( boolean visible ) {
    if( null != iconTextClear ) input.setCompoundDrawables( leftDrawable, null, visible ? iconTextClear : null, null );
  }

  public void checkTextWatcher() {
    if( null != input.getTag() ) return;
    input.removeTextChangedListener( NO_OP_TEXTWATCHER );
    input.addTextChangedListener( this );
    input.setTag( "notnull" );
  }

  public void removeTextWatcher() {
    BaseUtils.hideKeyboard( ctx );
    delayedHandler.removeMessages( DelayedGeocodeHandler.MESSAGE_TEXT_CHANGED );
    input.removeTextChangedListener( this );
    input.addTextChangedListener( NO_OP_TEXTWATCHER );
    input.setTag( null );
  }

  protected void showPreSuggestions() {
    if( null == autocompleteAdapter ) return;
    autocompleteAdapter.notifyDataSetChanged();
    input.setAdapter( autocompleteAdapter );
    input.showDropDown();
  }

  public void add( Location loc, CharSequence name ) {
    autocompleteAdapter.add( new LocationTuple( loc, name ) );
    autocompleteAdapter.notifyDataSetChanged();
  }

  public void initAdapter( boolean assignToInput ) {
    autocompleteAdapter = new ArrayAdapter<LocationTuple>( ctx, android.R.layout.simple_dropdown_item_1line ){
      @Override public View getView( int position, View convertView, ViewGroup parent ) {
        View v = super.getView( position, convertView, parent );
        LocationTuple lt = getItem( position );
        if( null != lt.getName() && !String.class.equals( lt.getName().getClass() ) ) ((TextView)v).setText( lt.getName() );
        return v;
      }
    };
    autocompleteAdapter.setNotifyOnChange( true );
    if( assignToInput ) input.setAdapter( autocompleteAdapter );
  }
  
  public Location getTarget() { return locationTuple.getLocation(); }

  public void setTarget( Location target ) {
    locationTuple.setLocation( target );
  }

  public String getTargetString() { return locationTuple.getName().toString(); }

  public void enable() {
    input.setEnabled( true );
    checkTextWatcher();
  }

  public void disable() {
    input.setEnabled( false );
  }

  public void setTargetString( String v ) {
    locationTuple.setName( v );
    fillText( v );
  }

  protected void fillText( CharSequence v ) {
    fireOnlyOnAdd = false;
    if( BaseUtils.isEmpty( v ) ){
      setClearIconVisible( false );
      return;
    }
    removeTextWatcher();
    input.setText( v );
    setClearIconVisible( true );
    checkTextWatcher();
  }
  
  public void setLocationTuple( LocationTuple locationTuple ) {
    this.locationTuple = locationTuple;
    fillText( locationTuple.getName() );
  }

  public LocationTuple getLocationTuple() {
    return locationTuple;
  }
  
  public void clear() {
    locationTuple.clear();
    input.setText( "" );
    setClearIconVisible( false );
  }

}
