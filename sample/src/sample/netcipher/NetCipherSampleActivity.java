
package sample.netcipher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import info.guardianproject.netcipher.client.StrongHttpsClient;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.netcipher.proxy.ProxyHelper;
import info.guardianproject.netcipher.proxy.PsiphonHelper;

public class NetCipherSampleActivity extends Activity {

    private final static String TAG = "NetCipherSampleActivity";
    private TextView txtView;
    private EditText txtUrl;
    private Button httpProxyButton;
    private Button socksProxyButton;
    private TextView torStatusTextView;

    // test the local device proxy provided by Orbot/Tor
    private final static String PROXY_HOST = "127.0.0.1";
    private int mProxyHttp = 8118; // default for Orbot/Tor
    private int mProxySocks = 9050; // default for Orbot/Tor

    private final static String DEFAULT_ONION_URL = "http://3g2upl4pq6kufc4m.onion";
    
    private Proxy.Type mProxyType;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        txtUrl = (EditText) findViewById(R.id.txtUrl);
        txtView = (TextView) findViewById(R.id.WizardTextBody);
        torStatusTextView = (TextView) findViewById(R.id.torStatus);

        Button getStatusButton = (Button) findViewById(R.id.getStatus);
        getStatusButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i(TAG, "getStatusButton setOnClickListener onClick");
                OrbotHelper.requestStartTor(getBaseContext());
            }
        });

        Button btn = (Button) findViewById(R.id.btnWizard1);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mProxyType = null;
                new Thread(runnableNet).start();
            }
        });

        httpProxyButton = (Button) findViewById(R.id.btnWizard2);
        httpProxyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mProxyType = Proxy.Type.HTTP;
                new Thread(runnableNet).start();
            }
        });

        socksProxyButton = (Button) findViewById(R.id.btnWizard3);
        socksProxyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mProxyType = Proxy.Type.SOCKS;

                new Thread(runnableNet).start();
            }
        });
    }

    //Orbot specific receiver
    private BroadcastReceiver torStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), OrbotHelper.ACTION_STATUS)) {
                Log.i(TAG, getPackageName() + " received intent : " + intent.getAction() + " " + intent.getPackage());
                String status = intent.getStringExtra(OrbotHelper.EXTRA_STATUS) + " (" + intent.getStringExtra(OrbotHelper.EXTRA_PACKAGE_NAME) + ")";
                torStatusTextView.setText(status);

                boolean enabled = (intent.getStringExtra(OrbotHelper.EXTRA_STATUS).equals(OrbotHelper.STATUS_ON));
                httpProxyButton.setEnabled(enabled);
                socksProxyButton.setEnabled(enabled);
                
                if(enabled){
	                if (intent.hasExtra(OrbotHelper.EXTRA_PROXY_PORT_HTTP))
	                	mProxyHttp = intent.getIntExtra(OrbotHelper.EXTRA_PROXY_PORT_HTTP, -1);
	                
	                if (intent.hasExtra(OrbotHelper.EXTRA_PROXY_PORT_SOCKS))
	                	mProxySocks = intent.getIntExtra(OrbotHelper.EXTRA_PROXY_PORT_SOCKS, -1);
	          
                }
            }
        }
    };

    //Generic proxy receiver
    private BroadcastReceiver proxyStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), ProxyHelper.ACTION_STATUS)) {
            	
                Log.i(TAG, getPackageName() + " received intent : " + intent.getAction() + " " + intent.getPackage());
                String status = intent.getStringExtra(ProxyHelper.EXTRA_STATUS) + " (" + intent.getStringExtra(ProxyHelper.EXTRA_PACKAGE_NAME) + ")";
                torStatusTextView.setText(status);

                boolean enabled = (intent.getStringExtra(ProxyHelper.EXTRA_STATUS).equals(ProxyHelper.STATUS_ON));
               
                if(enabled){
	                if (intent.hasExtra(ProxyHelper.EXTRA_PROXY_PORT_HTTP))
	                	mProxyHttp = intent.getIntExtra(ProxyHelper.EXTRA_PROXY_PORT_HTTP, -1);
	                
	                if (intent.hasExtra(ProxyHelper.EXTRA_PROXY_PORT_SOCKS))
	                	mProxySocks = intent.getIntExtra(ProxyHelper.EXTRA_PROXY_PORT_SOCKS, -1);
	          
                }
                else if (intent.hasExtra(ProxyHelper.EXTRA_PACKAGE_NAME))
                {
                
                	if (intent.getStringExtra(ProxyHelper.EXTRA_PACKAGE_NAME).equals(PsiphonHelper.PACKAGE_NAME))
                	{
                		PsiphonHelper pHelper = new PsiphonHelper();
                		pHelper.requestStart(NetCipherSampleActivity.this);
                	}
                }

                httpProxyButton.setEnabled(enabled);
                socksProxyButton.setEnabled(enabled);
            }
        }
    };
    
    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(torStatusReceiver, new IntentFilter(OrbotHelper.ACTION_STATUS));
        registerReceiver(proxyStatusReceiver, new IntentFilter(ProxyHelper.ACTION_STATUS));
        
        //this will check if Orbot is installed, and if so, will auto-request it to start in the background
        if (!OrbotHelper.isOrbotInstalled(this)) {
            
        	//orbot is not installed... check for other proxies
        	boolean foundProxy = false;
        	
        	ProxyHelper[] proxyHelpers = {new PsiphonHelper()};
            
            for (ProxyHelper proxyHelper : proxyHelpers)
            {        
    	        if (!proxyHelper.isInstalled(this))
    	        {
    	        	Intent intent = proxyHelper.getInstallIntent(this);
    	        	startActivity(intent);
    	        }
    	        else
    	        {
    	        	proxyHelper.requestStatus(this);  
    	        	foundProxy = true;
    	        	break;
    	        }
            }
            
            if (!foundProxy)
            	promptToInstall(); //request Orbot install if no other proxy found
            
        } else {
        
        	//found Orbot, request to start
            OrbotHelper.requestStartTor(this);
        
            //set default url to an onion
            txtUrl.setText(DEFAULT_ONION_URL);
        	
        }
        
        
        
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(torStatusReceiver);
    }

    public String checkHTTP(String url, Proxy.Type pType, String proxyHost, int proxyPort)
            throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, CertificateException, IOException {

        //can set a specific list of allowed hostnames
        //String specialHostnames = ".cloudflare.com,.cloudflaressl.com,.s3-us-west-1.amazonaws.com";
        String specialHostnames = null;

        KeyStore keyStore = null; //can load your own keystore for CA certs, or just use the default

        StrongHttpsClient httpclient = new StrongHttpsClient(getApplicationContext(),keyStore,specialHostnames);
        httpclient.enableSSLCompatibilityMode();

        if (pType == null) {
            // do nothing
            httpclient.useProxy(false, null, null, -1);

        } else if (pType == Proxy.Type.SOCKS) {

            httpclient.useProxy(true, StrongHttpsClient.TYPE_SOCKS, proxyHost, proxyPort);

        } else if (pType == Proxy.Type.HTTP) {

            httpclient.useProxy(true, StrongHttpsClient.TYPE_HTTP, proxyHost, proxyPort);

        }

        HttpGet httpget = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpget);

        StringBuffer sb = new StringBuffer();
        sb.append(response.getStatusLine()).append("\n\n");

        InputStream is = response.getEntity().getContent();

        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line = null;

        while ((line = br.readLine()) != null)
            sb.append(line);

        httpclient.close();
        return sb.toString();
    }

    Runnable runnableNet = new Runnable()
    {

        @Override
        public void run()
        {
            String url = txtUrl.getText().toString();

            try
            {
                Message msg = new Message();
                msg.getData().putString("status", "connecting to: " + url);
                handler.sendMessage(msg);

                int proxyPort = -1;
                if (mProxyType != null)
                {
                    if (mProxyType == Proxy.Type.HTTP)
                        proxyPort = mProxyHttp;
                    else if (mProxyType == Proxy.Type.SOCKS)
                        proxyPort = mProxySocks;
                }
                String resp = checkHTTP(url, mProxyType, PROXY_HOST, proxyPort);
                msg = new Message();
                msg.getData().putString("status", resp);
                handler.sendMessage(msg);
            }
            catch (Exception e)
            {
                String err = "error connecting to: " + url + "=" + e.toString();
                Log.e(TAG, err, e);
                Message msg = new Message();
                msg.getData().putString("status", err);
                handler.sendMessage(msg);
            }
        }
    };

    Handler handler = new Handler()
    {

        @Override
        public void handleMessage(Message msg) {

            String msgText = msg.getData().getString("status");

            txtView.setText(msgText);
        }
    };

    /**
     * Ask the user whether to install Orbot or not. Check if installing from
     * F-Droid or Google Play, otherwise take the user to the Orbot download
     * page on f-droid.org.
     */
    void promptToInstall() {
        String message = getString(R.string.you_must_have_orbot) + "  ";

        final Intent intent = OrbotHelper.getOrbotInstallIntent(this);
        if (intent.getPackage() == null) {
            message += getString(R.string.download_orbot_from_fdroid);
        } else {
            message += getString(R.string.get_orbot_from_fdroid);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.install_orbot_);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(intent);
            }
        });
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // nothing to do
            }
        });
        builder.show();
    }

    void requestOrbotStart() {
        AlertDialog.Builder downloadDialog = new AlertDialog.Builder(this);
        downloadDialog.setTitle(R.string.start_orbot_);
        downloadDialog
                .setMessage(R.string.orbot_doesn_t_appear_to_be_running_would_you_like_to_start_it_up_and_connect_to_tor_);
        downloadDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivityForResult(OrbotHelper.getShowOrbotStartIntent(), 1);
            }
        });
        downloadDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        downloadDialog.show();
    }

}
