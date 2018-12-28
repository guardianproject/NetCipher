package info.guardianproject.netcipher.webkit;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WebviewProxyTestActivityTest {

    @Rule
    public ActivityTestRule<WebviewProxyTestActivity> activityTestRule = new ActivityTestRule<>(WebviewProxyTestActivity.class);

    @Test
    public void testWebviewContent() throws InterruptedException {

        int webviewId = activityTestRule.getActivity().getWebViewId();
        onView(withId(webviewId))
                .perform(click())
                .check(matches(isAssignableFrom(WebView.class)));

        final WebView webView = (WebView) activityTestRule.getActivity().findViewById(webviewId);
        String html = getHtmlFromWebView(webView);


        Assert.assertNotNull(html);
        Assert.assertEquals("<html><head></head><body><h1>test</h1></body></html>", html);
    }

    private String getHtmlFromWebView(final WebView webView) throws InterruptedException {
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
            Thread.sleep(100);
        }

        return htmlContent.get();
    }
}