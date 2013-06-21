
package info.guardianproject.onionkit.web;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.http.HttpHost;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

public class WebkitProxy {

    private final static String DEFAULT_HOST = "127.0.0.1";
    private final static int DEFAULT_PORT = 8118;
    private final static int DEFAULT_SOCKET_PORT = 9050;

    private final static int REQUEST_CODE = 0;

    private final static String TAG = "OrbotHelpher";

    public static void setProxy(Context ctx) throws Exception
    {
        setProxy(ctx, DEFAULT_HOST, DEFAULT_PORT);
    }

    public static boolean setProxy(Context ctx, String host, int port) throws Exception
    {
        setSystemProperties(host, port);

        boolean worked = false;

        if (Build.VERSION.SDK_INT < 14)
        {
            worked = setWebkitProxyGingerbread(ctx, host, port);
        }
        else
        {
            worked = setWebkitProxyICS(ctx, host, port);
        }

        return worked;
    }

    private static void setSystemProperties(String host, int port)
    {

        System.setProperty("http.proxyHost", host);
        System.setProperty("http.proxyPort", port + "");

        /*
         * System.setProperty("https.proxyHost", host);
         * System.setProperty("https.proxyPort", port + "");
         * System.setProperty("socks.proxyHost", host);
         * System.setProperty("socks.proxyPort", port + "");
         */

        System.getProperty("networkaddress.cache.ttl", "-1");

    }

    /**
     * Override WebKit Proxy settings
     * 
     * @param ctx Android ApplicationContext
     * @param host
     * @param port
     * @return true if Proxy was successfully set
     */
    private static boolean setWebkitProxyGingerbread(Context ctx, String host, int port)
            throws Exception
    {
        boolean ret = false;

        Object requestQueueObject = getRequestQueue(ctx);
        if (requestQueueObject != null) {
            // Create Proxy config object and set it into request Q
            HttpHost httpHost = new HttpHost(host, port, "http");
            setDeclaredField(requestQueueObject, "mProxyHost", httpHost);
            return true;
        }
        return false;

    }

    private static boolean setWebkitProxyICS(Context ctx, String host, int port) throws Exception
    {

        // PSIPHON: added support for Android 4.x WebView proxy
        try
        {
            Class webViewCoreClass = Class.forName("android.webkit.WebViewCore");

            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
            if (webViewCoreClass != null && proxyPropertiesClass != null)
            {
                Method m = webViewCoreClass.getDeclaredMethod("sendStaticMessage", Integer.TYPE,
                        Object.class);
                Constructor c = proxyPropertiesClass.getConstructor(String.class, Integer.TYPE,
                        String.class);

                if (m != null && c != null)
                {
                    m.setAccessible(true);
                    c.setAccessible(true);
                    Object properties = c.newInstance(host, port, null);

                    // android.webkit.WebViewCore.EventHub.PROXY_CHANGED = 193;
                    m.invoke(null, 193, properties);
                    return true;
                }
                else
                    return false;
            }
        } catch (Exception e)
        {
            Log.e("ProxySettings",
                    "Exception setting WebKit proxy through android.net.ProxyProperties: "
                            + e.toString());
        } catch (Error e)
        {
            Log.e("ProxySettings",
                    "Exception setting WebKit proxy through android.webkit.Network: "
                            + e.toString());
        }

        return false;

    }

    public static void resetProxy(Context ctx) throws Exception {
         if (Build.VERSION.SDK_INT < 14)
        {
            resetProxyForGingerBread(ctx);
        }
        else
        {
            resetProxyForICS();
        }
    }

    private static void resetProxyForICS() throws Exception{
        try
        {
            Class webViewCoreClass = Class.forName("android.webkit.WebViewCore");
            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
            if (webViewCoreClass != null && proxyPropertiesClass != null)
            {
                Method m = webViewCoreClass.getDeclaredMethod("sendStaticMessage", Integer.TYPE,
                        Object.class);

                if (m != null)
                {
                    m.setAccessible(true);

                    // android.webkit.WebViewCore.EventHub.PROXY_CHANGED = 193;
                    m.invoke(null, 193, null);
                }
            }
        } catch (Exception e)
        {
            Log.e("ProxySettings",
                    "Exception setting WebKit proxy through android.net.ProxyProperties: "
                            + e.toString());
            throw e;
        } catch (Error e)
        {
            Log.e("ProxySettings",
                    "Exception setting WebKit proxy through android.webkit.Network: "
                            + e.toString());
            throw e;
        }
    }

    private static void resetProxyForGingerBread(Context ctx) throws Exception {
        Object requestQueueObject = getRequestQueue(ctx);
        if (requestQueueObject != null) {
            setDeclaredField(requestQueueObject, "mProxyHost", null);
        }
    }

    public static Object getRequestQueue(Context ctx) throws Exception {
        Object ret = null;
        Class networkClass = Class.forName("android.webkit.Network");
        if (networkClass != null) {
            Object networkObj = invokeMethod(networkClass, "getInstance", new Object[] {
                ctx
            }, Context.class);
            if (networkObj != null) {
                ret = getDeclaredField(networkObj, "mRequestQueue");
            }
        }
        return ret;
    }

    private static Object getDeclaredField(Object obj, String name)
            throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        Object out = f.get(obj);
        // System.out.println(obj.getClass().getName() + "." + name + " = "+
        // out);
        return out;
    }

    private static void setDeclaredField(Object obj, String name, Object value)
            throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static Object invokeMethod(Object object, String methodName, Object[] params,
            Class... types) throws Exception {
        Object out = null;
        Class c = object instanceof Class ? (Class) object : object.getClass();
        if (types != null) {
            Method method = c.getMethod(methodName, types);
            out = method.invoke(object, params);
        } else {
            Method method = c.getMethod(methodName);
            out = method.invoke(object);
        }
        // System.out.println(object.getClass().getName() + "." + methodName +
        // "() = "+ out);
        return out;
    }

    public static Socket getSocket(Context context, String proxyHost, int proxyPort)
            throws IOException
    {
        Socket sock = new Socket();

        sock.connect(new InetSocketAddress(proxyHost, proxyPort), 10000);

        return sock;
    }

    public static Socket getSocket(Context context) throws IOException
    {
        return getSocket(context, DEFAULT_HOST, DEFAULT_SOCKET_PORT);

    }

    public static AlertDialog initOrbot(Activity activity,
            CharSequence stringTitle,
            CharSequence stringMessage,
            CharSequence stringButtonYes,
            CharSequence stringButtonNo,
            CharSequence stringDesiredBarcodeFormats) {
        Intent intentScan = new Intent("org.torproject.android.START_TOR");
        intentScan.addCategory(Intent.CATEGORY_DEFAULT);

        try {
            activity.startActivityForResult(intentScan, REQUEST_CODE);
            return null;
        } catch (ActivityNotFoundException e) {
            return showDownloadDialog(activity, stringTitle, stringMessage, stringButtonYes,
                    stringButtonNo);
        }
    }

    private static AlertDialog showDownloadDialog(final Activity activity,
            CharSequence stringTitle,
            CharSequence stringMessage,
            CharSequence stringButtonYes,
            CharSequence stringButtonNo) {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(activity);
        downloadDialog.setTitle(stringTitle);
        downloadDialog.setMessage(stringMessage);
        downloadDialog.setPositiveButton(stringButtonYes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Uri uri = Uri.parse("market://search?q=pname:org.torproject.android");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                activity.startActivity(intent);
            }
        });
        downloadDialog.setNegativeButton(stringButtonNo, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return downloadDialog.show();
    }
}
