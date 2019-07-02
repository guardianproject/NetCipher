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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.URL;
import java.net.UnknownHostException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * This test suite can break others because it calls{@link NetCipher#useGlobalProxy()},
 * which uses {@link URL#setURLStreamHandlerFactory(java.net.URLStreamHandlerFactory)}.
 * There is no way to reset the {@link java.net.URLStreamHandlerFactory} after that has
 * been called, except for restarting the JVM.
 */
@RunWith(AndroidJUnit4.class)
public class UseGlobalProxyTest {

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

    /**
     * Using {@link NetCipher#useGlobalProxy()} leaks DNS on Android 7.x, but
     * the proxying still works.  Without proxied DNS, it is not possible to
     * connect to {@code .onion} addresses.
     */
    @Test
    public void testUseGlobalProxyWithDNSLeaksOnAndroid7x() throws Exception {
        Assume.assumeTrue("Only works on Android 7.1.2 or higher", Build.VERSION.SDK_INT >= 24);
        if (!HttpURLConnectionTest.canUseHostTorSocks()) try {
            new ServerSocket(OrbotHelper.DEFAULT_PROXY_SOCKS_PORT).close();
            Assume.assumeTrue("Requires either Orbot running in emulator or tor on host", false);
        } catch (IOException e) {
            // ignored
        }

        NetCipher.useGlobalProxyWithDNSLeaksOnAndroid7x();
        assertFalse("should not be running over Tor yet", NetCipher.isURLConnectionUsingTor());
        NetCipher.useTor();
        assertTrue("should be running over Tor", NetCipher.isURLConnectionUsingTor());
        NetCipher.clearProxy();
        assertFalse("should no longer be running over Tor", NetCipher.isURLConnectionUsingTor());
    }

    @Test
    public void testUseGlobalProxy() throws Exception {
        Assume.assumeTrue("Only works on Android 8.0 or higher", Build.VERSION.SDK_INT >= 26);
        if (!HttpURLConnectionTest.canUseHostTorSocks()) try {
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
        Assume.assumeTrue("Only works on Android 8.0 or higher", Build.VERSION.SDK_INT >= 26);
        if (!HttpURLConnectionTest.canUseHostTorSocks()) try {
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
}
