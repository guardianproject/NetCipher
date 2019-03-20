/*
 * Copyright 2019 Hans-Christoph Steiner
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

package info.guardianproject.netcipher.proxy;

import info.guardianproject.netcipher.NetCipher;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class NetCipherURLStreamHandler extends URLStreamHandler {

    private URLStreamHandler defaultHandler;

    public NetCipherURLStreamHandler(URLStreamHandler defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        URL withHandler = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile(), defaultHandler);
        if (NetCipher.getProxy() == null) {
            return withHandler.openConnection();
        } else {
            return NetCipher.getHttpURLConnection(withHandler, false);
        }
    }
}
