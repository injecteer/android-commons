package com.commons.android.vorm;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isTransient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.commons.android.BaseUtils;
import com.commons.android.Logg;
import com.commons.android.SingletonApplicationBase;
import com.commons.android.vorm.annotation.DrawableRes;
import com.commons.android.vorm.annotation.Id;
import com.commons.android.vorm.annotation.NotBlank;

import dalvik.system.DexFile;

public class ORMSupport {

  public static final SimpleDateFormat SDF_FROM = new SimpleDateFormat( "yyyy-MM-dd" );
  
  public static final SimpleDateFormat SDF_FROM_LONG = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ" );
  
  public static final SimpleDateFormat SDF_DD_MM_YYYY_TIME = new SimpleDateFormat( "dd.MM.yyyy HH:mm" );

  public static final SimpleDateFormat SDF_DD_MM_YY_TIME = new SimpleDateFormat( "dd.MM.yy HH:mm" );
  
  public static final SimpleDateFormat SDF_DD_MM_YYYY = new SimpleDateFormat( "dd.MM.yyyy" );
  
  public static final SimpleDateFormat SDF_TIME_ONLY = new SimpleDateFormat( "HH:mm" );
  
  public final static Map<Class<? extends DomainClass>, Map<Field, Integer>> VIEW_MAP = new HashMap<>();
  
  public final static Map<Class<? extends DomainClass>, String> ID_FIELDS = new HashMap<>();
  
  public final static Map<String, Class<? extends DomainClass>> DOMAIN_CLASSES = new HashMap<>();
  
  private static SQLiteDatabase db, dbw;
  
  public static void init( SingletonApplicationBase app ) {
    if( null != db || null != dbw ) return;
    db = app.getReadableDatabase();
    dbw = app.getWritableDatabase();
  }
  
  public static SQLiteDatabase db(){ return db; }
  
  public static SQLiteDatabase dbw(){ return dbw; }
  
  @SafeVarargs
  public static void fillMappings( Class<?> Rid, Class<? extends DomainClass>... classes ) {
    for( Class<? extends DomainClass> clazz : classes ) fillMapping( Rid, clazz );
  }
  
  @SuppressWarnings("unchecked")
  public static void fillMappings( Class<?> Rid, Context ctx, String pkg ) {
    try{
      ClassLoader classLoader = ctx.getClassLoader();
      DexFile df = new DexFile( ctx.getPackageCodePath() );
      for (Enumeration<String> iter = df.entries(); iter.hasMoreElements(); ){
        String name = iter.nextElement();
        if( !name.startsWith( pkg ) ) continue;
        Class<?> clazz = classLoader.loadClass( name );
        if( DomainClass.class.isAssignableFrom( clazz ) ) fillMapping( Rid, (Class<? extends DomainClass>)clazz );
      }
    }catch( IOException | ClassNotFoundException e ){
      Logg.e( ORMSupport.class, "fillMappings() failed", e );
    }
  }
  
  private static void fillMapping( Class<?> Rid, Class<? extends DomainClass> clazz ) {
    Map<Field, Integer> map = new HashMap<>();
    
    for( Field f : clazz.getDeclaredFields() ){
      if( null != f.getAnnotation( Id.class ) ) 
        ID_FIELDS.put( clazz, f.getName() );
      else 
        eachField( Rid, f, map );
    }
    if( !DomainClass.class.equals( clazz.getSuperclass() ) ){
      for( Field f : clazz.getSuperclass().getDeclaredFields() ) eachField( Rid, f, map );
    }
    try{
      map.put( DomainClass.class.getDeclaredField( "date" ), Rid.getDeclaredField( "dateView" ).getInt( null ) );
    }catch( Exception e ){}
    
    VIEW_MAP.put( clazz, map );
    DOMAIN_CLASSES.put( DomainClass.table( clazz ), clazz );
    Logg.i( ORMSupport.class, "class added: " + clazz );
  }

  private static void eachField( Class<?> Rid, Field f, Map<Field, Integer> map ) {
    int mods = f.getModifiers();
    if( isFinal( mods ) || isPrivate( mods ) || isTransient( mods ) && null == f.getAnnotation( DrawableRes.class ) ) return;
    
    String n = f.getName() + "View";
    Integer val = null;
    try{
      Field viewF = Rid.getDeclaredField( n );
      if( null != viewF ) val = viewF.getInt( null );
    }catch( IllegalAccessException | NoSuchFieldException e ){}
    map.put( f, val );
  }
  
  public static <T extends DomainClass> List<T> query( Class<T> clazz, String q, String... args ){
    Cursor c = db.rawQuery( q, args );
    return asList( clazz, c );
  }
  
  public static <T extends DomainClass> T querySingle( Class<T> clazz, String q, String... args ){
    Cursor c = db.rawQuery( q, args );
    try{
      while( c.moveToNext() ) return fromSql( clazz, c );
      return null;
    }finally{ c.close(); }
  }
  
  public static <T extends DomainClass> T get( Class<T> clazz, String val ){
    return findBy( clazz, "_id", val );
  }
  
  public static <T extends DomainClass> T findBySafe( Class<T> clazz, String name, String... val ){
    if( 1 == val.length && null == val[ 0 ] ) return null;
    T o = findBy( clazz, name, val );
    if( null == o )
      try{
        o = clazz.newInstance();
      }catch( InstantiationException | IllegalAccessException e ){}
    return o;
  }
  
  public static <T extends DomainClass> T findBy( Class<T> clazz, String name, String... val ){
    if( 0 == val.length || null == val[ 0 ] ) return null;
    Cursor c = db.query( DomainClass.table( clazz ), null, name + "=?", val, null, null, null, "1" );
    try{
      if( c.moveToNext() ) return fromSql( clazz, c );
      return null;
    }finally{ c.close(); }
  }
  
  public static <T extends DomainClass> List<T> findAllBy( Class<T> clazz, String name, String... val ){
    Cursor c = db.query( DomainClass.table( clazz ), null, name + "=?", val, null, null, null, null );
    return asList( clazz, c );
  }
  
  public static <T extends DomainClass> Cursor cursorIn( Class<T> clazz, String name, Collection<? extends Object> ids ){
    if( 0 == ids.size() ) return null;
    StringBuilder sb = new StringBuilder();
    for( Object id : ids ){
      if( 0 != sb.length() ) sb.append( "," );
      sb.append( id.toString() );
    }
    return db.query( DomainClass.table( clazz ), null, name + " in (" + sb.toString() + ")", null, null, null, null, null );
  }
  
  public static <T extends DomainClass> List<T> findAllIn( Class<T> clazz, String name, Collection<? extends Object> ids ){
    return asList( clazz, cursorIn( clazz, name, ids ) );
  }

  static <T extends DomainClass> List<T> asList( Class<T> clazz, Cursor c ) {
    List<T> res = new ArrayList<>();
    if( null != c ) try{
      while( c.moveToNext() ) res.add( fromSql( clazz, c ) );
    }finally{ c.close(); }
    return res;
  }
  
  public static <T extends DomainClass> Cursor cursor( Class<T> clazz, int... limit ){
    String lim = DomainClass.NF.format( 0 == limit.length ? 100 : limit[ 0 ] );
    return db.query( DomainClass.table( clazz ), null, null, null, null, null, "timestamp desc", lim );
  }
  
  public static <T extends DomainClass> Cursor cursorBy( Class<T> clazz, String name, String val ){
    return db.query( DomainClass.table( clazz ), null, name + "=?", new String[]{ val }, null, null, "timestamp desc" );
  }
  
  public static <T extends DomainClass> Cursor cursorBy( Class<T> clazz, String name, String val, String sort ){
    return db.query( DomainClass.table( clazz ), null, name + "=?", new String[]{ val }, null, null, sort );
  }
  
  public static long count( String q, String... args ){
    Cursor c = db.rawQuery( q, args );
    try{
      c.moveToNext();
      return c.getLong( 0 );
    }finally{ c.close(); }
  }
  
  public static <T extends DomainClass> long count( Class<T> clazz ){
    Cursor c = db.rawQuery( "select count(*) from " + DomainClass.table( clazz ), null );
    try{
      c.moveToNext();
      return c.getLong( 0 );
    }finally{ c.close(); }
  }
  
  public static long getLong( String q, String... args ){
    Cursor c = db.rawQuery( q, args );
    try{
      return c.moveToNext() ? c.getLong( 0 ) : Long.MIN_VALUE; 
    }finally{ c.close(); }
  }
  
  public static <T extends DomainClass> T fromSql( Class<T> clazz, Cursor cursor ) {
    return fromSql( clazz, cursor, "" );
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <T extends DomainClass> T fromSql( Class<T> clazz, Cursor cursor, String prefix ) {
    T o = null;
    try{
      o = clazz.newInstance();
      int idIx = cursor.getColumnIndex( prefix + "_id" );
      if( -1 < idIx ) o.id = cursor.getString( idIx );
      
      int tsIx = cursor.getColumnIndex( prefix + "timeStamp" );
      if( -1 < tsIx ) o.setTimestamp( cursor.getLong( tsIx ) );
      
      for( Field f : VIEW_MAP.get( clazz ).keySet() ){
        if( isTransient( f.getModifiers() ) ) continue;
        int ix = cursor.getColumnIndex( prefix + f.getName() );
        if( -1 == ix || cursor.isNull( ix ) ) continue;
        
        if( f.getType().isEnum() ){
          f.set( o, Enum.valueOf( (Class<Enum>)f.getType(), cursor.getString( ix ).toUpperCase() ) );
          continue;
        }
        switch( f.getType().getSimpleName().toLowerCase() ){
          case "date":
            f.set( o, new Date( cursor.getLong( ix ) * 1000 ) );
            break;
            
          case "string":
            f.set( o, cursor.getString( ix ) );
            break;
            
          case "boolean":
            f.set( o, 0 != cursor.getInt( ix ) );
            break;
            
          case "int":
          case "integer":
            f.set( o, cursor.getInt( ix ) );
            break;
            
          case "long":
            f.set( o, cursor.getLong( ix ) );
            break;
            
          case "float":
            f.set( o, (float)cursor.getDouble( ix ) );
            break;
        }
      }
    }catch( InstantiationException | IllegalAccessException e ){
      Logg.e( clazz, "" + clazz, e );
    }
    o.afterLoad();
    return o;
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <T extends DomainClass> T fromJson( Class<T> clazz, JSONObject json ) {
    T o = null;
    try{
      o = clazz.newInstance();
      if( ID_FIELDS.containsKey( clazz ) ) o.id = json.optString( ID_FIELDS.get( clazz ) );
      try{ o.setTimestamp( SDF_FROM_LONG.parse( json.optString( "_timeChanged" ) ) ); }catch( ParseException e1 ){}
      
      for( Field f : VIEW_MAP.get( clazz ).keySet() ){
        if( isTransient( f.getModifiers() ) ) continue;
        
        String n = f.getName();
        String v = json.has( n ) && !json.isNull( n ) ? json.optString( n ) : null;

        if( BaseUtils.isEmpty( v ) ){
          if( null != f.getAnnotation( NotBlank.class ) ) return null;
          continue;
        }
        
        if( f.getType().isEnum() ){
          f.set( o, Enum.valueOf( (Class<Enum>)f.getType(), json.optString( n ).toUpperCase() ) );
          continue;
        }
        
        switch( f.getType().getSimpleName().toLowerCase() ){
          case "base64image":
            f.set( o, new Base64Image( v ) );
            break;
            
          case "date":
            try{
              Date d = 10 == v.length() ? SDF_FROM.parse( v ) : SDF_FROM_LONG.parse( v );
              f.set( o, d );
            }catch( ParseException e ){}
            break;
            
          case "string":
            f.set( o, v );
            break;
            
          case "boolean":
            f.set( o, json.optBoolean( n, false ) || 0 != json.optInt( n, 0 ) );
            break;
            
          case "int":
          case "integer":
            f.set( o, json.optInt( n ) );
            break;
            
          case "long":
            f.set( o, json.optLong( n ) );
            break;
            
          case "float":
            f.set( o, (float)json.optDouble( n ) );
            break;
        }
      }
      o.completeJson( json );
    }catch( InstantiationException | IllegalAccessException e ){
      Logg.e( clazz, "", e );
    }
    return o;
  }
  
}
