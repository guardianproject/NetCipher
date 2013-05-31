
package info.guardianproject.onionkit.trust;

import android.content.Context;
import android.util.Log;

import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.conn.ClientConnectionOperator;
import ch.boye.httpclientandroidlib.conn.scheme.PlainSocketFactory;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.conn.tsccm.ThreadSafeClientConnManager;
import info.guardianproject.onionkit.proxy.MyThreadSafeClientConnManager;
import info.guardianproject.onionkit.proxy.SocksProxyClientConnOperator;

import java.security.KeyStore;

public class StrongHttpsClient extends DefaultHttpClient {

    final Context context;
    private HttpHost proxyHost;
    private String proxyType;

    private StrongSSLSocketFactory sFactory;
    private StrongTrustManager mTrustManager;
    private SchemeRegistry mRegistry;

    public StrongHttpsClient(Context context) {
        this.context = context;

        mRegistry = new SchemeRegistry();
        mRegistry.register(
                new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));

        try {
            mTrustManager = new StrongTrustManager(context);
            sFactory = new StrongSSLSocketFactory(context, mTrustManager);
            mRegistry.register(new Scheme("https", 443, sFactory));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public StrongHttpsClient(Context context, KeyStore keystore) {
        this.context = context;

        mRegistry = new SchemeRegistry();
        mRegistry.register(
                new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));

        try {
            mTrustManager = new StrongTrustManager(context, keystore);
            sFactory = new StrongSSLSocketFactory(context, mTrustManager);
            mRegistry.register(new Scheme("https", 443, sFactory));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected ThreadSafeClientConnManager createClientConnectionManager() {

        if (proxyHost == null && proxyType == null)
        {
            Log.d("StrongHTTPS", "not proxying");

            return new MyThreadSafeClientConnManager(getParams(), mRegistry);

        }
        else if (proxyHost != null && proxyType.equalsIgnoreCase("socks"))
        {
            Log.d("StrongHTTPS", "proxying using: " + proxyType);

            return new MyThreadSafeClientConnManager(getParams(), mRegistry)
            {

                @Override
                protected ClientConnectionOperator createConnectionOperator(
                        SchemeRegistry schreg) {

                    return new SocksProxyClientConnOperator(schreg, proxyHost.getHostName(),
                            proxyHost.getPort());
                }

            };
        }
        else
        {
            Log.d("StrongHTTPS", "proxying with: " + proxyType);

            return new MyThreadSafeClientConnManager(getParams(), mRegistry);
        }
    }

    public StrongTrustManager getStrongTrustManager()
    {
        return sFactory.getStrongTrustManager();
    }

    public void useProxy(boolean enableTor, String type, String host, int port)
    {

        if (proxyType != null)
        {
            getParams().removeParameter(proxyType);
            proxyHost = null;
        }

        if (enableTor)
        {
            this.proxyType = type;

            HttpHost proxyHost = new HttpHost(host, port);
            getParams().setParameter(type, proxyHost);

            if (type.equalsIgnoreCase("socks"))
            {
                this.proxyHost = proxyHost;
            }
        }

    }
}
