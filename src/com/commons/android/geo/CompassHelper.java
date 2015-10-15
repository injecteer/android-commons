package com.commons.android.geo;

import java.util.HashMap;
import java.util.Map;

import com.commons.android.Logg;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;

public class CompassHelper implements SensorEventListener {

  private float bearing = 0;
  
  private SensorManager sensorManager;

  private static final int[] SENSORS = { Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_MAGNETIC_FIELD };
  
  public interface OnVectorChanged {
    public void onChange( float bearing );
  }
  
  private Map<Integer, Tuple> sensorAccuracies = new HashMap<>();
  
  private OnVectorChanged onVectorChanged = null;
  
  private float[] valuesAccelerometer = new float[3], valuesMagneticField = new float[3],
                  matrixR = new float[9], matrixI = new float[9], matrixValues = new float[3];
  
  public CompassHelper() {
    for( int s : SENSORS ) sensorAccuracies.put( s, new Tuple( null, 0 ) );
  }
  
  public void startCompass( Context ctx ) {
    Logg.i( this, "starting..." );
    sensorManager = (SensorManager)ctx.getSystemService( Context.SENSOR_SERVICE );
    
    for( int s : SENSORS ){
      Sensor sensor = sensorManager.getDefaultSensor( s );
      sensorAccuracies.get( s ).sensor = sensor;
      sensorManager.registerListener( this, sensor, SensorManager.SENSOR_DELAY_NORMAL );
    }
  }

  public void stopCompass() {
    if( null == sensorManager ) return;
    Logg.i( this, "stopping" );
    for( Tuple t : sensorAccuracies.values() ){
      if( null != t.sensor ) sensorManager.unregisterListener( this, t.sensor );
    }
  }

  public float getBearing() {
    return bearing;
  }
  
  public void updateBearing( Location loc ) {
    if( !loc.hasBearing() ) loc.setBearing( bearing );
  }
  
  public void setOnVectorChanged( OnVectorChanged onVectorChanged ) {
    this.onVectorChanged = onVectorChanged;
  }
  
  @Override
  public void onSensorChanged( SensorEvent event ) {
    if( 1 > sensorAccuracies.get( event.sensor.getType() ).accuracy ) return;
    
    switch( event.sensor.getType() ){
      case Sensor.TYPE_ACCELEROMETER:
        System.arraycopy( event.values, 0, valuesAccelerometer, 0, 3 );
        break;
      case Sensor.TYPE_MAGNETIC_FIELD:
        System.arraycopy( event.values, 0, valuesMagneticField, 0, 3 );
        break;
      default: return;
    }
    
    if( SensorManager.getRotationMatrix( matrixR, matrixI, valuesAccelerometer, valuesMagneticField ) ){
      SensorManager.getOrientation( matrixR, matrixValues );
      bearing = (float)Math.toDegrees( matrixValues[ 0 ] );
      if( null != onVectorChanged ) onVectorChanged.onChange( bearing );
    }
  }

  @Override public void onAccuracyChanged( Sensor sensor, int accuracy ) {
    Logg.i( this, sensor + " -> " + accuracy );
    sensorAccuracies.get( sensor.getType() ).accuracy = accuracy;
  }
  
  class Tuple {
    Sensor sensor;
    int accuracy;
    Tuple( Sensor sensor, int accuracy ) {
      this.sensor = sensor;
      this.accuracy = accuracy;
    }
  }
  
}
