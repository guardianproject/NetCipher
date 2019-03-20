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

import android.annotation.SuppressLint;
import android.support.annotation.Nullable;
import android.util.Log;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class NetCipherURLStreamHandlerFactory implements URLStreamHandlerFactory {

    public static final String TAG = "NetCipherURL...rFactory";

    /**
     * This does not support {@code jar:}, {@code ftp:}, and anything else
     * that system's factory might handle.  This returns null in those cases
     * so that this fails closed, instead of returning an unproxied thing.
     *
     * @param protocol the name of the protocol aka URL Scheme
     * @return a {@link URLStreamHandler} or null if protocol not supported
     * @see java.net.URL#getURLStreamHandler(String protocol)
     */
    @SuppressLint("PrivateApi")
    @Nullable
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        try {
            switch (protocol) {
                case "http":
                    return new NetCipherURLStreamHandler(
                            (URLStreamHandler) Class.forName("com.android.okhttp.HttpHandler").newInstance());
                case "https":
                    return new NetCipherURLStreamHandler(
                            (URLStreamHandler) Class.forName("com.android.okhttp.HttpsHandler").newInstance());
                case "file":
                    return (URLStreamHandler) Class.forName("sun.net.www.protocol.file.Handler").newInstance();
                default:
                    Log.e(TAG, "Unsupported protocol: " + protocol);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
