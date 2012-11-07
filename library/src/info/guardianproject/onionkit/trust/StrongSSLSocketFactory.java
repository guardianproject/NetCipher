package info.guardianproject.onionkit.trust;

import info.guardianproject.onionkit.proxy.SocksSocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.http.conn.scheme.HostNameResolver;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;

public class StrongSSLSocketFactory extends org.apache.http.conn.ssl.SSLSocketFactory
{
	
	private SSLSocketFactory mFactory = null;

    private Proxy mProxy = null;
    
    public static final String TLS   = "TLS";
    public static final String SSL   = "SSL";
    public static final String SSLV2 = "SSLv2";
    
    private X509HostnameVerifier mHostnameVerifier = new StrictHostnameVerifier();
    private final HostNameResolver mNameResolver = new StrongHostNameResolver();

    
	public StrongSSLSocketFactory (Context context) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException
    {
    	super(null);
 
        SSLContext sslContext = SSLContext.getInstance ("TLS");
        StrongTrustManager tmStrong = new StrongTrustManager (context);
        TrustManager[] tm = new TrustManager[] { tmStrong };
        KeyManager[] km = createKeyManagers(tmStrong.getTrustStore(),tmStrong.getTrustStorePassword());
        sslContext.init (km, tm, new SecureRandom ());

        mFactory = sslContext.getSocketFactory ();
   
    }

	private KeyManager[] createKeyManagers(final KeyStore keystore, final String password)
        throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        if (keystore == null) {
            throw new IllegalArgumentException("Keystore may not be null");
        }
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        kmfactory.init(keystore, password != null ? password.toCharArray(): null);
        return kmfactory.getKeyManagers(); 
    }

	@Override
	public Socket createSocket() throws IOException
	{
	    return mFactory.createSocket();
	}

	@Override
	public Socket createSocket(Socket socket, String host, int port,
			boolean autoClose) throws IOException, UnknownHostException {
	
		return mFactory.createSocket(socket, host, port, autoClose);
	}

	@Override
	public Socket connectSocket(
        final Socket sock,
        final String host,
        final int port,
        final InetAddress localAddress,
        int localPort,
        final HttpParams params
    ) throws IOException {

        if (host == null) {
            throw new IllegalArgumentException("Target host may not be null.");
        }
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null.");
        }

        //Socket underlying = (Socket)
        //    ((sock != null) ? sock : createSocket());
        //Socket underlying = sock;
        //if (underlying == null) underlying = new Socket(); 
          
      //  Socket underlying = SocksSocketFactory.getSocketFactory("localhost", 9050).connectSocket(sock, host, port, localAddress, localPort, params);

        int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
        int soTimeout = HttpConnectionParams.getSoTimeout(params);
        
        Socket sockProxy = null;
        
        if (mProxy != null)
        {
        	InetSocketAddress proxyAddr = (InetSocketAddress)mProxy.address();
        	SocksSocketFactory ssf = SocksSocketFactory.getSocketFactory(proxyAddr.getHostName(),proxyAddr.getPort() );     	
        	sockProxy = ssf.createSocket(null, null, -1, params);
        }
        
        SSLSocket sslsock = (SSLSocket)createSocket(sockProxy, host, port, true);
        
        if ((localAddress != null) || (localPort > 0)) {

            // we need to bind explicitly
            if (localPort < 0)
                localPort = 0; // indicates "any"

            InetSocketAddress isa =
                new InetSocketAddress(localAddress, localPort);
            sslsock.bind(isa);
        }
        

        InetSocketAddress inetSocket;
		InetAddress inetHost = mNameResolver.resolve(host);
		
		if (inetHost != null) {
			inetSocket = new InetSocketAddress(inetHost, port); 
		} else {
			inetSocket = new InetSocketAddress(host, port);            
		}
    
        sslsock.connect(inetSocket, connTimeout);
    	sslsock.setSoTimeout(soTimeout);
        
        try {
        	
        	mHostnameVerifier.verify(host, sslsock);
            // verifyHostName() didn't blowup - good!
        } catch (IOException iox) {
            // close the socket before re-throwing the exception
            try { sslsock.close(); } catch (Exception x) { /*ignore*/ }
            throw iox;
        }

        return sslsock;
    }


	@Override
	public boolean isSecure(Socket sock) throws IllegalArgumentException {
		return (sock instanceof SSLSocket);
	}
	

	public void setProxy (Proxy proxy) {
		mProxy = proxy;
	}
	
	public Proxy getProxy ()
	{
		return mProxy;
	}
	
	class StrongHostNameResolver implements HostNameResolver
	{

		@Override
		public InetAddress resolve(String host) throws IOException {
			
			//can we do proxied name look ups here?
			
			//what can we implement to make name resolution strong
			
			return InetAddress.getByName(host);
		}
		
	}
	
}