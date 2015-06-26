
package info.guardianproject.netcipher;

import android.net.Uri;
import android.text.TextUtils;

import info.guardianproject.netcipher.client.TlsOnlySocketFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
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

    /**
     * Set the global HTTP proxy for all new {@link HttpURLConnection}s and
     * {@link HttpsURLConnection}s that are created after this is called.
     *
     * @param host the IP address for the HTTP proxy to use globally
     * @param port the port number for the HTTP proxy to use globally
     */
    public static void setProxy(String host, int port) {
        if (host != null && port > 0) {
            InetSocketAddress isa = new InetSocketAddress(host, port);
            proxy = new Proxy(Proxy.Type.HTTP, isa);
        } else {
            proxy = null;
        }
    }

    /**
     * Set the global HTTP proxy for all new {@link HttpURLConnection}s and
     * {@link HttpsURLConnection}s that are created after this is called.
     *
     * @param proxy the HTTP proxy to use globally
     */
    public static void setProxy(Proxy proxy) {
        NetCipher.proxy = proxy;
    }

    /**
     * Get the currently active global HTTP {@link Proxy}.
     *
     * @return the active HTTP {@link Proxy}
     */
    public static Proxy getProxy() {
        return proxy;
    }

    /**
     * Set Orbot as the global HTTP proxy for all new {@link HttpURLConnection}
     * s and {@link HttpsURLConnection}s that are created after this is called.
     */
    public static void useTor() {
        setProxy(ORBOT_HTTP_PROXY);
    }

    /**
     * Get a {@link HttpsURLConnection} from a URL {@link String} using the best
     * TLS configuration available on the device.
     *
     * @param urlString
     * @return the URL in an instance of {@link HttpsURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect
     */
    public static HttpsURLConnection getHttpsURLConnection(String urlString) throws IOException {
        urlString.replaceFirst("^[Hh][Tt][Tt][Pp]:", "https:");
        return getHttpsURLConnection(new URL(urlString), false);
    }

    /**
     * Get a {@link HttpsURLConnection} from a {@link Uri} using the best TLS
     * configuration available on the device.
     *
     * @param uri
     * @return the {@code uri} in an instance of {@link HttpsURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect
     */
    public static HttpsURLConnection getHttpsURLConnection(Uri uri) throws IOException {
        return getHttpsURLConnection(uri.toString());
    }

    /**
     * Get a {@link HttpsURLConnection} from a {@link URI} using the best TLS
     * configuration available on the device.
     *
     * @param uri
     * @return the {@code uri} in an instance of {@link HttpsURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect
     */
    public static HttpsURLConnection getHttpsURLConnection(URI uri) throws IOException {
        if (TextUtils.equals(uri.getScheme(), "https"))
            return getHttpsURLConnection(uri.toURL(), false);
        else
            // otherwise force scheme to https
            return getHttpsURLConnection(uri.toString());
    }

    /**
     * Get a {@link HttpsURLConnection} from a {@link URL} using the best TLS
     * configuration available on the device.
     *
     * @param url
     * @return the {@code url} in an instance of {@link HttpsURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect
     */
    public static HttpsURLConnection getHttpsURLConnection(URL url) throws IOException {
        return getHttpsURLConnection(url, false);
    }

    /**
     * Get a {@link HttpsURLConnection} from a {@link URL} using a more
     * compatible, but less strong, suite of ciphers.
     *
     * @param url
     * @return the {@code url} in an instance of {@link HttpsURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect
     */
    public static HttpsURLConnection getCompatibleHttpsURLConnection(URL url) throws IOException {
        return getHttpsURLConnection(url, true);
    }

    /**
     * Get a {@link HttpsURLConnection} from a {@link URL}, and specify whether
     * it should use a more compatible, but less strong, suite of ciphers.
     *
     * @param url
     * @param compatible
     * @return the {@code url} in an instance of {@link HttpsURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect
     */
    public static HttpsURLConnection getHttpsURLConnection(URL url, boolean compatible)
            throws IOException {
        // use default method, but enforce a HttpsURLConnection
        HttpURLConnection connection = getHttpURLConnection(url, compatible);
        if (connection instanceof HttpsURLConnection) {
            return (HttpsURLConnection) connection;
        } else {
            throw new IllegalArgumentException("not an HTTPS connection!");
        }
    }

    /**
     * Get a {@link HttpURLConnection} from a {@link URL}. If the connection is
     * {@code https://}, it will use a more compatible, but less strong, TLS
     * configuration.
     *
     * @param url
     * @return the {@code url} in an instance of {@link HttpsURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect
     */
    public static HttpURLConnection getCompatibleHttpURLConnection(URL url) throws IOException {
        return getHttpURLConnection(url, true);
    }

    /**
     * Get a {@link HttpURLConnection} from a URL {@link String}. If it is an
     * {@code https://} link, then this will use the best TLS configuration
     * available on the device.
     *
     * @param urlString
     * @return the URL in an instance of {@link HttpURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect
     */
    public static HttpURLConnection getHttpURLConnection(String urlString) throws IOException {
        return getHttpURLConnection(new URL(urlString));
    }

    /**
     * Get a {@link HttpURLConnection} from a {@link Uri}. If it is an
     * {@code https://} link, then this will use the best TLS configuration
     * available on the device.
     *
     * @param uri
     * @return the {@code uri} in an instance of {@link HttpURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect
     */
    public static HttpURLConnection getHttpURLConnection(Uri uri) throws IOException {
        return getHttpURLConnection(uri.toString());
    }

    /**
     * Get a {@link HttpURLConnection} from a {@link URI}. If it is an
     * {@code https://} link, then this will use the best TLS configuration
     * available on the device.
     *
     * @param uri
     * @return the {@code uri} in an instance of {@link HttpURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect
     */
    public static HttpURLConnection getHttpURLConnection(URI uri) throws IOException {
        return getHttpURLConnection(uri.toURL());
    }

    /**
     * Get a {@link HttpURLConnection} from a {@link URL}. If it is an
     * {@code https://} link, then this will use the best TLS configuration
     * available on the device.
     *
     * @param url
     * @return the {@code url} in an instance of {@link HttpURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect
     */
    public static HttpURLConnection getHttpURLConnection(URL url) throws IOException {
        return (HttpURLConnection) getHttpURLConnection(url, false);
    }

    /**
     * Get a {@link HttpURLConnection} from a {@link URL}, and specify whether
     * it should use a more compatible, but less strong, suite of ciphers.
     *
     * @param url
     * @param compatible
     * @return the {@code url} in an instance of {@link HttpURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect
     */
    public static HttpURLConnection getHttpURLConnection(URL url, boolean compatible)
            throws IOException {
        SSLContext sslcontext;
        try {
            sslcontext = SSLContext.getInstance("TLSv1");
            sslcontext.init(null, null, null); // null means use default
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        } catch (KeyManagementException e) {
            throw new IllegalArgumentException(e);
        }
        SSLSocketFactory tlsOnly = new TlsOnlySocketFactory(sslcontext.getSocketFactory(),
                compatible);
        HttpsURLConnection.setDefaultSSLSocketFactory(tlsOnly);
        if (proxy != null) {
            return (HttpURLConnection) url.openConnection(proxy);
        } else {
            return (HttpURLConnection) url.openConnection();
        }
    }
}
