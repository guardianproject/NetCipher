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

import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import info.guardianproject.netcipher.client.TlsOnlySocketFactory;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class HttpURLConnectionTest {

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
                "yahoo.com",
                "www.yandex.ru",
                "openstreetmap.org",
                "f-droid.org",
                "web.wechat.com",
                "mirrors.kernel.org",
                "www.google.com",
                "glympse.com",
                // uses SNI
                "firstlook.org",
                "guardianproject.info",
        };
        prefetchDns(hosts);
        // reset the default SSLSocketFactory, since it is global
        SSLContext sslcontext = SSLContext.getInstance("TLSv1");
        sslcontext.init(null, null, null); // null means use default
        HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
        for (String host : hosts) {
            URL url = new URL("https://" + host);
            System.out.println("default " + url + " =================================");
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            SSLSocketFactory sslSocketFactory = connection.getSSLSocketFactory();
            assertFalse(sslSocketFactory instanceof TlsOnlySocketFactory);
            connection.setConnectTimeout(0); // blocking connect with TCP timeout
            connection.setReadTimeout(20000);
            connection.getContent();
            assertEquals(200, connection.getResponseCode());
            assertEquals("text/html", connection.getContentType().split(";")[0]);
            System.out.println(host + " " + connection.getCipherSuite());
            connection.disconnect();
        }
    }

    @Test
    public void testConnectHttps()
            throws MalformedURLException, IOException, KeyManagementException {
        String[] hosts = {
                "yahoo.com",
                "www.yandex.ru",
                "openstreetmap.org",
                "f-droid.org",
                "web.wechat.com",
                "mirrors.kernel.org",
                "www.google.com",
                "glympse.com",
                // uses SNI
                "firstlook.org",
                "guardianproject.info",
        };
        prefetchDns(hosts);
        for (String host : hosts) {
            URL url = new URL("https://" + host);
            System.out.println("netcipher " + url + " =================================");
            HttpsURLConnection connection = NetCipher.getHttpsURLConnection(url);
            connection.setConnectTimeout(0); // blocking connect with TCP timeout
            connection.setReadTimeout(20000);
            SSLSocketFactory sslSocketFactory = connection.getSSLSocketFactory();
            assertTrue(sslSocketFactory instanceof TlsOnlySocketFactory);
            connection.getContent();
            assertEquals(200, connection.getResponseCode());
            assertEquals("text/html", connection.getContentType().split(";")[0]);
            System.out.println(host + " " + connection.getCipherSuite());
            connection.disconnect();
        }
    }

    @Test
    public void testConnectOutdatedHttps()
            throws MalformedURLException, IOException, KeyManagementException, InterruptedException {
        String[] hosts = {
                // these are here to make sure it works with good servers too
                "yahoo.com",
                "www.yandex.ru",
                "openstreetmap.org",
                "f-droid.org",
                "web.wechat.com",
                "www.google.com",
                // uses SNI
                "firstlook.org",
                "guardianproject.info",
        };
        prefetchDns(hosts);
        for (String host : hosts) {
            URL url = new URL("https://" + host);
            System.out.println("outdated " + url + " =================================");
            HttpsURLConnection connection = NetCipher.getCompatibleHttpsURLConnection(url);
            connection.setConnectTimeout(0); // blocking connect with TCP timeout
            connection.setReadTimeout(20000);
            SSLSocketFactory sslSocketFactory = connection.getSSLSocketFactory();
            assertTrue(sslSocketFactory instanceof TlsOnlySocketFactory);
            connection.getContent();
            assertEquals(200, connection.getResponseCode());
            assertEquals("text/html", connection.getContentType().split(";")[0]);
            System.out.println(host + " " + connection.getCipherSuite());
            connection.disconnect();
        }
    }

    @Test
    public void testConnectBadSslCom()
            throws MalformedURLException, IOException, KeyManagementException, InterruptedException {
        String[] hosts = {
                "wrong.host.badssl.com",
        };
        prefetchDns(hosts);
        for (String host : hosts) {
            URL url = new URL("https://" + host);
            System.out.println("badssl " + url + " =================================");
            HttpsURLConnection connection = NetCipher.getHttpsURLConnection(url);
            connection.setConnectTimeout(0); // blocking connect with TCP timeout
            connection.setReadTimeout(20000);
            SSLSocketFactory sslSocketFactory = connection.getSSLSocketFactory();
            assertTrue(sslSocketFactory instanceof TlsOnlySocketFactory);
            try {
                connection.getContent();
                System.out.println("This should not have connected, it has BAD SSL!");
                fail();
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
}
