package com.commons.android;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;

public class CompassHelper implements SensorEventListener {

  private float bearing = 0;
  
  private SensorManager sensorManager;

  private boolean started = false;
  
  private Sensor sensorAccelerometer, sensorMagneticField;
    
  private float[] valuesAccelerometer = new float[3], valuesMagneticField = new float[3], 
                  matrixR = new float[9], matrixI = new float[9], matrixValues = new float[3];
  
  public void startCompass( Context ctx ) {
    if( started ) return;
    sensorManager = (SensorManager)ctx.getSystemService( Context.SENSOR_SERVICE );
    sensorAccelerometer = sensorManager.getDefaultSensor( Sensor.TYPE_ACCELEROMETER );
    sensorMagneticField = sensorManager.getDefaultSensor( Sensor.TYPE_MAGNETIC_FIELD );
    sensorManager.registerListener( this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL );
    sensorManager.registerListener( this, sensorMagneticField, SensorManager.SENSOR_DELAY_NORMAL );
    started = true;
  }

  public void stopCompass() {
    if( !started ) return;
    sensorManager.unregisterListener( this, sensorAccelerometer );
    sensorManager.unregisterListener( this, sensorMagneticField );
    started = false;
  }

  public float getBearing() {
    return bearing;
  }
  
  public void updateBearing( Location loc ) {
    if( !loc.hasBearing() ) loc.setBearing( bearing );
  }
  
  @Override
  public void onSensorChanged( SensorEvent event ) {
    switch( event.sensor.getType() ){
      case Sensor.TYPE_ACCELEROMETER:
        for( int i = 0; i < 3; i++ ) valuesAccelerometer[ i ] = event.values[ i ];
        break;
      case Sensor.TYPE_MAGNETIC_FIELD:
        for( int i = 0; i < 3; i++ ) valuesMagneticField[ i ] = event.values[ i ];
        break;
    }
    if( SensorManager.getRotationMatrix( matrixR, matrixI, valuesAccelerometer, valuesMagneticField ) ){
      SensorManager.getOrientation( matrixR, matrixValues );
      bearing = (float)Math.toDegrees( matrixValues[ 0 ] );
    }
  }

  @Override public void onAccuracyChanged( Sensor sensor, int accuracy ) {}
  
}
