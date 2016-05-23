/**
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

/**
 * BroadcastReceiver that listens for Orbot status intents
 * @author vanitas
 */
class OrbotStatusReceiver extends BroadcastReceiver {
    private static final String TAG = "OrbotStatReceiver";

    //Last information we received about Orbot's state
    private boolean orbotWasRunning = false;
    private int lastProxyPortHttp;
    private int lastProxyPortSocks;
    private String lastProxyHost;

    private ArrayList<OrbotStatusListener> orbotStatusListeners;

    public OrbotStatusReceiver() {
        orbotStatusListeners = new ArrayList();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (OrbotHelper.ACTION_STATUS.equals(intent.getAction())) {
            Log.d(TAG, context.getPackageName() + " received intent : " + intent.getAction() + " " + intent.getPackage());
            String status = intent.getStringExtra(OrbotHelper.EXTRA_STATUS) + " (" + intent.getStringExtra(OrbotHelper.EXTRA_PACKAGE_NAME) + ")";
            Log.d(TAG, "Orbot status: " + status);

            //STATUS_ON
            if(intent.getStringExtra(OrbotHelper.EXTRA_STATUS).equals(OrbotHelper.STATUS_ON)) {
                Bundle extras = intent.getExtras();
                String newHost = "127.0.0.1"; //TODO: Get actual proxy host (or is this unnecessary?)
                int newProxyPortHttp = extras.getInt(OrbotHelper.EXTRA_PROXY_PORT_HTTP, -1);
                int newProxyPortSocks = extras.getInt(OrbotHelper.EXTRA_PROXY_PORT_SOCKS, -1);

                if (!orbotWasRunning) {
                    notifyOrbotStarted();
                }

                if (newHost != null && !newHost.equals(lastProxyHost)) {
                    lastProxyHost = newHost;
                }

                if (newProxyPortHttp != -1 && newProxyPortHttp != lastProxyPortHttp) {
                    lastProxyPortHttp = newProxyPortHttp;
                    notifyHttpProxyChanged(lastProxyHost, lastProxyPortHttp);
                }

                if (newProxyPortSocks != -1 && newProxyPortSocks != lastProxyPortSocks) {
                    lastProxyPortSocks = newProxyPortSocks;
                    notifySocksProxyChanged(lastProxyHost, lastProxyPortSocks);
                }
                orbotWasRunning = true;
                return;
            }

            //STATUS_OFF
            else if(intent.getStringExtra(OrbotHelper.EXTRA_STATUS).equals(OrbotHelper.STATUS_OFF)) {
                orbotWasRunning = false;
                if(orbotWasRunning) notifyOrbotStopped();
                return;
            }

            //STATUS_STARTING
            else if(intent.getStringExtra(OrbotHelper.EXTRA_STATUS).equals(OrbotHelper.STATUS_STARTING)) {
                notifyOrbotIsStarting();
                return;
            }

            //STATUS_STOPPING
            else if(intent.getStringExtra(OrbotHelper.EXTRA_STATUS).equals(OrbotHelper.STATUS_STOPPING)) {
                notifyOrbotIsStopping();
                return;
            }

            //STATUS_STARTS_DISABLED
            else if(intent.getStringExtra(OrbotHelper.EXTRA_STATUS).equals(OrbotHelper.STATUS_STARTS_DISABLED)) {
                notifyOrbotStartsDisabled();
                return;

            }
        }
    }

    public void notifyOrbotStarted() {
        Log.d(TAG, "Notify "+orbotStatusListeners.size()+" OrbotStatusListeners that Orbot has started.");
        for(OrbotStatusListener l : orbotStatusListeners) {
            l.onOrbotStarted();
        }
    }

    public void notifyOrbotStopped() {
        Log.d(TAG, "Notify "+orbotStatusListeners.size()+" OrbotStatusListeners that Orbot has stopped.");
        for(OrbotStatusListener l : orbotStatusListeners) {
            l.onOrbotStopped();
        }
    }

    public void notifyHttpProxyChanged(String host, int port) {
        Log.d(TAG, "Notify "+orbotStatusListeners.size()+" OrbotStatusListeners that Orbot's Http proxy changed to "+host+":"+port+".");
        for(OrbotStatusListener l : orbotStatusListeners) {
            l.onOrbotHttpProxyChanged(host, port);
        }
    }

    public void notifySocksProxyChanged(String host, int port) {
        Log.d(TAG, "Notify "+orbotStatusListeners.size()+" OrbotStatusListeners that Orbot's Socks proxy changed to "+host+":"+port+".");
        for(OrbotStatusListener l : orbotStatusListeners) {
            l.onOrbotSocksProxyChanged(host, port);
        }
    }

    public void notifyOrbotIsStarting() {
        Log.d(TAG, "Notify "+orbotStatusListeners.size()+" OrbotStatusListeners that Orbot is starting.");
        for(OrbotStatusListener l : orbotStatusListeners) {
            l.onOrbotIsStarting();
        }
    }

    public void notifyOrbotIsStopping() {
        Log.d(TAG, "Notify "+orbotStatusListeners.size()+" OrbotStatusListeners that Orbot is stopping.");
        for(OrbotStatusListener l : orbotStatusListeners) {
            l.onOrbotIsStopping();
        }
    }

    public void notifyOrbotStartsDisabled() {
        Log.d(TAG, "Notify "+orbotStatusListeners.size()+" OrbotStatusListeners that Orbot background starts are disabled.");
        for(OrbotStatusListener l : orbotStatusListeners) {
            l.onOrbotBackgroundStartsDisabled();
        }
    }

    void addListener(OrbotStatusListener listener) {
        Log.d(TAG, "Added listener "+listener.getClass());
        if(!orbotStatusListeners.contains(listener)) orbotStatusListeners.add(listener);
    }

    void removeListener(OrbotStatusListener listener) {
        if(orbotStatusListeners.contains(listener)) orbotStatusListeners.remove(listener);
    }

    boolean getOrbotIsRunning() {
        return orbotWasRunning;
    }

    int getOrbotProxyPortHttp() {
        return lastProxyPortHttp;
    }

    int getOrbotProxyPortSocks() {
        return lastProxyPortSocks;
    }

    String getOrbotProxyHost() {
        return lastProxyHost;
    }
}
