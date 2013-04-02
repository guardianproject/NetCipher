package info.guardianproject.onionkit.trust;


import info.guardianproject.onionkit.proxy.SocksProxyClientConnOperator;
import android.content.Context;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.conn.ClientConnectionOperator;
import ch.boye.httpclientandroidlib.conn.scheme.PlainSocketFactory;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.conn.tsccm.ThreadSafeClientConnManager;

public class StrongHttpsClient extends DefaultHttpClient {

  final Context context;
  private HttpHost socksProxy;
  
  private StrongSSLSocketFactory sFactory;
  private SchemeRegistry mRegistry;
  private ThreadSafeClientConnManager mConnMgr;
  
  public StrongHttpsClient(Context context) {
    this.context = context;
    
    mRegistry = new SchemeRegistry();
    mRegistry.register(
      new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
  
  
  try {
  	sFactory = new StrongSSLSocketFactory(context);
  	mRegistry.register(new Scheme("https", 443, sFactory));
  } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @Override protected ThreadSafeClientConnManager createClientConnectionManager() {
   
    
	  if (mConnMgr != null)
	  {
		  
		  return mConnMgr;
	  }
	  else
	  {
	 
	    socksProxy = (HttpHost)getParams().getParameter("SOCKS");
	    
	    if (socksProxy == null)
	    {
	    	return  mConnMgr = new ThreadSafeClientConnManager(getParams(), mRegistry);
	    	
	    }
	    else
	    {
	    	
	    
		    return mConnMgr = new ThreadSafeClientConnManager(getParams(), mRegistry)
		    		{
		
						@Override
						protected ClientConnectionOperator createConnectionOperator(
								SchemeRegistry schreg) {
							
							return new SocksProxyClientConnOperator(schreg, socksProxy.getHostName(), socksProxy.getPort());
						}
		    	
		    		};
		    }
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