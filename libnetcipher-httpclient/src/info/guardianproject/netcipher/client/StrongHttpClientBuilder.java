/*
 * Copyright 2012-2016 Nathan Freitas
 * Copyright 2015 str4d
 * Portions Copyright (c) 2016 CommonsWare, LLC
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
import org.json.JSONObject;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import cz.msebera.android.httpclient.HttpHost;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.config.Registry;
import cz.msebera.android.httpclient.config.RegistryBuilder;
import cz.msebera.android.httpclient.conn.HttpClientConnectionManager;
import cz.msebera.android.httpclient.conn.socket.ConnectionSocketFactory;
import cz.msebera.android.httpclient.conn.socket.PlainConnectionSocketFactory;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.impl.conn.PoolingHttpClientConnectionManager;
import info.guardianproject.netcipher.proxy.OrbotHelper;

/**
 * Subclass of HttpClientBuilder that adds configuration
 * options and defaults for NetCipher, improving the security
 * of socket connections.
 */
public class StrongHttpClientBuilder extends HttpClientBuilder
  implements StrongBuilder<StrongHttpClientBuilder, HttpClient> {
  final static String PROXY_HOST="127.0.0.1";
  private Simple netCipher;
  private final Context ctxt;
  private boolean validateTor=false;

  /**
   * Creates a StrongHttpClientBuilder using the strongest set
   * of options for security. Use this if the strongest set of
   * options is what you want; otherwise, create a
   * builder via the constructor and configure it as you see fit.
   *
   * @param ctxt any Context will do
   * @return a configured StrongHttpClientBuilder
   * @throws Exception
   */
  static public StrongHttpClientBuilder forMaxSecurity(Context ctxt)
    throws Exception {
    return(new StrongHttpClientBuilder(ctxt));
  }

  /**
   * Standard constructor
   *
   * @param ctxt any Context will do; we hold onto the Application
   *             singleton
   */
  public StrongHttpClientBuilder(Context ctxt) {
    this.ctxt=ctxt.getApplicationContext();
    netCipher=new Simple(ctxt);
  }

  /**
   * Copy constructor.
   *
   * @param original builder to clone
   */
  public StrongHttpClientBuilder(StrongHttpClientBuilder original) {
    this.netCipher=new Simple(original.netCipher);
    this.ctxt=original.ctxt;
  }

  @Override
  public CloseableHttpClient build() {
    throw new IllegalStateException(
      "Use a one-parameter build() method please");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public HttpClient build(Intent status) throws Exception {
    init(status);

    return(super.build());
  }

  @Override
  public void build(final Callback<HttpClient> callback) {
    OrbotHelper.get(ctxt).addStatusCallback(
      new OrbotHelper.SimpleStatusCallback() {
        @Override
        public void onEnabled(Intent statusIntent) {
          OrbotHelper.get(ctxt).removeStatusCallback(this);

          try {
            HttpClient connection=build(statusIntent);

            if (validateTor) {
              validateTor=false;
              checkTor(callback, statusIntent, connection);
            }
            else {
              callback.onConnected(connection);
            }
          }
          catch (Exception e) {
            callback.onConnectionException(e);
          }
        }

        @Override
        public void onStatusTimeout() {
          OrbotHelper.get(ctxt).removeStatusCallback(this);
          callback.onTimeout();
        }
      });
  }

  private void checkTor(final Callback<HttpClient> callback,
                        final Intent status,
                        final HttpClient connection) {
    new Thread() {
      @Override
      public void run() {
        try {
          HttpGet get=new HttpGet(StrongBuilderBase.TOR_CHECK_URL);
          String result=connection.execute(get, new BasicResponseHandler());
          JSONObject json=new JSONObject(result);

          if (json.optBoolean("IsTor", false)) {
            callback.onConnected(connection);
          }
          else {
            callback.onInvalid();
          }
        }
        catch (Exception e) {
          callback.onConnectionException(e);
        }
      }
    }.start();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StrongHttpClientBuilder withBestProxy() {
    netCipher.withBestProxy();

    return(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean supportsHttpProxy() {
    return(true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StrongHttpClientBuilder withHttpProxy() {
    netCipher.withHttpProxy();

    return(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean supportsSocksProxy() {
    return(true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StrongHttpClientBuilder withSocksProxy() {
    netCipher.withSocksProxy();

    return(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StrongHttpClientBuilder withWeakCiphers() {
    netCipher.withWeakCiphers();

    return(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StrongHttpClientBuilder withTrustManagers(TrustManager[] trustManagers)
    throws NoSuchAlgorithmException, KeyManagementException {
    netCipher.withTrustManagers(trustManagers);

    return(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StrongHttpClientBuilder withTorValidation() {
    validateTor=true;

    return(this);
  }

  protected void init(Intent status) throws Exception {
    StrongSSLSocketFactory2 sFactory;
    int socksPort=netCipher.getSocksPort(status);
    SSLContext sslContext=netCipher.getSSLContext();

    if (sslContext==null) {
      sslContext=SSLContext.getInstance("TLSv1");
      sslContext.init(null, null, null);
    }

    if (socksPort==-1) {
      int httpPort=netCipher.getHttpPort(status);

      if (httpPort!=-1) {
        setProxy(new HttpHost(PROXY_HOST, httpPort));
      }

      sFactory=
        new StrongSSLSocketFactory2(sslContext);
    }
    else {
      sFactory=
        new StrongSSLSocketFactory2(sslContext, socksPort);
    }

    setSSLSocketFactory(sFactory);

    Registry<ConnectionSocketFactory> registry=
      RegistryBuilder.<ConnectionSocketFactory>create()
        .register("http", PlainConnectionSocketFactory.getSocketFactory())
        .register("https", sFactory)
        .build();

    HttpClientConnectionManager ccm=
      new PoolingHttpClientConnectionManager(registry);

    setConnectionManager(ccm);
  }

  private static class Simple extends StrongBuilderBase<Simple, HttpClient> {
    public Simple(Context ctxt) {
      super(ctxt);
    }

    public Simple(StrongBuilderBase original) {
      super(original);
    }

    @Override
    public HttpClient build(Intent status) throws IOException {
      throw new IllegalStateException("Um, don't use this, m'kay?");
    }

    @Override
    protected String get(Intent status, HttpClient connection,
                         String url) throws Exception {
      throw new IllegalStateException("Um, don't use this, m'kay?");
    }
  }
}
