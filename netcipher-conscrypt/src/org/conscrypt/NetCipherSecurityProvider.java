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

package org.conscrypt;

import java.security.Provider;

public class NetCipherSecurityProvider extends Provider {

    public static final String NAME = "NetCipherConscrypt";
    public static final int VERSION = 1;
    public static final String INFO = "paranoid NetChipher Security Provider Based on Conscrypt";

    /**
     * Constructs a provider with the specified name, version number,
     * and information.
     */

    public NetCipherSecurityProvider() {
        super("NetCipherConscrypt", 1, INFO);

        Provider conscrypt = Conscrypt.newProvider();

        for(Entry<Object, Object> e : conscrypt.entrySet()) {
            put(e.getKey(), mapToNetCipher(e.getKey(), e.getValue()));
        }
    }

    private Object mapToNetCipher(Object key, Object val) {

        if (val instanceof String && key instanceof String) {

            String valStr = (String) val;
            String keyStr = (String) key;

            switch (keyStr) {
                case "SSLContext.Default":
                    valStr = NetCipherDefaultSSLContextImpl.class.getName();
                    break;
                case "SSLContext.SSL":
                case "SSLContext.TLS":
                case "SSLContext.TLSv1.3":
                    valStr = NetCipherOpenSSLContextImpl.TLSv13.class.getName();
                    break;
                case "SSLContext.TLSv1":
                    valStr = NetCipherOpenSSLContextImpl.TLSv1.class.getName();
                    break;
                case "SSLContext.TLSv1.1":
                    valStr = NetCipherOpenSSLContextImpl.TLSv11.class.getName();
                    break;
                case "SSLContext.TLSv1.2":
                    valStr = NetCipherOpenSSLContextImpl.TLSv12.class.getName();
                    break;
            }

            // if (!valStr.equals(val)) {
            //     Log.d("###", "provider override '" + key + "': " + val + " -> " + valStr);
            // }

            return valStr;

        } else {
            return val;
        }
    }
}
