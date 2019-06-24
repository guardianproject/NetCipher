/*
 * Copyright 2014-2016 Hans-Christoph Steiner
 * Copyright 2012-2016 Nathan Freitas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.guardianproject.netcipher;

import android.app.Application;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import info.guardianproject.netcipher.client.StrongBuilderBase;
import info.guardianproject.netcipher.client.TlsOnlySocketFactory;
import info.guardianproject.netcipher.proxy.NetCipherURLStreamHandlerFactory;
import info.guardianproject.netcipher.proxy.OrbotHelper;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class NetCipher {
    private static final String TAG = "NetCipher";

    private NetCipher() {
        // this is a utility class with only static methods
    }

    public final static Proxy ORBOT_HTTP_PROXY = new Proxy(Proxy.Type.HTTP,
            new InetSocketAddress("127.0.0.1", OrbotHelper.DEFAULT_PROXY_HTTP_PORT));
    public final static Proxy ORBOT_SOCKS_PROXY = new Proxy(Proxy.Type.SOCKS,
            new InetSocketAddress("127.0.0.1", OrbotHelper.DEFAULT_PROXY_SOCKS_PORT));

    private static Proxy proxy;

    /**
     * Set the global HTTP proxy for all new {@link HttpURLConnection}s and
     * {@link HttpsURLConnection}s that are created after this is called.
     * <p>
     * {@link #useTor()} will override this setting.  Traffic must be directed
     * to Tor using the proxy settings, and Orbot has its own proxy settings
     * for connections that need proxies to work.  So if "use Tor" is enabled,
     * as tested by looking for the static instance of Proxy, then no other
     * proxy settings are allowed to override the current Tor proxy.
     *
     * @param host the IP address for the HTTP proxy to use globally
     * @param port the port number for the HTTP proxy to use globally
     * @see #setProxy(Proxy)
     * @see #clearProxy()
     */
    public static void setProxy(String host, int port) {
        if (!TextUtils.isEmpty(host) && port > 0) {
            InetSocketAddress isa = new InetSocketAddress(host, port);
            setProxy(new Proxy(Proxy.Type.HTTP, isa));
        } else if (NetCipher.proxy != ORBOT_HTTP_PROXY) {
            setProxy(null);
        }
    }

    /**
     * Set the global HTTP proxy for all new {@link HttpURLConnection}s and
     * {@link HttpsURLConnection}s that are created after this is called.
     * <p>
     * {@link #useTor()} will override this setting.  Traffic must be directed
     * to Tor using the proxy settings, and Orbot has its own proxy settings
     * for connections that need proxies to work.  So if "use Tor" is enabled,
     * as tested by looking for the static instance of Proxy, then no other
     * proxy settings are allowed to override the current Tor proxy.
     *
     * @param proxy the HTTP proxy to use globally
     * @see #setProxy(String, int)
     * @see #clearProxy()
     */
    public static void setProxy(Proxy proxy) {
        if (proxy != null && NetCipher.proxy == ORBOT_HTTP_PROXY) {
            Log.w(TAG, "useTor is enabled, ignoring new proxy settings!");
        } else {
            NetCipher.proxy = proxy;
        }
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
     * Clear the global HTTP proxy for all new {@link HttpURLConnection}s and
     * {@link HttpsURLConnection}s that are created after this is called. This
     * returns things to the default, proxy-less state.
     */
    public static void clearProxy() {
        setProxy(null);
    }

    /**
     * Set Orbot as the global HTTP proxy for all new {@link HttpURLConnection}
     * s and {@link HttpsURLConnection}s that are created after this is called.
     * This overrides all future calls to {@link #setProxy(Proxy)}, except to
     * clear the proxy, e.g. {@code #setProxy(null)} or {@link #clearProxy()}.
     * <p>
     * Traffic must be directed to Tor using the proxy settings, and Orbot has its
     * own proxy settings for connections that need proxies to work.  So if "use
     * Tor" is enabled, as tested by looking for the static instance of Proxy,
     * then no other proxy settings are allowed to override the current Tor proxy.
     *
     * @see #clearProxy()
     * @see #useGlobalProxy()
     */
    public static void useTor() {
        if (Build.VERSION.SDK_INT < 24) {
            setProxy(ORBOT_HTTP_PROXY);
        } else {
            setProxy(ORBOT_SOCKS_PROXY);
        }
    }

    /**
     * Makes a connection to {@code check.torproject.org} to read its results
     * of whether the connection came via Tor or not.
     *
     * @return true if {@code check.torproject.org} says connection is via Tor, false if not or on error
     * @see <a href="https://check.torproject.org">check.torproject.org</a>
     */
    @RequiresApi(api = 11)
    public static boolean isURLConnectionUsingTor() {
        if (Build.VERSION.SDK_INT < 11) {
            throw new UnsupportedOperationException("only works on android-11 or higher");
        }

        try {
            URL url = new URL(StrongBuilderBase.TOR_CHECK_URL);
            return checkIsTor(url.openConnection());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @RequiresApi(api = 11)
    public static boolean isNetCipherGetHttpURLConnectionUsingTor() {
        if (Build.VERSION.SDK_INT < 11) {
            throw new UnsupportedOperationException("only works on android-11 or higher");
        }

        try {
            URL url = new URL(StrongBuilderBase.TOR_CHECK_URL);
            return checkIsTor(NetCipher.getHttpURLConnection(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @RequiresApi(api = 11)
    private static boolean checkIsTor(URLConnection connection) throws IOException {
        boolean isTor = false;
        JsonReader jsonReader = new JsonReader(new InputStreamReader(connection.getInputStream()));
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();
            if ("IsTor".equals(name)) {
                isTor = jsonReader.nextBoolean();
                break;
            } else {
                jsonReader.skipValue();
            }
        }
        return isTor;
    }

    /**
     * Call this method in {@link Application#onCreate()} to enable NetCipher
     * to control the proxying.  This only works on
     * {@link Build.VERSION_CODES#O Android 8.0 Oreo} or newer. There needs to
     * be a separate call to {@link #setProxy(Proxy)} or {@link #useTor()} for
     * proxying to actually be enabled.  {@link #clearProxy()} will then remove
     * the proxying when the global proxy control is in place, but the
     * {@link URLStreamHandlerFactory} will stay in place until app restart.
     *
     * @see #useTor()
     * @see #setProxy(Proxy)
     * @see #setProxy(String, int)
     * @see #clearProxy()
     * @see URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)
     */
    @RequiresApi(api = 26)
    public static void useGlobalProxy() {
        if (Build.VERSION.SDK_INT < 26) {
            throw new UnsupportedOperationException("only works on Android 8.0 (26) or higher");
        }
        URL.setURLStreamHandlerFactory(new NetCipherURLStreamHandlerFactory());
    }

    /**
     * This is the same as {@link #useGlobalProxy()} except that it can run on
     * Android 7.x (SDK 24 and 25).  The global proxying leaks DNS on Android 7.x,
     * so this is not suitable for a privacy proxy.  It will make access proxying
     * work.  It can also be used as a failsafe to help prevent leaks when the
     * proxying is configured per-connection.
     *
     * @see #useGlobalProxy()
     * @see #useTor()
     * @see #setProxy(Proxy)
     * @see #setProxy(String, int)
     * @see #clearProxy()
     * @see URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)
     */
    @Deprecated
    @RequiresApi(api = 24)
    public static void useGlobalProxyWithDNSLeaksOnAndroid7x() {
        if (Build.VERSION.SDK_INT >= 26) {
            useGlobalProxy();
            return;
        }
        if (Build.VERSION.SDK_INT < 24) {
            throw new UnsupportedOperationException("only works on Android 7.0 (24) or higher");
        }
        Log.w(TAG, "Android 7.x fails to globally proxy DNS! DNS will leak and .onion addresses will always fail!");
        URL.setURLStreamHandlerFactory(new NetCipherURLStreamHandlerFactory());
    }

    /**
     * Get a {@link TlsOnlySocketFactory} from NetCipher.
     *
     * @see HttpsURLConnection#setDefaultSSLSocketFactory(SSLSocketFactory)
     */
    public static TlsOnlySocketFactory getTlsOnlySocketFactory() {
        return getTlsOnlySocketFactory(false);
    }

    /**
     * Get a {@link TlsOnlySocketFactory} from NetCipher, and specify whether
     * it should use a more compatible, but less strong, suite of ciphers.
     *
     * @see HttpsURLConnection#setDefaultSSLSocketFactory(SSLSocketFactory)
     */
    public static TlsOnlySocketFactory getTlsOnlySocketFactory(boolean compatible) {
        SSLContext sslcontext;
        try {
            sslcontext = SSLContext.getInstance(TlsOnlySocketFactory.TLSV1);
            sslcontext.init(null, null, null);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        } catch (KeyManagementException e) {
            throw new IllegalArgumentException(e);
        }
        return new TlsOnlySocketFactory(sslcontext.getSocketFactory(), compatible);
    }

    /**
     * Get a {@link HttpURLConnection} from a {@link URL}, and specify whether
     * it should use a more compatible, but less strong, suite of ciphers.
     * <p>
     * If {@link #useGlobalProxy()} is called, this method will use the global
     * proxy settings.  For {@code .onion} addresses, this will still directly
     * configure the proxy, but that should be the same exact settings.
     *
     * @param url
     * @param compatible
     * @return the {@code url} in an instance of {@link HttpURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect
     */
    public static HttpURLConnection getHttpURLConnection(URL url, boolean compatible)
            throws IOException {
        // .onion addresses only work via Tor, so force Tor for all of them
        Proxy proxy = NetCipher.proxy;
        if (OrbotHelper.isOnionAddress(url)) {
            if (Build.VERSION.SDK_INT < 24) {
                proxy = ORBOT_HTTP_PROXY;
            } else {
                proxy = ORBOT_SOCKS_PROXY;
            }
        }

        HttpURLConnection connection;
        if (proxy != null) {
            connection = (HttpURLConnection) url.openConnection(proxy);
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }

        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConnection = ((HttpsURLConnection) connection);
            SSLSocketFactory tlsOnly = getTlsOnlySocketFactory(compatible);
            httpsConnection.setSSLSocketFactory(tlsOnly);
            if (Build.VERSION.SDK_INT < 16) {
                httpsConnection.setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
            }
        }
        return connection;
    }

    /**
     * Get a {@link HttpsURLConnection} from a URL {@link String} using the best
     * TLS configuration available on the device.
     *
     * @param urlString
     * @return the URL in an instance of {@link HttpsURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect,
     *                                  or if an HTTP URL is given that does not support HTTPS
     */
    public static HttpsURLConnection getHttpsURLConnection(String urlString) throws IOException {
        URL url = new URL(urlString.replaceFirst("^[Hh][Tt][Tt][Pp]:", "https:"));
        return getHttpsURLConnection(url, false);
    }

    /**
     * Get a {@link HttpsURLConnection} from a {@link Uri} using the best TLS
     * configuration available on the device.
     *
     * @param uri
     * @return the {@code uri} in an instance of {@link HttpsURLConnection}
     * @throws IOException
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect,
     *                                  or if an HTTP URL is given that does not support HTTPS
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
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect,
     *                                  or if an HTTP URL is given that does not support HTTPS
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
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect,
     *                                  or if an HTTP URL is given that does not support HTTPS
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
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect,
     *                                  or if an HTTP URL is given that does not support HTTPS
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
     * @throws IllegalArgumentException if the proxy or TLS setup is incorrect,
     *                                  or if an HTTP URL is given that does not support HTTPS
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
}
