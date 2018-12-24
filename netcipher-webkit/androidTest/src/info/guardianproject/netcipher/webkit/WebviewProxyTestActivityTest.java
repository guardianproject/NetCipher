package info.guardianproject.netcipher.webkit;

import android.os.Looper;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isJavascriptEnabled;
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

        activityTestRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.evaluateJavascript(
                    "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
                    new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String html) {
                            Log.d("###", "HTML: " + html);
                        }
                    }
                );
            }
        });
    }

}