/*
 * Copyright (c) 2019 Michael Pöhn
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

package sample.netcipher.conscrypt;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends Activity {

    private ProgressBar progress;
    private Button button;
    private TextView msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        progress = findViewById(R.id.progress);
        button = findViewById(R.id.button);
        msg = findViewById(R.id.msg);

        button.setOnClickListener(onButtonClicked);
        displayStateInitial();
    }

    private final View.OnClickListener onButtonClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            displayStateRequesting();

            new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        URL u = new URL("https://example.com");
                        URLConnection c = u.openConnection();
                        if (!(c instanceof HttpsURLConnection)){
                            throw new RuntimeException("expected HttpsURLConnection but encountered:" + c.getClass().getSimpleName());
                        }
                        HttpsURLConnection hc = (HttpsURLConnection) c;
                        hc.connect();
                        String socketFactoryName = hc.getSSLSocketFactory().getClass().getSimpleName();
                        int httpStatus = hc.getResponseCode();

                        if (httpStatus != 200) {
                            throw new RuntimeException("expected http-status 200 but encountered: " + httpStatus + "\nSocketFactory: " + socketFactoryName);
                        }

                        displayStateReuquestOkay(socketFactoryName);

                    } catch (MalformedURLException e) {
                        displayStateRequestFailed(e);
                    } catch (IOException e) {
                        displayStateRequestFailed(e);
                    } catch (RuntimeException e) {
                        displayStateRequestFailed(e);
                    }

                    return null;
                }
            }.execute();
        }
    };

    void displayStateInitial() {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                progress.setVisibility(View.GONE);
                button.setVisibility(View.VISIBLE);
                msg.setVisibility(View.GONE);
                button.setText("do request");
            }
        });
    }

    void displayStateRequesting() {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                progress.setVisibility(View.VISIBLE);
                button.setVisibility(View.GONE);
                msg.setVisibility(View.VISIBLE);
                msg.setText("requesting https://example.com ...");
            }
        });
    }

    void displayStateRequestFailed(final Throwable e) {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                progress.setVisibility(View.GONE);
                button.setVisibility(View.VISIBLE);
                msg.setVisibility(View.VISIBLE);
                msg.setText("request failed\n\n" + e.getMessage());
                button.setText("re-request");
                Log.e("###", e.getMessage(), e);
            }
        });
    }

    void displayStateReuquestOkay(final String socketFactoryName) {
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                progress.setVisibility(View.GONE);
                button.setVisibility(View.VISIBLE);
                msg.setVisibility(View.VISIBLE);
                msg.setText("request okay\n(used: " + socketFactoryName + ")");
                button.setText("re-request");
            }
        });
    }
}
