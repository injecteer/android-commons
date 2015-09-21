package com.commons.android;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ResponseTuple {

  private int statusCode;
  
  private InputStream inputStream;
  
  private String body;
  
  private Map<String,String> headers = new HashMap<>();
  
  public ResponseTuple( int statusCode, String body, Header... hs ) {
    this.statusCode = statusCode;
    this.body = body;
    addHeaders( hs );
  }
  
  public ResponseTuple( int statusCode, InputStream inputStream, Header... hs ) {
    this.statusCode = statusCode;
    this.inputStream = inputStream;
    addHeaders( hs );
  }
  
  private void addHeaders( Header[] hs ) {
    for( Header h : hs ) headers.put( h.getName(), h.getValue() );
  }

  public String h( String n ) { return headers.get( n ); }
  
  public int getStatusCode() {
    return statusCode;
  }
  
  public String getBody() {
    return body;
  }
  
  public JSONObject getJson() throws JSONException {
    return BaseUtils.isEmpty( body ) ? null : new JSONObject( body );
  }
  
  public JSONArray getJsonArray() throws JSONException {
    return BaseUtils.isEmpty( body ) ? null : new JSONArray( body );
  }
  
  public Reader getReader() {
    return null == inputStream ? null : new InputStreamReader( inputStream );
  }

  public InputStream getInputStream() {
    return inputStream;
  }
  
}
