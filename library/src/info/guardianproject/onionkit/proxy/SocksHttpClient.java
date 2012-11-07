package info.guardianproject.onionkit.proxy;

import info.guardianproject.onionkit.trust.StrongSSLSocketFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.content.Context;

public class SocksHttpClient extends DefaultHttpClient {

	private final static String DEFAULT_SOCKS_HOST = "localhost";
	private final static int DEFAULT_SOCKS_PORT = 9050;

	private final static int DEFAULT_HTTP_PORT = 80;
	private final static int DEFAULT_HTTPS_PORT = 443;
	
	private static ClientConnectionManager ccm = null;
	private static HttpParams params = null;
	
	public SocksHttpClient (Context context) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException
	{
		this (context, DEFAULT_SOCKS_HOST, DEFAULT_SOCKS_PORT);
		
        
	}
	
	public SocksHttpClient (Context context, String proxyHost, int proxyPort) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException
	{
       super(initConnectionManager(context, Proxy.Type.SOCKS, proxyHost, proxyPort), initParams());

	}
	
	
	private static ClientConnectionManager initConnectionManager (Context context, Proxy.Type proxyType, String proxyHost, int proxyPort) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException
	{
		if (ccm == null)
		{
			
			Proxy proxy = new Proxy(proxyType, new InetSocketAddress(proxyHost, proxyPort));
		    
			SchemeRegistry supportedSchemes = new SchemeRegistry();
		
			supportedSchemes.register(new Scheme("http", 
	                SocksSocketFactory.getSocketFactory(proxyHost, proxyPort), DEFAULT_HTTP_PORT));
	    
		 StrongSSLSocketFactory sfStrong = new StrongSSLSocketFactory(context);
		 sfStrong.setProxy(proxy);
		 supportedSchemes.register(new Scheme("https", 
	                sfStrong, DEFAULT_HTTPS_PORT));
	
	    	
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
