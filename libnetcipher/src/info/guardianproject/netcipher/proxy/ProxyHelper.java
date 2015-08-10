package info.guardianproject.netcipher.proxy;

import android.content.Context;
import android.content.Intent;

public interface ProxyHelper {

	public boolean isInstalled (Context context);
	public void requestStatus (Context context);
	public boolean requestStart (Context context);
	public Intent getInstallIntent (Context context);
	public Intent getStartIntent (Context context);
	public String getName ();

    public final static String FDROID_PACKAGE_NAME = "org.fdroid.fdroid";
    public final static String PLAY_PACKAGE_NAME = "com.android.vending";
    
    /**
     * A request to Orbot to transparently start Tor services
     */
    public final static String ACTION_START = "android.intent.action.PROXY_START";
    /**
     * {@link Intent} send by Orbot with {@code ON/OFF/STARTING/STOPPING} status
     */
    public final static String ACTION_STATUS = "android.intent.action.PROXY_STATUS";
    /**
     * {@code String} that contains a status constant: {@link #STATUS_ON},
     * {@link #STATUS_OFF}, {@link #STATUS_STARTING}, or
     * {@link #STATUS_STOPPING}
     */
    public final static String EXTRA_STATUS = "android.intent.extra.PROXY_STATUS";
    
    public final static String EXTRA_PROXY_PORT_HTTP = "android.intent.extra.PROXY_PORT_HTTP";
    public final static String EXTRA_PROXY_PORT_SOCKS = "android.intent.extra.PROXY_PORT_SOCKS";
    
    /**
     * A {@link String} {@code packageName} for Orbot to direct its status reply
     * to, used in {@link #ACTION_START} {@link Intent}s sent to Orbot
     */
    public final static String EXTRA_PACKAGE_NAME = "android.intent.extra.PROXY_PACKAGE_NAME";

    /**
     * All tor-related services and daemons are stopped
     */
    public final static String STATUS_OFF = "OFF";
    /**
     * All tor-related services and daemons have completed starting
     */
    public final static String STATUS_ON = "ON";
    public final static String STATUS_STARTING = "STARTING";
    public final static String STATUS_STOPPING = "STOPPING";
    /**
     * The user has disabled the ability for background starts triggered by
     * apps. Fallback to the old Intent that brings up Orbot.
     */
    public final static String STATUS_STARTS_DISABLED = "STARTS_DISABLED";
}

