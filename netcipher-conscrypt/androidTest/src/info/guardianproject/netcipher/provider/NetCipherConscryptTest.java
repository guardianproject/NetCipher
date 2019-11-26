/*
 * Copyright 2019 Michael Pöhn
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

package info.guardianproject.netcipher.provider;

import android.os.Build;
import android.support.test.runner.AndroidJUnit4;

import org.conscrypt.NetCipherDefaultSSLContextImpl;
import org.conscrypt.NetCipherOpenSSLContextImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.URL;
import java.security.Provider;
import java.security.Security;

import javax.net.ssl.HttpsURLConnection;

import info.guardianproject.netcipher.client.TlsOnlySocketFactory;
import okhttp3.OkHttpClient;

@RunWith(AndroidJUnit4.class)
public class NetCipherConscryptTest {

    @Before
    public void before() {
        Security.removeProvider("NetCipherConscrypt");
    }

    @After
    public void after() {
        Security.removeProvider("NetCipherConscrypt");
    }

    @Test
    public void testInsertNetCipherProvider(){
        Provider p = Security.getProvider("NetCipherConscrypt");
        Assert.assertNull(p);

        Security.insertProviderAt(NetCipherConscrypt.newProvider(), 1);
        p = Security.getProvider("NetCipherConscrypt");
        Assert.assertNotNull(p);

        Security.removeProvider("NetCipherConscrypt");
        p = Security.getProvider("NetCipherConscrypt");
        Assert.assertNull(p);
    }

    @Test
    public void testNetCipherProviderTlsImplPresent() {

        String ctxImplName = NetCipherOpenSSLContextImpl.class.getName();
        Assert.assertEquals("org.conscrypt.NetCipherOpenSSLContextImpl", ctxImplName);

        Provider p = NetCipherConscrypt.newProvider();
        Assert.assertEquals(ctxImplName + "$TLSv13", p.getProperty("SSLContext.SSL"));
        Assert.assertEquals(ctxImplName + "$TLSv13", p.getProperty("SSLContext.TLS"));
        Assert.assertEquals(ctxImplName + "$TLSv1", p.getProperty("SSLContext.TLSv1"));
        Assert.assertEquals(ctxImplName + "$TLSv11", p.getProperty("SSLContext.TLSv1.1"));
        Assert.assertEquals(ctxImplName + "$TLSv12", p.getProperty("SSLContext.TLSv1.2"));
        Assert.assertEquals(ctxImplName + "$TLSv13", p.getProperty("SSLContext.TLSv1.3"));
        Assert.assertEquals(NetCipherDefaultSSLContextImpl.class.getName(), p.getProperty("SSLContext.Default"));
    }

    @Test
    public void testHttpUrlConnectionSocketFactory() throws IOException {
        Assume.assumeTrue("Only works on Android 8.1 (sdk 27) or newer", Build.VERSION.SDK_INT >= 27);

        Provider p = Security.getProvider("NetCipherConscrypt");
        Assert.assertNull(p);

        Security.insertProviderAt(NetCipherConscrypt.newProvider(), 1);
        p = Security.getProvider("NetCipherConscrypt");
        Assert.assertNotNull(p);

        URL url = new URL("https://example.com");
        HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
        Assert.assertEquals(TlsOnlySocketFactory.class, c.getSSLSocketFactory().getClass());

        Security.removeProvider("NetCipherConscrypt");
        p = Security.getProvider("NetCipherConscrypt");
        Assert.assertNull(p);
    }

    @Test
    public void testOkHttp() throws IOException {
        Provider p = Security.getProvider("NetCipherConscrypt");
        Assert.assertNull(p);

        Security.insertProviderAt(NetCipherConscrypt.newProvider(), 1);
        p = Security.getProvider("NetCipherConscrypt");
        Assert.assertNotNull(p);

        OkHttpClient c = new OkHttpClient.Builder().build();
        Assert.assertEquals(TlsOnlySocketFactory.class, c.sslSocketFactory().getClass());

        Security.removeProvider("NetCipherConscrypt");
        p = Security.getProvider("NetCipherConscrypt");
        Assert.assertNull(p);
    }

}
