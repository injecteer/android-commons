package com.commons.android;

public class TrayAttr {

  public int icon = 0;
  
  public int title = 0;
  
  public String text;
  
  public int textId = 0;
  
  public boolean onGoing = true;

  public TrayAttr( int icon, int title, int textId ) {
    this.icon = icon;
    this.title = title;
    this.textId = textId;
  }
  
}
