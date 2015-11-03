package com.commons.android;

import java.io.InputStream;
import java.security.KeyStore;

import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;

public class SSLAwareHttpClient extends DefaultHttpClient {

  public static long DEFAULT_SYNC_MIN_GZIP_BYTES = 256;

  private static final int SOCKET_OPERATION_TIMEOUT = 60 * 1000;
  
  private final Context context;
  
  private final int keystoreId;

  private final String pass;

  public SSLAwareHttpClient( Context context, int keystoreId, String pass ) {
    this.context = context;
    this.keystoreId = keystoreId;
    this.pass = pass;
  }

  @Override
  protected ClientConnectionManager createClientConnectionManager() {
    HttpParams params = new BasicHttpParams();
    HttpConnectionParams.setStaleCheckingEnabled( params, false );
    HttpConnectionParams.setConnectionTimeout( params, SOCKET_OPERATION_TIMEOUT );
    HttpConnectionParams.setSoTimeout( params, SOCKET_OPERATION_TIMEOUT );
    HttpConnectionParams.setSocketBufferSize( params, 8192 );
    HttpClientParams.setRedirecting( params, false );
    
    SchemeRegistry registry = new SchemeRegistry();
    registry.register( new Scheme( "http", PlainSocketFactory.getSocketFactory(), 80 ) );
    registry.register( new Scheme( "https", newSslSocketFactory(), 443 ) );
    
    return new ThreadSafeClientConnManager( params, registry );
  }

  private SSLSocketFactory newSslSocketFactory() {
    try{
      KeyStore trusted = KeyStore.getInstance( "BKS" );
      InputStream in = context.getResources().openRawResource( keystoreId );
      try{
        trusted.load( in, pass.toCharArray() );
      }finally{
        in.close();
      }
      SSLSocketFactory sf = new SSLSocketFactory( trusted );
      sf.setHostnameVerifier( SSLSocketFactory.STRICT_HOSTNAME_VERIFIER );
      return sf;
    }catch( Exception e ){
      throw new AssertionError( e );
    }
  }
}
