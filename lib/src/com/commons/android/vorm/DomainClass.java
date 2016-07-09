package com.commons.android.vorm;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import com.commons.android.BaseUtils;
import com.commons.android.Logg;
import com.commons.android.vorm.annotation.DrawableRes;
import com.commons.android.vorm.annotation.HTML;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static java.text.DateFormat.SHORT;
import static java.text.DateFormat.getDateInstance;

public abstract class DomainClass {

  public static final NumberFormat NF = new DecimalFormat();
  public static final NumberFormat NF_00 = new DecimalFormat();
  
  static {
    NF.setGroupingUsed( false );
    NF.setMaximumFractionDigits( 0 );
    NF_00.setGroupingUsed( false );
    NF_00.setRoundingMode( RoundingMode.HALF_DOWN );
    NF_00.setMinimumFractionDigits( 2 );
    NF_00.setMaximumFractionDigits( 2 );
  }
  
  public static final String[] EMPTY_ARGS = {};

  public Long id;
  
  public Date date;
  
  // time in seconds, millis / 1000
  public long timeStamp;

  public transient Map<String,Object> binding = new HashMap<>();

  public void fillView( final View v ) {
    if( null != id ) v.setTag( id );
    ImageGetter imageGetter = null != binding && binding.containsKey( "imageGetter" ) ? (ImageGetter)binding.get( "imageGetter" ) : null;
    
    for( Entry<Field, Integer> e : ORMSupport.VIEW_MAP.get( this.getClass() ).entrySet() ){
      Field f = e.getKey();
      try{
        if( null == e.getValue() ) continue;
        View trg = v.findViewById( e.getValue() );
        if( null == trg ) continue;
        
        DrawableRes annotation = f.getAnnotation( DrawableRes.class );
        if( null != annotation ){
          fillImage( trg, f );
          continue;
        }
        
        Object value = f.get( this );
        if( null == value ){
          if( View.VISIBLE == trg.getVisibility() ) trg.setVisibility( View.INVISIBLE );
          continue;
        }else
          trg.setVisibility( View.VISIBLE );

        TextView textView = (TextView)trg;
        switch( f.getType().getSimpleName().toLowerCase() ){
          case "string":
            String s = (String)value;
            if( BaseUtils.isEmpty( s ) ){
              trg.setVisibility( View.INVISIBLE );
              break;
            }
            if( WebView.class.equals( trg.getClass() ) ){
              WebView wv = (WebView)trg;
              wv.loadData( s, "text/html", "UTF-8" );
              break;
            }
            textView.setText( null != f.getAnnotation( HTML.class ) ? Html.fromHtml( s, imageGetter, null ) : s );
            break;
            
          case "int":
          case "integer":
          case "long":
            textView.setText( NF.format( value ) );
            break;
            
          case "float":
          case "double":
            NumberFormat nf = NF_00;
            if( binding.containsKey( "decimalFormat" ) ) nf = (NumberFormat)binding.get( "decimalFormat" );
            textView.setText( nf.format( value ) );
            break;
            
          case "date":
            SimpleDateFormat sdf = ORMSupport.SDF_DD_MM_YY_TIME;
            if( binding.containsKey( f.getName() ) ) sdf = (SimpleDateFormat)binding.get( f.getName() );
            textView.setText( sdf.format( (Date)value ) );
            break;
          
          case "boolean":
            trg.setVisibility( (boolean)value ? View.VISIBLE : View.GONE );
            break;
        }
      }catch( Exception e1 ){
        Logg.w( this, "f = " + f, e1 );
      }
    }
  }

  public void fillImage( View trg, Field f ) {}

  public void setTimestamp( long ts ) {
    timeStamp = ts;
    date = new Date( ts * 1000 );
  }
  
  public void setTimestamp( Date d ) {
    date = d;
    timeStamp = d.getTime() / 1000;
  }

  public void afterLoad() {}
  
  public int delete( SQLiteDatabase db ) {
    return db.delete( table( getClass() ), "_id=?", new String[]{ NF.format( id ) } );
  }
  
  public boolean save() {
    String table = table( this.getClass() );
    ContentValues cv = asContentValues();
    if( null == id ){
      long nid = ORMSupport.dbw().insert( table, null, cv );
      if( -1 != nid ){
        id = nid;
        return true;
      }
    }else{
      ORMSupport.dbw().update( table, cv, "_id=?", new String[]{ NF.format( id ) } );
      return true;
    }
    return false;
  }
  
  public static String table( Class<? extends DomainClass> clazz ) { return clazz.getSimpleName().toLowerCase(); }
  
  public static String getLocalizedFullDate( Date date ) {
    return getDateInstance( SHORT ).format( date ) + ", " + ORMSupport.SDF_TIME_ONLY.format( date );
  }

  public void completeJson( JSONObject json ) {}
  
  /**
   * 
   * @return the CREATE TABLE sqlite command.
   */
  public String getCreateSql() {
    return "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
           + "timeStamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP );";
  }
  
  /**
   * 1. rename table to temp_[name]
   * 2. create new [name] table
   * 3. copy data temp_[name] => [name]
   * 4. drop temp_[name] table 
   * 
   * @param db - DB instance
   * @param dropColumns - if the columns should be dropped
   */
  public void migrate( SQLiteDatabase db, boolean dropColumns ) {
    try{
      String n = table( this.getClass() );
      db.execSQL( "alter table " + n + " rename to temp_" + n + ";" );
      db.execSQL( this.getCreateSql() );
      
      String copyQ = "insert into " + n + " select * from temp_" + n + ";";
      if( dropColumns ){
        StringBuilder cols = new StringBuilder( "_id, timeStamp" );
        for( Field f : ORMSupport.VIEW_MAP.get( this.getClass() ).keySet() ){
          if( !f.getName().matches( "(id|date)" ) ) cols.append( ", " ).append( f.getName() );
        }
        String c = cols.toString();
        copyQ = "insert into " + n + " ( " + c + " ) select " + c + " from temp_" + n + ";";
      }
      db.execSQL( copyQ );
      
      db.execSQL( "drop table temp_" + n + ";" );
      Logg.i( this, "migration of " + n + " successful" );
    }catch( SQLException e ){
      Logg.e( this, "onUpgrade", e );
    }
  }
  
  /**
   * used for Data Import
   * 
   * @return
   */
  @SuppressWarnings("rawtypes")
  public ContentValues asContentValues(){
    ContentValues cv = new ContentValues();
    if( null != id ) cv.put( "timeStamp", "datetime()" );

    for( Field f : ORMSupport.VIEW_MAP.get( this.getClass() ).keySet() ){
      if( Modifier.isTransient( f.getModifiers() ) ) continue;
      String n = f.getName();
      try{
        Object v = f.get( this );
        if( null == v ) continue;
        if( f.getType().isEnum() ){
          cv.put( n, ((Enum)v).name() );
          continue;
        }
        switch( f.getType().getSimpleName().toLowerCase() ){
          case "base64image":
            cv.put( n, ((Base64Image)v).image );
            break;
          case "date":
            if( !"date".equals( n ) ) cv.put( n, (int)(((Date)v).getTime() / 1000) );
            break;
          case "string":
            cv.put( n, v.toString() );
            break;
          case "int":
          case "integer":
            cv.put( n, (Integer)v );
            break;
          case "long":
            cv.put( n, (Long)v );
            break;
          case "float":
            cv.put( n, (Float)v );
            break;
          case "boolean":
            cv.put( n, (boolean)v ? 1 : 0 );
            break;
        }
      }catch( IllegalAccessException | IllegalArgumentException ignored ){}
    }
    return cv;
  }
  
  @Override
  public String toString() {
    return "<" + getClass().getSimpleName() + ":" + id + ">";
  }
  
  public static String now() {
    return NF.format( System.currentTimeMillis() / 1000 );
  }
  
  public static String[] nowArr() {
    return new String[]{ now() };
  }

  public static String asInList( Collection<String> vals ) {
    StringBuilder sb = new StringBuilder();
    for( String v : vals ){
      if( 0 != sb.length() ) sb.append( "," );
      sb.append( "'" ).append( v ).append( "'" );
    }
    return sb.toString();
  }
  
  public static String asInList( String... vals ) {
    StringBuilder sb = new StringBuilder();
    for( String v : vals ){
      if( 0 != sb.length() ) sb.append( "," );
      sb.append( "'" ).append( v ).append( "'" );
    }
    return sb.toString();
  }
  
}