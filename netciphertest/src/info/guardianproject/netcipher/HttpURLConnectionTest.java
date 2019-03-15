/*
 * Copyright 2014-2016 Hans-Christoph Steiner
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

import android.os.Build;
import android.support.test.runner.AndroidJUnit4;
import info.guardianproject.netcipher.client.TlsOnlySocketFactory;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class HttpURLConnectionTest {
    public static final String TAG = "HttpURLConnectionTest";

    private static final String HTTP_URL_STRING = "http://127.0.0.1:";

    /**
     * Prime the DNS cache with the hosts that are used in these tests.
     */
    private void prefetchDns(String[] hosts) {
        for (final String host : hosts) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        InetAddress.getByName(host);
                    } catch (UnknownHostException e) {
                    }
                }
            }.start();
        }
    }

    private void prefetchDns(List<String> hosts) {
        prefetchDns(hosts.toArray(new String[hosts.size()]));
    }

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        clearProxySettings();
    }

    @After
    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        clearProxySettings();
    }

    /**
     * Clear all proxy settings, since they are global.
     */
    private void clearProxySettings() throws NoSuchFieldException, IllegalAccessException {
        NetCipher.clearProxy();

        if (Build.VERSION.SDK_INT >= 24) {
            // reset the system's URLStreamHandlerFactory
            Field factoryField = URL.class.getDeclaredField("factory");
            factoryField.setAccessible(true);
            factoryField.set(factoryField, null);
        }
    }

    @Test
    public void testConnectHttp() throws MalformedURLException, IOException {
        // include trailing \n in test string, otherwise it gets added anyhow
        final String content = "content!";
        final String httpResponse = "HTTP/1.1 200 OK\nContent-Type: text/plain\n\n" + content;
        final ServerSocket serverSocket = new ServerSocket(0); // auto-assign
        final int port = serverSocket.getLocalPort();
        new Thread() {
            @Override
            public void run() {
                try {
                    Socket s = serverSocket.accept();
                    OutputStream os = s.getOutputStream();
                    os.write(httpResponse.getBytes());
                    os.flush();
                    os.close();
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                }
            }
        }.start();
        HttpURLConnection connection = NetCipher.getHttpURLConnection(new URL(HTTP_URL_STRING
                + port));
        InputStream is = (InputStream) connection.getContent();
        byte buffer[] = new byte[256];
        int read = is.read(buffer);
        String msg = new String(buffer, 0, read);
        assertEquals(content, msg);
        assertEquals(200, connection.getResponseCode());
        assertEquals("text/plain", connection.getContentType());
        connection.disconnect();
    }

    @Test
    public void testCannotConnectHttp() throws MalformedURLException {
        try {
            HttpURLConnection http = NetCipher.getHttpURLConnection(new URL(
                    "http://127.0.0.1:63453"));
            http.setConnectTimeout(0); // blocking connect with TCP timeout
            http.connect();
            fail();
        } catch (IOException e) {
            // this should not connect
        }
    }

    @Test
    public void testCannotConnectHttps() throws MalformedURLException, KeyManagementException {
        // TODO test connecting to http://
        // TODO test connecting to non-HTTPS port
        try {
            HttpsURLConnection https = NetCipher.getHttpsURLConnection(new URL(
                    "https://127.0.0.1:63453"));
            https.setConnectTimeout(0); // blocking connect with TCP timeout
            https.connect();
            fail();
        } catch (IOException e) {
            // this should not connect
        }
    }

    @Test
    public void testStandardHttpURLConnection()
            throws MalformedURLException, IOException, KeyManagementException, NoSuchAlgorithmException {
        String[] hosts = {
                "www.yandex.ru",
                "openstreetmap.org",
                "f-droid.org",
                "web.wechat.com",
                "mirrors.kernel.org",
                "www.google.com",
                // uses SNI
                "check-tls.akamaized.net",
                "guardianproject.info",
                // TLS 1.3 enabled
                "enabled.tls13.com",
                "www.allizom.org",
                "www.theregister.co.uk",
        };
        prefetchDns(hosts);
        // reset the default SSLSocketFactory, since it is global
        SSLContext sslcontext = SSLContext.getInstance(TlsOnlySocketFactory.TLSV1);
        sslcontext.init(null, null, null); // null means use default
        HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
        for (String host : hosts) {
            URL url = new URL("https://" + host);
            System.out.println("default " + url + " =================================");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            SSLSocketFactory sslSocketFactory = connection.getSSLSocketFactory();
            assertFalse(sslSocketFactory instanceof TlsOnlySocketFactory);
            connection.setConnectTimeout(0); // blocking connect with TCP timeout
            connection.setReadTimeout(0);
            connection.getContent();
            assertEquals(200, connection.getResponseCode());
            assertEquals("text/html", connection.getContentType().split(";")[0]);
            System.out.println(host + " " + connection.getCipherSuite());
            assertTrue(connection.getCipherSuite().startsWith("TLS"));
            connection.disconnect();
        }
    }

    @Test
    public void testConnectHttps()
            throws MalformedURLException, IOException, KeyManagementException {
        String[] hosts = {
                "www.yandex.ru",
                "openstreetmap.org",
                "f-droid.org",
                "web.wechat.com",
                "mirrors.kernel.org",
                "www.google.com",
                "glympse.com",
                // uses SNI
                "check-tls.akamaized.net",
                "guardianproject.info",
                // TLS 1.3 enabled
                "enabled.tls13.com",
                "www.allizom.org",
                "www.theregister.co.uk",
        };
        prefetchDns(hosts);
        for (String host : hosts) {
            URL url = new URL("https://" + host);
            System.out.println("netcipher " + url + " =================================");
            HttpsURLConnection connection = NetCipher.getHttpsURLConnection(url);
            connection.setConnectTimeout(0); // blocking connect with TCP timeout
            connection.setReadTimeout(0);
            SSLSocketFactory sslSocketFactory = connection.getSSLSocketFactory();
            assertTrue(sslSocketFactory instanceof TlsOnlySocketFactory);
            connection.getContent();
            assertEquals(200, connection.getResponseCode());
            assertEquals("text/html", connection.getContentType().split(";")[0]);
            System.out.println(host + " " + connection.getCipherSuite());
            assertTrue(connection.getCipherSuite().startsWith("TLS"));
            connection.disconnect();
        }
    }

    @Test
    public void testConnectOutdatedHttps()
            throws MalformedURLException, IOException, KeyManagementException, InterruptedException {
        String[] hosts = {
                // these are here to make sure it works with good servers too
                "www.yandex.ru",
                "openstreetmap.org",
                "f-droid.org",
                "web.wechat.com",
                "www.google.com",
                // uses SNI
                "check-tls.akamaized.net",
                "guardianproject.info",
                // TLS 1.3 enabled
                "enabled.tls13.com",
                "www.allizom.org",
                "www.theregister.co.uk",
        };
        prefetchDns(hosts);
        for (String host : hosts) {
            URL url = new URL("https://" + host);
            System.out.println("outdated " + url + " =================================");
            HttpsURLConnection connection = NetCipher.getCompatibleHttpsURLConnection(url);
            connection.setConnectTimeout(0); // blocking connect with TCP timeout
            connection.setReadTimeout(0);
            SSLSocketFactory sslSocketFactory = connection.getSSLSocketFactory();
            assertTrue(sslSocketFactory instanceof TlsOnlySocketFactory);
            connection.getContent();
            assertEquals(200, connection.getResponseCode());
            assertEquals("text/html", connection.getContentType().split(";")[0]);
            System.out.println(host + " " + connection.getCipherSuite());
            assertTrue(connection.getCipherSuite().startsWith("TLS"));
            connection.disconnect();
        }
    }

    @Test
    public void testConnectBadSslCom()
            throws MalformedURLException, IOException, KeyManagementException, InterruptedException {
        ArrayList<String> hosts = new ArrayList<>(Arrays.asList(
                "wrong.host.badssl.com",
                "self-signed.badssl.com",
                "expired.badssl.com",
                "untrusted-root.badssl.com",
                "rc4.badssl.com",
                "rc4-md5.badssl.com",
                "null.badssl.com"));

        if (Build.VERSION.SDK_INT > 22) {
            hosts.add("dh480.badssl.com");
            hosts.add("dh512.badssl.com");
        }

        prefetchDns(hosts);
        for (String host : hosts) {
            URL url = new URL("https://" + host);
            System.out.println("badssl " + url + " =================================");
            HttpsURLConnection connection = NetCipher.getHttpsURLConnection(url);
            connection.setConnectTimeout(0); // blocking connect with TCP timeout
            connection.setReadTimeout(0);
            SSLSocketFactory sslSocketFactory = connection.getSSLSocketFactory();
            assertTrue("socket factory of type 'TlsOnlySocketFactory' expected", sslSocketFactory instanceof TlsOnlySocketFactory);
            try {
                connection.getContent();
                fail("This should not have connected, it has BAD SSL: " + host);
            } catch (IOException e) {
                e.printStackTrace();
                // success! these should fail!
            } finally {
                connection.disconnect();
            }
        }
    }

    @Test
    public void testDefaultSSLSocketFactory() throws IOException {
        SSLSocketFactory sslSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        assertFalse(sslSocketFactory instanceof TlsOnlySocketFactory);
        URL url = new URL("https://guardianproject.info");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        assertFalse(connection.getSSLSocketFactory() instanceof TlsOnlySocketFactory);
        connection.disconnect();

        HttpsURLConnection.setDefaultSSLSocketFactory(NetCipher.getTlsOnlySocketFactory());
        assertTrue(HttpsURLConnection.getDefaultSSLSocketFactory() instanceof TlsOnlySocketFactory);
        connection = (HttpsURLConnection) url.openConnection();
        assertTrue(connection.getSSLSocketFactory() instanceof TlsOnlySocketFactory);
        connection.disconnect();
    }

    @Test
    public void testUseGlobalProxy() throws Exception {
        Assume.assumeTrue("Only works on Android 7.1.2 or higher", Build.VERSION.SDK_INT >= 24);
        if (!canUseHostTorSocks()) try {
            new ServerSocket(OrbotHelper.DEFAULT_PROXY_SOCKS_PORT).close();
            Assume.assumeTrue("Requires either Orbot running in emulator or tor on host", false);
        } catch (IOException e) {
            // ignored
        }

        NetCipher.useGlobalProxy();
        assertFalse("should not be running over Tor yet", NetCipher.isURLConnectionUsingTor());
        NetCipher.useTor();
        assertTrue("should be running over Tor", NetCipher.isURLConnectionUsingTor());

        URL url = new URL("https://facebookcorewwwi.onion/osd.xml");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        assertTrue("https://facebookcorewwwi.onion should use TLS",
                connection.getSSLSocketFactory() instanceof TlsOnlySocketFactory);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        Document document = documentBuilder.parse(connection.getInputStream());
        assertEquals("OpenSearchDescription", document.getDocumentElement().getNodeName());
        connection.disconnect();

        NetCipher.clearProxy();
        assertFalse("should no longer be running over Tor", NetCipher.isURLConnectionUsingTor());
    }

    @Test(expected = UnknownHostException.class)
    public void testUseGlobalProxyWithoutProxy() throws Exception {
        Assume.assumeTrue("Only works on Android 7.1.2 or higher", Build.VERSION.SDK_INT >= 24);
        if (!canUseHostTorSocks()) try {
            new ServerSocket(OrbotHelper.DEFAULT_PROXY_SOCKS_PORT).close();
            Assume.assumeTrue("Requires either Orbot running in emulator or tor on host", false);
        } catch (IOException e) {
            // ignored
        }

        NetCipher.useGlobalProxy();
        URL url = new URL("https://facebookcorewwwi.onion/osd.xml");
        url.openConnection().connect();
        fail();
    }

    @Test
    public void testSocksProxyWithOnion() throws IOException {
        Assume.assumeTrue("Only works on Android 7.1.2 or higher", Build.VERSION.SDK_INT >= 24);
        if (!canUseHostTorSocks()) try {
            new ServerSocket(OrbotHelper.DEFAULT_PROXY_SOCKS_PORT).close();
            Assume.assumeTrue("Requires either Orbot running in emulator or tor on host", false);
        } catch (IOException e) {
            // ignored
        }
        assertFalse("URLConnection should not use Tor by default", NetCipher.isURLConnectionUsingTor());
        NetCipher.useTor();
        assertFalse("URLConnection should not use Tor by default", NetCipher.isURLConnectionUsingTor());
        assertTrue("NetCipher.getHttpURLConnection should use Tor",
                NetCipher.isNetCipherGetHttpURLConnectionUsingTor());
    }

    @Test(expected = ConnectException.class)
    public void testBadHttpProxyFails() throws IOException {
        final int testPort = 58273;
        try {
            new ServerSocket(testPort).close();
        } catch (IOException e) {
            fail("This assumes nothing is running on port " + testPort);
        }

        URL url = new URL("https://github.com");
        NetCipher.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", testPort)));
        HttpURLConnection connection = NetCipher.getHttpURLConnection(url);
        connection.getContent();
        fail();
    }

    @Test(expected = SocketException.class)
    public void testBadSocksProxyFails() throws IOException {
        final int testPort = 58273;
        try {
            new ServerSocket(testPort).close();
        } catch (IOException e) {
            fail("This assumes nothing is running on port " + testPort);
        }

        URL url = new URL("https://github.com");
        NetCipher.setProxy(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", testPort)));
        HttpURLConnection connection = NetCipher.getHttpURLConnection(url);
        connection.getContent();
        fail();
    }

    /**
     * @see <a href="https://developer.android.com/studio/run/emulator-networking">Set up Android Emulator networking</a>
     */
    public static boolean canUseHostTorSocks() {
        Socket s = null;
        final String emulatorSpecialAliasIp = "10.0.2.2";
        try {
            s = new Socket(emulatorSpecialAliasIp, OrbotHelper.DEFAULT_PROXY_SOCKS_PORT);
            Field field = NetCipher.class.getDeclaredField("ORBOT_SOCKS_PROXY");
            field.setAccessible(true);
            field.set(field, new Proxy(Proxy.Type.SOCKS,
                    new InetSocketAddress(emulatorSpecialAliasIp, OrbotHelper.DEFAULT_PROXY_SOCKS_PORT)));
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
