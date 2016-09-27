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
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Builds an HttpUrlConnection that connects via Tor through
 * Orbot.
 */
public class StrongVolleyQueueBuilder extends
  StrongBuilderBase<StrongVolleyQueueBuilder, RequestQueue> {
  /**
   * Creates a StrongVolleyQueueBuilder using the strongest set
   * of options for security. Use this if the strongest set of
   * options is what you want; otherwise, create a
   * builder via the constructor and configure it as you see fit.
   *
   * @param context any Context will do
   * @return a configured StrongVolleyQueueBuilder
   * @throws Exception
   */
  static public StrongVolleyQueueBuilder forMaxSecurity(Context context)
    throws Exception {
    return(new StrongVolleyQueueBuilder(context).withBestProxy());
  }

  /**
   * Creates a builder instance.
   *
   * @param context any Context will do; builder will hold onto
   *             Application context
   */
  public StrongVolleyQueueBuilder(Context context) {
    super(context);
  }

  /**
   * Copy constructor.
   *
   * @param original builder to clone
   */
  public StrongVolleyQueueBuilder(StrongVolleyQueueBuilder original) {
    super(original);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RequestQueue build(Intent status) {
    return(Volley.newRequestQueue(context,
      new StrongHurlStack(buildSocketFactory(), buildProxy(status))));
  }

  @Override
  protected void checkTor(final Callback<RequestQueue> callback,
                          final Intent status,
                          final RequestQueue connection) {
    new Thread() {
      @Override
      public void run() {
        try {
          final StringRequest stringRequest=
            new StringRequest(StringRequest.Method.GET, TOR_CHECK_URL,
              new Response.Listener<String>() {
                @Override
                public void onResponse(String result) {
                  try {
                    JSONObject json=new JSONObject(result);

                    if (json.optBoolean("IsTor", false)) {
                      callback.onConnected(connection);
                    }
                    else {
                      callback.onInvalid();
                    }
                  }
                  catch (JSONException e) {
                    callback.onConnectionException(e);
                  }
                }
              },
              new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                  callback.onConnectionException(error);
                }
              });

          connection.add(stringRequest);
        }
        catch (Exception e) {
          callback.onConnectionException(e);
        }
      }
    }.start();
  }

  @Override
  protected String get(Intent status, RequestQueue connection,
                       String url) throws Exception {
    throw new IllegalStateException("How did you get here?");
  }
}