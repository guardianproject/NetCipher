
package info.guardianproject.netcipher;

import android.test.InstrumentationTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;

import javax.net.ssl.HttpsURLConnection;

public class HttpURLConnectionTest extends InstrumentationTestCase {

    private static final String HTTP_URL_STRING = "http://127.0.0.1:";
    private static final String HTTPS_URL_STRING = "https://127.0.0.1:";

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

    public void testCannotConnectHttp() throws MalformedURLException {
        try {
            HttpURLConnection http = NetCipher.getHttpURLConnection(new URL(
                    "http://127.0.0.1:63453"));
            http.setConnectTimeout(1000);
            http.connect();
            fail();
        } catch (IOException e) {
            // this should not connect
        }
    }

    public void testCannotConnectHttps() throws MalformedURLException, KeyManagementException {
        // TODO test connecting to http://
        // TODO test connecting to non-HTTPS port
        try {
            HttpsURLConnection https = NetCipher.getHttpsURLConnection(new URL(
                    "https://127.0.0.1:63453"));
            https.setConnectTimeout(1000);
            https.connect();
            fail();
        } catch (IOException e) {
            // this should not connect
        }
    }

    public void testConnectHttps() throws MalformedURLException, IOException,
            KeyManagementException {
        String[] hosts = {
                "www.google.com",
                "firstlook.org",
        };
        for (String host : hosts) {
            URL url = new URL("https://" + host);
            HttpsURLConnection https = NetCipher.getHttpsURLConnection(url);
            https.connect();
            https.disconnect();
        }
    }
}
