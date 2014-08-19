package com.commons.android;

public class TrayAttr {

  public int icon;
  
  public int title;
  
  public String text;
  
  public boolean onGoing = true;

  public int textId;

  public TrayAttr( int icon, int title, int textId ) {
    this.icon = icon;
    this.title = title;
    this.textId = textId;
  }
  
}
