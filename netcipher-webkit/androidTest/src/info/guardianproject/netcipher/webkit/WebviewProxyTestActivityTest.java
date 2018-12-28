package info.guardianproject.netcipher.webkit;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import fi.iki.elonen.NanoHTTPD;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WebviewProxyTestActivityTest {

    public static final String GOOD_BODY = "<html><head></head><body><h1>good</h1></body></html>";
    public static final int TEST_WEB_SERVER_PORT = 18488;

    static TestWebServer testWebServer = null;

    public static class TestWebServer extends NanoHTTPD {
        public TestWebServer() throws IOException {
            super(TEST_WEB_SERVER_PORT);
        }

        @Override
        public Response serve(IHTTPSession session) {
            Map<String, String> params = session.getParms();
            return newFixedLengthResponse(GOOD_BODY);
        }
    }

    @Rule
    public ActivityTestRule<WebviewProxyTestActivity> activityTestRule = new ActivityTestRule<>(WebviewProxyTestActivity.class);

    @BeforeClass
    public static void beforeClass() throws IOException {
        // start local web server
        testWebServer = new TestWebServer();
        testWebServer.start();
    }

    @AfterClass
    public static void afterClass() {
        // terminate local web server
        testWebServer.stop();
    }

    @Test
    public void testWebviewContent() throws InterruptedException {

        int webviewId = activityTestRule.getActivity().getWebViewId();
        onView(withId(webviewId))
                .perform(click())
                .check(matches(isAssignableFrom(WebView.class)));

        final WebView webView = (WebView) activityTestRule.getActivity().findViewById(webviewId);
        loadUrl(webView, String.format("http://localhost:%d", TEST_WEB_SERVER_PORT));
        String html = getHtml(webView);

        Assert.assertNotNull(html);
        Assert.assertEquals(GOOD_BODY, html);
    }

    private void loadUrl(final WebView webView, final String url) throws InterruptedException {
        activityTestRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);

            }
        });
        // give the webview a moment to load the page
        // probably not ideal but far more straight forward than
        // doing a complex WebViewClient setup
        Thread.sleep(100);
    }

    private String getHtml(final WebView webView) throws InterruptedException {
        long timeout = System.currentTimeMillis() + (5 * 1000);
        final AtomicReference<String> htmlContent = new AtomicReference<>(null);
        activityTestRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.evaluateJavascript(
                        "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
                        new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String html) {
                                // fix weird unicode style LT
                                html = html.replace("\\u003C", "<");
                                // remote encapsulating quotes if present
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

        // wait until content is ready (or timeout via @Test)
        while (htmlContent.get() == null && timeout > System.currentTimeMillis()) {
            Thread.sleep(50);
        }

        return htmlContent.get();
    }
}