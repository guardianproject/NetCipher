/*
 * Copyright (c) 2018 Michael Pöhn
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

package sample.netcipher.webviewclient;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;


public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webview);
        final TextView status = (TextView) findViewById(R.id.status);
        GenericWebViewClient webViewClient = new GenericWebViewClient(this);

        webViewClient.setRequestCounterListener(new GenericWebViewClient.RequestCounterListener() {
            @Override
            public void countChanged(int requestCount) {
                status.setText("request count: " + requestCount);
            }
        });

        webView.setWebViewClient(webViewClient);
        webView.loadUrl("https://guardianproject.info/code/netcipher/");
    }

}
