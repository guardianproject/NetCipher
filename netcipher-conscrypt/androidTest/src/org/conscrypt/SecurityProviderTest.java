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

package org.conscrypt;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.Provider;
import java.security.Security;

import info.guardianproject.netcipher.provider.NetCipherConscrypt;

@RunWith(AndroidJUnit4.class)
public class SecurityProviderTest {

    @Test
    public void testInsertConscryptSecurityProvider(){
        Provider p = Security.getProvider("Conscrypt");
        Assert.assertNull(p);

        Security.insertProviderAt(Conscrypt.newProvider(), 1);
        p = Security.getProvider("Conscrypt");
        Assert.assertNotNull(p);

        Security.removeProvider("Conscrypt");
        p = Security.getProvider("Conscrypt");
        Assert.assertNull(p);
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
    public void testNetChiperProviderTlsImplPresent() {

        String ctxImplName = NetCipherOpenSSLContextImpl.class.getName();
        Assert.assertEquals("org.conscrypt.NetCipherOpenSSLContextImpl", ctxImplName);

        Provider p = NetCipherConscrypt.newProvider();
        Assert.assertEquals(ctxImplName + "$TLSv13", p.getProperty("SSLContext.SSL"));
        Assert.assertEquals(ctxImplName + "$TLSv13", p.getProperty("SSLContext.TLS"));
        Assert.assertEquals(ctxImplName + "$TLSv1", p.getProperty("SSLContext.TLSv1"));
        Assert.assertEquals(ctxImplName + "$TLSv11", p.getProperty("SSLContext.TLSv1.1"));
        Assert.assertEquals(ctxImplName + "$TLSv12", p.getProperty("SSLContext.TLSv1.2"));
        Assert.assertEquals(ctxImplName + "$TLSv13", p.getProperty("SSLContext.TLSv1.3"));
        Assert.assertEquals("org.conscrypt.DefaultSSLContextImpl", p.getProperty("SSLContext.Default"));
    }
}
