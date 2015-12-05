package com.commons.android;

/**
 * Created by Injecteer on 26.11.2015.
 */
public class NameValuePair {

  private String name;

  private String value;

  public NameValuePair( String name, String value ) {
    this.name = name;
    this.value = value;
  }

  public String getName() { return name; }

  public String getValue() { return value; }

  @Override
  public String toString() {
    return name + ":" + value;
  }
}
