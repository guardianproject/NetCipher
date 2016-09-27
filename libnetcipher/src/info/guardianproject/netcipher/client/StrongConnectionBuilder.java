/*
 * Copyright (c) 2016 CommonsWare, LLC
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

package info.guardianproject.netcipher.client;

import android.content.Context;
import android.content.Intent;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * Builds an HttpUrlConnection that connects via Tor through
 * Orbot.
 */
public class StrongConnectionBuilder
        extends StrongBuilderBase<StrongConnectionBuilder, HttpURLConnection> {
    private URL url;

    /**
     * Creates a StrongConnectionBuilder using the strongest set
     * of options for security. Use this if the strongest set of
     * options is what you want; otherwise, create a
     * builder via the constructor and configure it as you see fit.
     *
     * @param context any Context will do
     * @return a configured StrongConnectionBuilder
     * @throws Exception
     */
    static public StrongConnectionBuilder forMaxSecurity(Context context)
            throws Exception {
        return (new StrongConnectionBuilder(context)
                .withBestProxy());
    }

    /**
     * Creates a builder instance.
     *
     * @param context any Context will do; builder will hold onto
     *                Application context
     */
    public StrongConnectionBuilder(Context context) {
        super(context);
    }

    /**
     * Copy constructor.
     *
     * @param original builder to clone
     */
    public StrongConnectionBuilder(StrongConnectionBuilder original) {
        super(original);
        this.url = original.url;
    }
/*
  public boolean supportsSocksProxy() {
    return(false);
  }
*/

    /**
     * Sets the URL to build a connection for.
     *
     * @param url the URL
     * @return the builder
     * @throws MalformedURLException
     */
    public StrongConnectionBuilder connectTo(String url)
            throws MalformedURLException {
        connectTo(new URL(url));

        return (this);
    }

    /**
     * Sets the URL to build a connection for.
     *
     * @param url the URL
     * @return the builder
     */
    public StrongConnectionBuilder connectTo(URL url) {
        this.url = url;

        return (this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpURLConnection build(Intent status) throws IOException {
        return (buildForUrl(status, url));
    }

    @Override
    protected String get(Intent status, HttpURLConnection connection,
                         String url) throws Exception {
        HttpURLConnection realConnection = buildForUrl(status, new URL(url));

        return (slurp(realConnection.getInputStream()));
    }

    private HttpURLConnection buildForUrl(Intent status, URL urlToUse)
            throws IOException {
        URLConnection result;
        Proxy proxy = buildProxy(status);

        if (proxy == null) {
            result = urlToUse.openConnection();
        } else {
            result = urlToUse.openConnection(proxy);
        }

        if (result instanceof HttpsURLConnection && sslContext != null) {
            SSLSocketFactory tlsOnly = buildSocketFactory();
            HttpsURLConnection https = (HttpsURLConnection) result;

            https.setSSLSocketFactory(tlsOnly);
        }

        return ((HttpURLConnection) result);
    }

    // based on http://stackoverflow.com/a/309718/115145

    public static String slurp(final InputStream is)
            throws IOException {
        final char[] buffer = new char[128];
        final StringBuilder out = new StringBuilder();
        final Reader in = new InputStreamReader(is, "UTF-8");

        for (; ; ) {
            int rsz = in.read(buffer, 0, buffer.length);
            if (rsz < 0)
                break;
            out.append(buffer, 0, rsz);
        }

        in.close();

        return out.toString();
    }
}
