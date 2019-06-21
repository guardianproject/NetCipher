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
import android.support.test.runner.AndroidJUnit4;
import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongConnectionBuilder;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.proxy.StatusCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.support.test.InstrumentationRegistry.getContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class StrongConnectionBuilderTest {

    private static final String TEST_URL =
            "https://gitlab.com/guardianproject/NetCipher/raw/master/netciphertest/res/test.json";

    private static final String EXPECTED = "{\"Hello\": \"world\"}";
    private static AtomicBoolean initialized = new AtomicBoolean(false);
    private static AtomicBoolean isOrbotInstalled = null;
    private static CountDownLatch initLatch = new CountDownLatch(1);

    private CountDownLatch responseLatch;
    private Exception innerException = null;
    private String testResult = null;

    @Before
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
            assertTrue("setup timeout", initLatch.await(60, TimeUnit.SECONDS));
            initialized.set(true);
        }

        responseLatch = new CountDownLatch(1);
    }

    @Test
    public void testOrbotInstalled() {
        assertTrue("we were not initialized", initialized.get());
        assertNotNull("we did not get an Orbot status", isOrbotInstalled);

        try {
            getContext().getPackageManager().getApplicationInfo("org.torproject.android", 0);
            assertTrue("Orbot is installed, but NetCipher thinks it is not", isOrbotInstalled.get());
        } catch (PackageManager.NameNotFoundException e) {
            assertFalse("Orbot not installed, but NetCipher thinks it is", isOrbotInstalled.get());
        }
    }

    @Test
    public void testStrongConnectionBuilder() throws Exception {
        assertTrue("we were not initialized", initialized.get());
        assertNotNull("we did not get an Orbot status", isOrbotInstalled);

        if (isOrbotInstalled.get()) {
            StrongConnectionBuilder builder = StrongConnectionBuilder.forMaxSecurity(getContext());

            testStrongBuilder(builder.connectTo(TEST_URL), new TestBuilderCallback<HttpURLConnection>() {
                @Override
                protected void loadResult(HttpURLConnection c) throws Exception {
                    try {
                        testResult = StrongConnectionBuilder.slurp(c.getInputStream());
                    } finally {
                        c.disconnect();
                    }
                }
            });
        }
    }

    @Test
    public void testValidatedStrongConnectionBuilder() throws Exception {
        assertTrue("we were not initialized", initialized.get());
        assertNotNull("we did not get an Orbot status", isOrbotInstalled);

        if (isOrbotInstalled.get()) {
            StrongConnectionBuilder builder =
                    StrongConnectionBuilder.forMaxSecurity(getContext()).withTorValidation();

            testStrongBuilder(builder.connectTo(TEST_URL), new TestBuilderCallback<HttpURLConnection>() {
                @Override
                protected void loadResult(HttpURLConnection c)
                        throws Exception {
                    try {
                        testResult = StrongConnectionBuilder.slurp(c.getInputStream());
                    } finally {
                        c.disconnect();
                    }
                }
            });
        }
    }

    private void testStrongBuilder(StrongBuilder builder, TestBuilderCallback callback) throws Exception {
        testResult = null;
        builder.build(callback);

        assertTrue(responseLatch.await(120, TimeUnit.SECONDS));

        if (innerException != null) {
            throw innerException;
        }

        assertEquals(EXPECTED, testResult);
    }

    private abstract class TestBuilderCallback<C> implements StrongBuilder.Callback<C> {

        abstract protected void loadResult(C connection) throws Exception;

        @Override
        public void onConnected(C connection) {
            try {
                loadResult(connection);
                responseLatch.countDown();
            } catch (Exception e) {
                innerException = e;
                responseLatch.countDown();
            }
        }

        @Override
        public void onConnectionException(Exception e) {
            innerException = e;
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
    }
}
