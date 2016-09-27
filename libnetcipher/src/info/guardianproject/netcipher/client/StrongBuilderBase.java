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

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import info.guardianproject.netcipher.proxy.OrbotHelper;

/**
 * Builds an HttpUrlConnection that connects via Tor through
 * Orbot.
 */
abstract public class
  StrongBuilderBase<T extends StrongBuilderBase, C>
  implements StrongBuilder<T, C> {
  /**
   * Performs an HTTP GET request using the supplied connection
   * to a supplied URL, returning the String response or
   * throws an Exception (e.g., cannot reach the server).
   * This is used as part of validating the Tor connection.
   *
   * @param status the status Intent we got back from Orbot
   * @param connection a connection of the type for the builder
   * @param url an public Web page
   * @return the String response from the GET request
   */
  abstract protected String get(Intent status, C connection, String url)
    throws Exception;

  final static String TOR_CHECK_URL="https://check.torproject.org/api/ip";
  private final static String PROXY_HOST="127.0.0.1";
  protected final Context context;
  protected Proxy.Type proxyType;
  protected SSLContext sslContext=null;
  protected boolean useWeakCiphers=false;
  protected boolean validateTor=false;

  /**
   * Standard constructor.
   *
   * @param context any Context will do; the StrongBuilderBase
   *             will hold onto the Application singleton
   */
  public StrongBuilderBase(Context context) {
    this.context=context.getApplicationContext();
  }

  /**
   * Copy constructor.
   *
   * @param original builder to clone
   */
  public StrongBuilderBase(StrongBuilderBase original) {
    this.context=original.context;
    this.proxyType=original.proxyType;
    this.sslContext=original.sslContext;
    this.useWeakCiphers=original.useWeakCiphers;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T withBestProxy() {
    if (supportsSocksProxy()) {
      return(withSocksProxy());
    }
    else {
      return(withHttpProxy());
    }
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
  public T withHttpProxy() {
    proxyType=Proxy.Type.HTTP;

    return((T)this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean supportsSocksProxy() {
    return(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T withSocksProxy() {
    proxyType=Proxy.Type.SOCKS;

    return((T)this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T withTrustManagers(TrustManager[] trustManagers)
    throws NoSuchAlgorithmException, KeyManagementException {

    sslContext=SSLContext.getInstance("TLSv1");
    sslContext.init(null, trustManagers, null);

    return((T)this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T withWeakCiphers() {
    useWeakCiphers=true;

    return((T)this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T withTorValidation() {
    validateTor=true;

    return((T)this);
  }

  public SSLContext getSSLContext() {
    return(sslContext);
  }

  public int getSocksPort(Intent status) {
    if (status.getStringExtra(OrbotHelper.EXTRA_STATUS)
      .equals(OrbotHelper.STATUS_ON)) {
      return(status.getIntExtra(OrbotHelper.EXTRA_PROXY_PORT_SOCKS,
        9050));
    }

    return(-1);
  }

  public int getHttpPort(Intent status) {
    if (status.getStringExtra(OrbotHelper.EXTRA_STATUS)
      .equals(OrbotHelper.STATUS_ON)) {
      return(status.getIntExtra(OrbotHelper.EXTRA_PROXY_PORT_HTTP,
        8118));
    }

    return(-1);
  }

  protected SSLSocketFactory buildSocketFactory() {
    if (sslContext==null) {
      return(null);
    }

    SSLSocketFactory result=
      new TlsOnlySocketFactory(sslContext.getSocketFactory(),
        useWeakCiphers);

    return(result);
  }

  public Proxy buildProxy(Intent status) {
    Proxy result=null;

    if (status.getStringExtra(OrbotHelper.EXTRA_STATUS)
      .equals(OrbotHelper.STATUS_ON)) {
      if (proxyType==Proxy.Type.SOCKS) {
        result=new Proxy(Proxy.Type.SOCKS,
          new InetSocketAddress(PROXY_HOST, getSocksPort(status)));
      }
      else if (proxyType==Proxy.Type.HTTP) {
        result=new Proxy(Proxy.Type.HTTP,
          new InetSocketAddress(PROXY_HOST, getHttpPort(status)));
      }
    }

    return(result);
  }

  @Override
  public void build(final Callback<C> callback) {
    OrbotHelper.get(context).addStatusCallback(
      new OrbotHelper.SimpleStatusCallback() {
        @Override
        public void onEnabled(Intent statusIntent) {
          OrbotHelper.get(context).removeStatusCallback(this);

          try {
            C connection=build(statusIntent);

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
        public void onNotYetInstalled() {
          OrbotHelper.get(context).removeStatusCallback(this);
          callback.onTimeout();
        }

        @Override
        public void onStatusTimeout() {
          OrbotHelper.get(context).removeStatusCallback(this);
          callback.onTimeout();
        }
      });
  }

  protected void checkTor(final Callback<C> callback, final Intent status,
                        final C connection) {
    new Thread() {
      @Override
      public void run() {
        try {
          String result=get(status, connection, TOR_CHECK_URL);
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
}
