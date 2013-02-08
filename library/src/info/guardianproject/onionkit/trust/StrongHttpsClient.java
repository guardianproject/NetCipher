package info.guardianproject.onionkit.trust;


import android.content.Context;

import org.apache.http.HttpHost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;

import info.guardianproject.onionkit.proxy.SocksProxyClientConnOperator;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class StrongHttpsClient extends DefaultHttpClient {

  final Context context;
  private HttpHost socksProxy;
  
  
  public StrongHttpsClient(Context context) {
    this.context = context;
  }

  @Override protected ClientConnectionManager createClientConnectionManager() {
   
	  
	  SchemeRegistry registry = new SchemeRegistry();
    registry.register(
        new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    try {
		registry.register(new Scheme("https", new StrongSSLSocketFactory(context), 443));
    } catch (Exception e) {
        throw new AssertionError(e);
      }
    
    socksProxy = (HttpHost)getParams().getParameter("SOCKS");
    
    if (socksProxy == null)
    {
    	return  new SingleClientConnManager(getParams(), registry);
    	
    }
    else
    {
    	
    
	    return new SingleClientConnManager(getParams(), registry)
	    		{
	
					@Override
					protected ClientConnectionOperator createConnectionOperator(
							SchemeRegistry schreg) {
						
						return new SocksProxyClientConnOperator(schreg, socksProxy.getHostName(), socksProxy.getPort());
					}
	    	
	    		};
	    }
  }

  public void useProxy (boolean enableTor, String type, String host, int port)
  {
	  if (enableTor)
		getParams().setParameter(type,  new HttpHost(host, port));
	  else
		  getParams().removeParameter(type);
	  
  }
}