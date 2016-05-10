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

package info.guardianproject.netcipher;

import android.test.AndroidTestCase;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongConnectionBuilder;
import info.guardianproject.netcipher.proxy.OrbotHelper;

public class StrongConnectionBuilderTest extends
  AndroidTestCase {
  private static final String TEST_URL=
    "https://wares.commonsware.com/test.json";
  private static final String EXPECTED="{\"Hello\": \"world\"}";
  private static boolean initialized=false;
  private CountDownLatch responseLatch;
  private Exception innerException=null;
  private String testResult=null;

  public void setUp() {
    if (!initialized) {
      OrbotHelper.get(getContext()).init();
      initialized=true;
    }

    responseLatch=new CountDownLatch(1);
  }

  public void testDefaultHURL() throws IOException {
    testHURL(
      (HttpURLConnection)new URL(TEST_URL).openConnection());
  }

  public void testStrongConnectionBuilder()
    throws Exception {
    StrongConnectionBuilder builder=
      StrongConnectionBuilder.forMaxSecurity(getContext());

    testStrongBuilder(builder.connectTo(TEST_URL),
      new TestBuilderCallback<HttpURLConnection>() {
        @Override
        protected void loadResult(HttpURLConnection c)
          throws Exception {
          try {
            testResult=slurp(c.getInputStream());
          }
          finally {
            c.disconnect();
          }
        }
      });
  }

  private void testHURL(HttpURLConnection c) throws IOException {
    try {
      String result=slurp(c.getInputStream());

      assertEquals(EXPECTED, result);
    }
    finally {
      c.disconnect();
    }
  }

  // based on http://stackoverflow.com/a/309718/115145

  public static String slurp(final InputStream is)
    throws IOException {
    final char[] buffer = new char[128];
    final StringBuilder out = new StringBuilder();
    final Reader in = new InputStreamReader(is, "UTF-8");

    for (;;) {
      int rsz = in.read(buffer, 0, buffer.length);
      if (rsz < 0)
        break;
      out.append(buffer, 0, rsz);
    }

    return out.toString();
  }

  private void testStrongBuilder(StrongBuilder builder,
                                 TestBuilderCallback callback)
    throws Exception {
    testResult=null;
    builder.build(callback);

    assertTrue(responseLatch.await(30, TimeUnit.SECONDS));

    if (innerException!=null) {
      throw innerException;
    }

    assertEquals(EXPECTED, testResult);
  }

  private abstract class TestBuilderCallback<C>
    implements StrongBuilder.Callback<C> {

    abstract protected void loadResult(C connection)
      throws Exception;

    @Override
    public void onConnected(C connection) {
      try {
        loadResult(connection);
        responseLatch.countDown();
      }
      catch (Exception e) {
        innerException=e;
        responseLatch.countDown();
      }
    }

    @Override
    public void onConnectionException(IOException e) {
      innerException=e;
      responseLatch.countDown();
    }

    @Override
    public void onTimeout() {
      responseLatch.countDown();
    }
  }
}
