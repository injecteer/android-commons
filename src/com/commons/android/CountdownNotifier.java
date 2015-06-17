package com.commons.android;

import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat.Builder;

public class CountdownNotifier implements Runnable {

  private Builder builder;
  
  private int max, progress;
  
  private boolean cancelled = false;

  private int sleep = 1000;

  private NotificationManager manager;
  
  private Runnable finisher;

  private final int TRAY_ID;
  
  public CountdownNotifier( NotificationManager manager, Builder builder, int tray_id, int max ) {
    this.manager = manager;
    this.builder = builder;
    this.TRAY_ID = tray_id;
    this.max = max;
    this.progress = max;
    builder.setVibrate( null ).setSound( null );
  }
  
  public CountdownNotifier( NotificationManager manager, Builder builder, int tray_id, int max, int progress ) {
    this( manager, builder, tray_id, max );
    this.progress = progress;
  }
  
  public void setFinisher( Runnable finisher ) {
    this.finisher = finisher;
  }
  
  @Override
  public void run() {
    while( !cancelled && 0 <= progress-- ){
      builder.setProgress( max, progress, false );
      manager.notify( TRAY_ID, builder.build() );
      try{ Thread.sleep( sleep ); }catch( InterruptedException e ){}
    }
    if( !cancelled ){
      manager.cancel( TRAY_ID );
      if( null != finisher ) finisher.run();
    }
  }
  
  public void start() { new Thread( this ).start(); }
  
  public void cancel() { 
    cancelled = true; 
    manager.cancel( TRAY_ID );
  }
  
  public void setSleep( int sleep ) {
    this.sleep = sleep;
  }
  
  public int getProgress() { return progress; }

}