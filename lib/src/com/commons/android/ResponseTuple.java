package com.commons.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponseTuple {

  private int statusCode;
  
  private InputStream inputStream;
  
  private String body;
  
  private Map<String, List<String>> headers = new HashMap<>();
  
  public ResponseTuple( int statusCode, String body ) {
    this.statusCode = statusCode;
    this.body = body;
  }

  public ResponseTuple( int statusCode, String body, Map<String, List<String>> headers ) {
    this( statusCode, body );
    this.headers.putAll( headers );
  }

  public ResponseTuple( int statusCode, InputStream inputStream ) {
    this.statusCode = statusCode;
    this.inputStream = inputStream;
  }

  public ResponseTuple( int statusCode, InputStream inputStream, Map<String, List<String>> headers ) {
    this( statusCode, inputStream );
    this.headers.putAll( headers );
  }

  public List<String> h( String n ) { return headers.get( n ); }
  
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
