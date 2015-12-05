package com.commons.android.geo;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.hardware.Sensor;
import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_ROTATION_VECTOR;
import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;

import com.commons.android.Logg;

public class CompassHelper implements SensorEventListener {
  
  private static final float DEFAULT_ALPHA = .3f;

  /*
   * time smoothing constant for low-pass filter
   * 0 ≤ alpha ≤ 1 ; a smaller value basically means more smoothing
   * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
   */
  private float ALPHA;

  private float bearing = 0;
  
  private SensorManager sensorManager;

  private static final int[] SENSORS = { TYPE_ROTATION_VECTOR, TYPE_ACCELEROMETER, TYPE_MAGNETIC_FIELD };
  
  public interface OnVectorChanged {
    void onChange( float bearing );
  }
  
  private Map<Integer, Tuple> sensors = new HashMap<>();
  
  private OnVectorChanged onVectorChanged = null;
  
  private float[] matrixR = new float[9], matrixI = new float[9], result = new float[3];

  public CompassHelper( float... alpha ) {
    ALPHA = 1 == alpha.length ? alpha[ 0 ] : DEFAULT_ALPHA;
    for( int s : SENSORS ) sensors.put( s, new Tuple( null, 0 ) );
  }
   
  public void startCompass( Context ctx ) {
    sensorManager = (SensorManager)ctx.getSystemService( Context.SENSOR_SERVICE );
    for( int s : SENSORS ){
      Sensor sensor = sensorManager.getDefaultSensor( s );
      sensors.get( s ).sensor = sensor;
      sensorManager.registerListener( this, sensor, SensorManager.SENSOR_DELAY_NORMAL );
      if( TYPE_ROTATION_VECTOR == s && null != sensor ) break;
    }
    Logg.i( this, "started" );
  }

  public void stopCompass() {
    if( null == sensorManager ) return;
    for( Tuple t : sensors.values() ){
      if( null != t.sensor ) sensorManager.unregisterListener( this, t.sensor );
    }
    Logg.i( this, "stopped" );
  }

  public float getBearing() {
    return bearing;
  }
  
  public void updateBearing( Location loc ) {
    if( !loc.hasBearing() ) loc.setBearing( bearing );
  }
  
  public void setOnVectorChanged( Context ctx, OnVectorChanged onVectorChanged ) {
    boolean wasNull = null == this.onVectorChanged;
    this.onVectorChanged = onVectorChanged;
    if( wasNull && null != onVectorChanged ) startCompass( ctx );
  }
  
  @Override
  public void onSensorChanged( SensorEvent event ) {
    int type = event.sensor.getType();
    Tuple tuple = sensors.get( type );
    if( null == tuple || 1 > tuple.accuracy ) return;
    
    lowPass( event.values, tuple.values );

    if( TYPE_ROTATION_VECTOR == type )
      SensorManager.getRotationMatrixFromVector( matrixR, tuple.values );
    else if( !SensorManager.getRotationMatrix( matrixR, matrixI, sensors.get( TYPE_ACCELEROMETER ).values, sensors.get( TYPE_MAGNETIC_FIELD ).values ) )
      return;
    
    SensorManager.getOrientation( matrixR, result );
    bearing = (float)Math.toDegrees( result[ 0 ] );
    if( null != onVectorChanged ) onVectorChanged.onChange( bearing );
  }

  /**
   * @see http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
   * @see http://developer.android.com/reference/android/hardware/SensorEvent.html#values
   */
  private void lowPass( float[] input, float[] out ) {
    if( null == out || null == input ) return;
    for( int i=0; i < input.length; i++ ) out[ i ] += ALPHA * ( input[ i ] - out[ i ] );
  }  

  @Override 
  public void onAccuracyChanged( Sensor sensor, int accuracy ) {
    Logg.i( this, sensor + " -> " + accuracy );
    sensors.get( sensor.getType() ).accuracy = accuracy;
  }
  
  class Tuple {
    Sensor sensor;
    int accuracy;
    float[] values = new float[ 9 ];    
    Tuple( Sensor sensor, int accuracy ) {
      this.sensor = sensor;
      this.accuracy = accuracy;
    }
  }
  
}
