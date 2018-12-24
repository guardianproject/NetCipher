package info.guardianproject.netcipher.webkit;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

public class WebviewProxyTestActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.setId(View.generateViewId());
        setContentView(webView);
        webView.loadData("<html><h1>test</h1></html>", "text/html", "UTF-16");
        webView.getSettings().setJavaScriptEnabled(true);
    }

    public int getWebViewId() {
        return webView.getId();
    }
}
