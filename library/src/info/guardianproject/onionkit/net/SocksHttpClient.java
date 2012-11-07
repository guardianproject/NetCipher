package info.guardianproject.onionkit.net;

import java.net.Proxy;
import java.net.UnknownHostException;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class SocksHttpClient extends DefaultHttpClient {

	private final static String DEFAULT_HOST = "localhost";
	private final static int DEFAULT_PORT = 9050;
	
	private static ClientConnectionManager ccm = null;
	private static HttpParams params = null;
	
	public SocksHttpClient () throws UnknownHostException
	{
		
       super(initConnectionManager(DEFAULT_HOST, DEFAULT_PORT), initParams());
      
       
	}
	
	public SocksHttpClient (String host, int port) throws UnknownHostException
	{
		
       super(initConnectionManager(host, port), initParams());
  
       
	}
	
	
	private static ClientConnectionManager initConnectionManager (String proxyHost, int proxyPort) throws UnknownHostException
	{
		if (ccm == null)
		{
			
		
		SchemeRegistry supportedSchemes = new SchemeRegistry();
		
		 supportedSchemes.register(new Scheme("http", 
	                SocksSocketFactory.getSocketFactory(proxyHost, proxyPort), 80));
	    
		 supportedSchemes.register(new Scheme("https", 
	                ModSSLSocketFactory.getSocketFactory(Proxy.Type.SOCKS, proxyHost, proxyPort), 443));
	
		 
		  ccm = new MyThreadSafeClientConnManager(initParams(), supportedSchemes);
		  
		  
		}
		
      return ccm;
	}
	
	private static HttpParams initParams ()
	{
	    if (params == null)
	    {
	      // prepare parameters
	      params = new BasicHttpParams();
	 //     HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
//	      HttpProtocolParams.setContentCharset(params, "UTF-8");
	//      HttpProtocolParams.setUseExpectContinue(params, true);
	    }
	    
	    return params;
	}
}
