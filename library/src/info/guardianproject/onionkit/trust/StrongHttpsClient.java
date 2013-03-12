package info.guardianproject.onionkit.trust;


import info.guardianproject.onionkit.proxy.SocksProxyClientConnOperator;

import org.apache.http.HttpHost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;

import android.content.Context;

public class StrongHttpsClient extends DefaultHttpClient {

  final Context context;
  private HttpHost socksProxy;
  
  private StrongSSLSocketFactory sFactory;
  private SchemeRegistry mRegistry;
  
  public StrongHttpsClient(Context context) {
    this.context = context;
    
    mRegistry = new SchemeRegistry();
    mRegistry.register(
      new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
  
  
  try {
  	sFactory = new StrongSSLSocketFactory(context);
  	mRegistry.register(new Scheme("https", sFactory, 443));
  } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @Override protected ClientConnectionManager createClientConnectionManager() {
   
    
    socksProxy = (HttpHost)getParams().getParameter("SOCKS");
    
    if (socksProxy == null)
    {
    	return  new SingleClientConnManager(getParams(), mRegistry);
    	
    }
    else
    {
    	
    
	    return new SingleClientConnManager(getParams(), mRegistry)
	    		{
	
					@Override
					protected ClientConnectionOperator createConnectionOperator(
							SchemeRegistry schreg) {
						
						return new SocksProxyClientConnOperator(schreg, socksProxy.getHostName(), socksProxy.getPort());
					}
	    	
	    		};
	    }
  }
  
  public StrongTrustManager getStrongTrustManager ()
  {
	  return sFactory.getStrongTrustManager();
  }

  public void useProxy (boolean enableTor, String type, String host, int port)
  {
	  if (enableTor)
		getParams().setParameter(type,  new HttpHost(host, port));
	  else
		  getParams().removeParameter(type);
	  
  }
}