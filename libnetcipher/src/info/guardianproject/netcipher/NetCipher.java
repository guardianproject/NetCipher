
package info.guardianproject.netcipher;

import android.net.Uri;
import android.text.TextUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

public class NetCipher {

    private NetCipher() {
        // this is a utility class with only static methods
    }

    public final static Proxy ORBOT_HTTP_PROXY = new Proxy(Proxy.Type.HTTP,
            new InetSocketAddress("127.0.0.1", 8118));

    private static Proxy proxy;

    public static void setProxy(String host, int port) {
        if (host != null && port > 0) {
            InetSocketAddress isa = new InetSocketAddress(host, port);
            proxy = new Proxy(Proxy.Type.HTTP, isa);
        } else {
            proxy = null;
        }
    }

    public static void setProxy(Proxy proxy) {
        NetCipher.proxy = proxy;
    }

    public static Proxy getProxy() {
        return proxy;
    }

    public static HttpURLConnection getHttpURLConnection(String urlString)
            throws MalformedURLException, IOException {
        return getHttpURLConnection(new URL(urlString));
    }

    public static HttpURLConnection getHttpURLConnection(URL url) throws IOException {
        if (proxy != null) {
            return (HttpURLConnection) url.openConnection(proxy);
        } else {
            return (HttpURLConnection) url.openConnection();
        }
    }

    public static HttpsURLConnection getHttpsURLConnection(String urlString)
            throws MalformedURLException, IOException, KeyManagementException {
        urlString.replaceFirst("^[Hh][Tt][Tt][Pp]:", "https:");
        return getHttpsURLConnection(new URL(urlString));
    }

    public static HttpsURLConnection getHttpsURLConnection(Uri uri)
            throws MalformedURLException, IOException, KeyManagementException {
        return getHttpsURLConnection(uri.toString());
    }

    public static HttpsURLConnection getHttpsURLConnection(URI uri)
            throws MalformedURLException, IOException, KeyManagementException {
        if (TextUtils.equals(uri.getScheme(), "https"))
            return getHttpsURLConnection(uri.toURL());
        else
            // otherwise force scheme to https
            return getHttpsURLConnection(uri.toString());
    }

    public static HttpsURLConnection getHttpsURLConnection(URL url) throws IOException,
            KeyManagementException {
        SSLContext sslcontext;
        try {
            sslcontext = SSLContext.getInstance("TLSv1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        sslcontext.init(null, null, null); // null means use default
        SSLSocketFactory NoSSLv3Factory = new TlsOnlySocketFactory(sslcontext.getSocketFactory());

        HttpsURLConnection.setDefaultSSLSocketFactory(NoSSLv3Factory);
        if (proxy != null) {
            return (HttpsURLConnection) url.openConnection(proxy);
        } else {
            return (HttpsURLConnection) url.openConnection();
        }
    }
}
