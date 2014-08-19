package com.commons.android;

import org.json.JSONException;
import org.json.JSONObject;

public class ResponseTuple {

  private int statusCode;
  
  private String body;
  
  public ResponseTuple( int statusCode, String body ) {
    this.statusCode = statusCode;
    this.body = body;
  }
  
  public int getStatusCode() {
    return statusCode;
  }
  
  public String getBody() {
    return body;
  }
  
  public JSONObject getJson() throws JSONException {
    return new JSONObject( body );
  }
  
}
