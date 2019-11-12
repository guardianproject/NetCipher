/*
 * Copyright (c) 2019 Michael Pöhn
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

package sample.netcipher.conscrypt;

import android.app.Application;
import android.util.Log;

import org.conscrypt.Conscrypt;

import java.security.Provider;
import java.security.Security;
import java.util.Map;

import info.guardianproject.netcipher.provider.NetCipherConscrypt;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Security.insertProviderAt(NetCipherConscrypt.newProvider(), 1);

        // make sure the default system ssl implementation does not get used
        Security.removeProvider("AndroidOpenSSL");

        // for (Provider p : Security.getProviders()) {
        //     Log.d("###", "active provider: " + p.getName());
        //}
        //
        // if(Security.getProvider("NetCipherConscrypt") != null) {
        //     for (Map.Entry<Object, Object> e : Security.getProvider("NetCipherConscrypt").entrySet()) {
        //         Log.d("###", "NetCipherConscrypt Provider item: " + e.getKey() + " -> " + e.getValue());
        //     }
        // }

    }

}
