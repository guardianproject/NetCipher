
package info.guardianproject.onionkit.trust;

import android.content.Context;

import ch.boye.httpclientandroidlib.conn.scheme.LayeredSchemeSocketFactory;
import ch.boye.httpclientandroidlib.params.HttpParams;

import java.io.IOException;
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
import javax.net.ssl.X509TrustManager;

public class StrongSSLSocketFactory extends ch.boye.httpclientandroidlib.conn.ssl.SSLSocketFactory
        implements LayeredSchemeSocketFactory
{

    private SSLSocketFactory mFactory = null;

    private Proxy mProxy = null;

    public static final String TLS = "TLS";
    public static final String SSL = "SSL";
    public static final String SSLV2 = "SSLv2";

    // private X509HostnameVerifier mHostnameVerifier = new
    // StrictHostnameVerifier();
    // private final HostNameResolver mNameResolver = new
    // StrongHostNameResolver();

    private TrustManager mTrustManager;

    public StrongSSLSocketFactory(Context context, TrustManager trustManager, KeyStore kStore, String kStorePasswd)
            throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, CertificateException, IOException
    {
        super(kStore);

        mTrustManager = trustManager;

        SSLContext sslContext = SSLContext.getInstance("TLS");
        TrustManager[] tm = new TrustManager[] {
        		mTrustManager
        };
        KeyManager[] km = createKeyManagers(kStore,
        		kStorePasswd);
        sslContext.init(km, tm, new SecureRandom());

        mFactory = sslContext.getSocketFactory();

    }

    public TrustManager getStrongTrustManager()
    {
        return mTrustManager;
    }

    private KeyManager[] createKeyManagers(final KeyStore keystore, final String password)
            throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        if (keystore == null) {
            throw new IllegalArgumentException("Keystore may not be null");
        }
        KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        kmfactory.init(keystore, password != null ? password.toCharArray() : null);
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
    public boolean isSecure(Socket sock) throws IllegalArgumentException {
        return (sock instanceof SSLSocket);
    }

    public void setProxy(Proxy proxy) {
        mProxy = proxy;
    }

    public Proxy getProxy()
    {
        return mProxy;
    }

    @Override
    public Socket createSocket(HttpParams arg0) throws IOException {

        return mFactory.createSocket();

    }

    @Override
    public Socket createLayeredSocket(Socket arg0, String arg1, int arg2,
            boolean arg3) throws IOException, UnknownHostException {
        return ((LayeredSchemeSocketFactory) mFactory).createLayeredSocket(arg0, arg1, arg2, arg3);
    }

}
