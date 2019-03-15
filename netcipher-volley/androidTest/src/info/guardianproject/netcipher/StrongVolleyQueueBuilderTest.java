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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.test.AndroidTestCase;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongVolleyQueueBuilder;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.proxy.StatusCallback;

public class StrongVolleyQueueBuilderTest extends
        AndroidTestCase {
    private static final String TEST_URL =
        "https://gitlab.com/guardianproject/NetCipher/raw/master/netciphertest/res/test.json";
    private static final String EXPECTED = "{\"Hello\": \"world\"}";
    private static AtomicBoolean initialized = new AtomicBoolean(false);
    private static AtomicBoolean isOrbotInstalled = null;
    private CountDownLatch responseLatch;
    private Exception innerException = null;
    private String testResult = null;
    private static CountDownLatch initLatch = new CountDownLatch(1);

    public void setUp() throws InterruptedException {
        if (!initialized.get()) {
            OrbotHelper
                    .get(getContext())
                    .statusTimeout(60000)
                    .addStatusCallback(
                            new StatusCallback() {
                                @Override
                                public void onEnabled(Intent statusIntent) {
                                    isOrbotInstalled = new AtomicBoolean(true);
                                    initLatch.countDown();
                                }

                                @Override
                                public void onStarting() {

                                }

                                @Override
                                public void onStopping() {

                                }

                                @Override
                                public void onDisabled() {
                                    // we got a broadcast with a status of off, so keep waiting
                                }

                                @Override
                                public void onStatusTimeout() {
                                    initLatch.countDown();
                                    throw new RuntimeException("Orbot status request timed out");
                                }

                                @Override
                                public void onNotYetInstalled() {
                                    isOrbotInstalled = new AtomicBoolean(false);
                                    initLatch.countDown();
                                }
                            })
                    .init();
            assertTrue("setup timeout", initLatch.await(600, TimeUnit.SECONDS));
            initialized.set(true);
        }

        responseLatch = new CountDownLatch(1);
    }

    public void testOrbotInstalled() throws InterruptedException {
        assertTrue("we were not initialized", initialized.get());
        assertNotNull("we did not get an Orbot status", isOrbotInstalled);

        try {
            getContext()
                    .getPackageManager()
                    .getApplicationInfo("org.torproject.android", 0);
            assertTrue("Orbot is installed, but NetCipher thinks it is not",
                    isOrbotInstalled.get());
        } catch (PackageManager.NameNotFoundException e) {
            assertFalse("Orbot not installed, but NetCipher thinks it is",
                    isOrbotInstalled.get());
        }
    }

    public void testBuilder()
            throws Exception {
        assertTrue("we were not initialized", initialized.get());
        assertNotNull("we did not get an Orbot status", isOrbotInstalled);

        if (isOrbotInstalled.get()) {
            StrongVolleyQueueBuilder builder =
                    StrongVolleyQueueBuilder
                            .forMaxSecurity(getContext())
                            .withTorValidation();

            final StringRequest stringRequest =
                    new StringRequest(StringRequest.Method.GET, TEST_URL,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    testResult = response;
                                    responseLatch.countDown();
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    innerException = new RuntimeException("string request failed",
                                            error);
                                    responseLatch.countDown();
                                }
                            });

            builder.build(new StrongBuilder.Callback<RequestQueue>() {
                @Override
                public void onConnected(RequestQueue connection) {
                    connection.add(stringRequest);
                }

                @Override
                public void onConnectionException(Exception e) {
                    innerException = new RuntimeException("connection exception encountered", e);
                    responseLatch.countDown();
                }

                @Override
                public void onTimeout() {
                    responseLatch.countDown();
                }

                @Override
                public void onInvalid() {
                    responseLatch.countDown();
                }
            });

            assertTrue(responseLatch.await(600, TimeUnit.SECONDS));

            if (innerException != null) {
                throw innerException;
            }

            assertEquals(EXPECTED, testResult);
        }
    }
}
