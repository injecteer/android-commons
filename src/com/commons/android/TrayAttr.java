package com.commons.android;

public class TrayAttr {

  public int icon = 0;
  
  public int title = 0;
  
  public String text;
  
  public int textId = 0;
  
  public boolean onGoing = false;
  
  public boolean sound = true;
  
  public boolean vibrate = false;

  public TrayAttr( int icon ) {
    this.icon = icon;
  }
  
  public TrayAttr( int icon, int title, int textId ) {
    this.icon = icon;
    this.title = title;
    this.textId = textId;
  }
  
  public TrayAttr( int icon, int title, String text ) {
    this.icon = icon;
    this.title = title;
    this.text = text;
  }
  
}
