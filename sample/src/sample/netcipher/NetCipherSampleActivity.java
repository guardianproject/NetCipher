
package sample.netcipher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import info.guardianproject.onionkit.trust.StrongHttpsClient;
import info.guardianproject.onionkit.ui.OrbotHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class NetCipherSampleActivity extends Activity {

    private final static String TAG = "OrlibSample";
    private TextView txtView = null;
    private EditText txtUrl = null;

    // test the local device proxy provided by Orbot/Tor
    private final static String PROXY_HOST = "127.0.0.1";
    private final static int PROXY_HTTP_PORT = 8118; // default for Orbot/Tor
    private final static int PROXY_SOCKS_PORT = 9050; // default for Orbot/Tor

    private Proxy.Type mProxyType = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        txtUrl = (EditText) findViewById(R.id.txtUrl);
        txtView = (TextView) findViewById(R.id.WizardTextBody);

        Button btn;

        btn = ((Button) findViewById(R.id.btnWizard1));

        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mProxyType = null;
                new Thread(runnableNet).start();
            }
        });

        btn = ((Button) findViewById(R.id.btnWizard2));

        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mProxyType = Proxy.Type.HTTP;
                new Thread(runnableNet).start();
            }
        });

        btn = ((Button) findViewById(R.id.btnWizard3));

        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                mProxyType = Proxy.Type.SOCKS;

                new Thread(runnableNet).start();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!OrbotHelper.isOrbotInstalled(this)) {
            promptToInstall();
        } else if (!OrbotHelper.isOrbotRunning(this)) {
            requestOrbotStart();
        }
    }

    public String checkHTTP(String url, Proxy.Type pType, String proxyHost, int proxyPort)
            throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, CertificateException, IOException {

        StrongHttpsClient httpclient = new StrongHttpsClient(getApplicationContext());

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
        httpclient.close();

        StringBuffer sb = new StringBuffer();
        sb.append(response.getStatusLine()).append("\n\n");

        InputStream is = response.getEntity().getContent();

        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line = null;

        while ((line = br.readLine()) != null)
            sb.append(line);

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
                        proxyPort = PROXY_HTTP_PORT;
                    else if (mProxyType == Proxy.Type.SOCKS)
                        proxyPort = PROXY_SOCKS_PORT;
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
                startActivityForResult(OrbotHelper.getOrbotStartIntent(), 1);
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
