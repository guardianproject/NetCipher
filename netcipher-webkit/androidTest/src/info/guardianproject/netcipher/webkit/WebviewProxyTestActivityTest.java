package info.guardianproject.netcipher.webkit;

import android.os.Build;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.filters.ResponseFilter;
import net.lightbody.bmp.util.HttpMessageContents;
import net.lightbody.bmp.util.HttpMessageInfo;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import fi.iki.elonen.NanoHTTPD;
import io.netty.handler.codec.http.HttpResponse;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@LargeTest
@RunWith(AndroidJUnit4.class)
@FixMethodOrder()
public class WebviewProxyTestActivityTest {

    public static final String CONTENT_PROXIED = "<html><head></head><body><h1>pr0x13d</h1></body></html>";
    public static final String CONTENT_DIRECT = "<html><head></head><body><h1>d1r3ct</h1></body></html>";

    static TestWebServer webServer = null;
    private static BrowserMobProxyServer proxy = null;

    /**
     * Minimal embedded Web Server
     */
    public static class TestWebServer extends NanoHTTPD {
        public TestWebServer() throws IOException {
            // port = 0 means to just auto-select a random available port
            super(0);
        }

        @Override
        public Response serve(IHTTPSession session) {
            Map<String, String> params = session.getParms();
            return newFixedLengthResponse(CONTENT_DIRECT);
        }
    }



    @Rule
    public ActivityTestRule<WebviewProxyTestActivity> activityTestRule = new ActivityTestRule<>(WebviewProxyTestActivity.class);

    @BeforeClass
    public static void beforeClass() throws IOException {

        if (Build.VERSION.SDK_INT >= 19) {
            // start embedded http proxy server
            proxy = new BrowserMobProxyServer();
            // instruct proxy server to manipulate response contents
            // this will be used for assertions in the test cases
            proxy.addResponseFilter(new ResponseFilter() {
                @Override
                public void filterResponse(HttpResponse httpResponse, HttpMessageContents httpMessageContents, HttpMessageInfo httpMessageInfo) {
                    httpMessageContents.setTextContents(CONTENT_PROXIED);
                }
            });
            proxy.start();

            // start embedded web server
            webServer = new TestWebServer();
            webServer.start();
        }
    }

    @AfterClass
    public static void afterClass() {
        if (Build.VERSION.SDK_INT >= 19) {
            // terminate embedded web server
            webServer.stop();

            // terminate embedded http proxy server
            proxy.stop();
        }
    }

    /**
     * This is not testing webkit proxying, it's about making sure the test utilities are working.
     */
    @Test
    public void testWebviewContent() throws InterruptedException {

        Assume.assumeTrue("API level has to be >= 19", Build.VERSION.SDK_INT >= 19);

        int webviewId = activityTestRule.getActivity().getWebViewId();
        onView(withId(webviewId))
                .perform(click())
                .check(matches(isAssignableFrom(WebView.class)));
        final WebView webView = (WebView) activityTestRule.getActivity().findViewById(webviewId);

        loadUrl(webView, String.format("http://localhost:%d", webServer.getListeningPort()));
        String html = getHtml(webView);

        Assert.assertNotNull(html);
        Assert.assertEquals(CONTENT_DIRECT, html);
    }

    @Test
    public void testWebkitProxy() throws Exception {

        Assume.assumeTrue("API level has to be >= 19", Build.VERSION.SDK_INT >= 19);
        Assume.assumeFalse("support for API level 22 and 23 is broken, " +
                "see: https://gitlab.com/guardianproject/NetCipher/issues/1",
                Arrays.asList(22, 23).contains(Build.VERSION.SDK_INT));

        int webviewId = activityTestRule.getActivity().getWebViewId();
        onView(withId(webviewId))
                .perform(click())
                .check(matches(isAssignableFrom(WebView.class)));
        final WebView webView = (WebView) activityTestRule.getActivity().findViewById(webviewId);

        WebkitProxy.setProxy(TestApp.class.getCanonicalName(), webView.getContext().getApplicationContext(), webView, "localhost", proxy.getPort());
        loadUrl(webView, String.format("http://localhost:%d", webServer.getListeningPort()));
        String html = getHtml(webView);

        Assert.assertNotNull(html);
        Assert.assertEquals(CONTENT_PROXIED, html);
    }

    /**
     * load a url into a webview and block until the loading finished
     * NOTE: this resets any previously set WebViewClient!
     */
    private void loadUrl(final WebView webView, final String url) throws InterruptedException {

        long timeout = System.currentTimeMillis() + (5 * 1000 * TestHelper.timeoutScale());
        final AtomicBoolean finished = new AtomicBoolean(false);

        activityTestRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        finished.set(true);
                    }
                });
                webView.loadUrl(url);
            }
        });

        // wait until webview loaded or timeout
        while (!finished.get() && timeout > System.currentTimeMillis()) {
            Thread.sleep(50);
        }
    }

    /**
     * fetch content of a webview
     */
    private String getHtml(final WebView webView) throws InterruptedException {
        long timeout = System.currentTimeMillis() + (5 * 1000 * TestHelper.timeoutScale());
        final AtomicReference<String> htmlContent = new AtomicReference<>(null);
        activityTestRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.evaluateJavascript(
                        "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
                        new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String html) {
                                // fix weird unicode style '<'
                                html = html.replace("\\u003C", "<");
                                // fix weird unicode style '>'
                                html = html.replace("\\u003E", ">");
                                // remove encapsulating quotes if present
                                if (html.charAt(0) == '\"' && html.charAt(html.length()-1) == '\"') {
                                    html = html.substring(1, html.length() - 1);
                                }
                                // store reference to html content string
                                htmlContent.set(html);
                            }
                        }
                );
            }
        });

        // wait until content is ready
        while (htmlContent.get() == null && timeout > System.currentTimeMillis()) {
            Thread.sleep(50);
        }

        return htmlContent.get();
    }
}