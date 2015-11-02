
package info.guardianproject.netcipher.client;

import android.content.Context;
import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.TrustManagerFactory;

import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.conn.ClientConnectionOperator;
import ch.boye.httpclientandroidlib.conn.params.ConnRoutePNames;
import ch.boye.httpclientandroidlib.conn.scheme.PlainSocketFactory;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.conn.ssl.BrowserCompatHostnameVerifier;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.conn.tsccm.ThreadSafeClientConnManager;

public class StrongHttpsClient extends DefaultHttpClient {

    final Context context;
    private HttpHost proxyHost;
    private String proxyType;
    private SocksAwareProxyRoutePlanner routePlanner;

    private StrongSSLSocketFactory sFactory;
    private SchemeRegistry mRegistry;

    private final static String TRUSTSTORE_TYPE = "BKS";
    private final static String TRUSTSTORE_PASSWORD = "changeit";

    private KeyStore mKeyStore;
    private String mTrustHosts;




    public StrongHttpsClient(Context context, KeyStore keystore) {

        this (context, keystore, null);

    }

    public StrongHttpsClient(Context context, KeyStore keystore, String trustHosts) {

        this.context = context;

        mKeyStore = keystore;
        mTrustHosts = trustHosts;

        init();

    }

    public StrongHttpsClient(Context context, int keystoreRawId, String trustHosts) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {

        this.context = context;

        mKeyStore = loadKeyStoreRawResource(keystoreRawId);
        mTrustHosts = trustHosts;

        init();

    }


    private void init ()
    {

        mRegistry = new SchemeRegistry();
        mRegistry.register(
                new Scheme(TYPE_HTTP, 80, PlainSocketFactory.getSocketFactory()));

        try {

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(mKeyStore);
            sFactory = new StrongSSLSocketFactory(context, trustManagerFactory.getTrustManagers(), mKeyStore, TRUSTSTORE_PASSWORD);

            if (mTrustHosts != null)
            {
                    SMVerifier verifier = new SMVerifier(context,mTrustHosts);
                    sFactory.setHostnameVerifier(verifier);
            }
            else
            {
                sFactory.setHostnameVerifier(new BrowserCompatHostnameVerifier());
            }

            mRegistry.register(new Scheme("https", 443, sFactory));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private KeyStore loadKeyStoreRawResource (int rawId) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
    {

        KeyStore trustStore = KeyStore.getInstance(TRUSTSTORE_TYPE);

        InputStream in = context.getResources().openRawResource(rawId);
        trustStore.load(in, TRUSTSTORE_PASSWORD.toCharArray());

        return trustStore;
    }


    @Override
    protected ThreadSafeClientConnManager createClientConnectionManager() {

        return new ThreadSafeClientConnManager(getParams(), mRegistry)
        {
            @Override
            protected ClientConnectionOperator createConnectionOperator(
                    SchemeRegistry schreg) {

                return new SocksAwareClientConnOperator(schreg, proxyHost, proxyType,
                        routePlanner);
            }
        };
    }

    public void useProxy(boolean enableTor, String type, String host, int port)
    {
        if (enableTor)
        {
            this.proxyType = type;

            if (type.equalsIgnoreCase(TYPE_SOCKS))
            {
                proxyHost = new HttpHost(host, port);
            }
            else
            {
            	proxyHost = new HttpHost(host, port, type);
                getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxyHost);
            }
        }
        else
        {
        	getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
            proxyHost = null;
        }

    }

    public void disableProxy ()
    {
    	getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
        proxyHost = null;
    }

    public void useProxyRoutePlanner(SocksAwareProxyRoutePlanner proxyRoutePlanner)
    {
        routePlanner = proxyRoutePlanner;
        setRoutePlanner(proxyRoutePlanner);
    }
    
    /**
     * NOT ADVISED, but some sites don't yet have latest protocols and ciphers available, and some
     * apps still need to support them
     * https://dev.guardianproject.info/issues/5644
     */
    public void enableSSLCompatibilityMode() {
        sFactory.setEnableStongerDefaultProtocalVersion(false);
        sFactory.setEnableStongerDefaultSSLCipherSuite(false);
    }

    public final static String TYPE_SOCKS = "socks";
    public final static String TYPE_HTTP = "http";

}
