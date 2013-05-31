
package info.guardianproject.onionkit.test;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.conn.params.ConnRoutePNames;
import info.guardianproject.onionkit.trust.StrongHttpsClient;

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

public class TLSPretenseClientActivity extends Activity {

    private final static String TAG = "OrlibSample";
    private TextView txtView = null;
    private EditText txtUrl = null;
    private EditText numTestsView = null;
    private ScrollView consoleScroll = null;
    private TestQueue testQueue = new TestQueue();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        txtUrl = (EditText) findViewById(R.id.txtUrl);
        txtView = (TextView) findViewById(R.id.WizardTextBody);
        numTestsView = (EditText) findViewById(R.id.numTestsEdit);
        consoleScroll = (ScrollView) findViewById(R.id.consoleScrollView);


        Button btn;

        btn = ((Button) findViewById(R.id.btnStartTest));

        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                txtView.setText("");
                int numTests = Integer.parseInt(numTestsView.getText().toString());
                String url = txtUrl.getText().toString();
                testQueue.start(numTests, url);

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         *  Not using orbot in this test suite for now
         * OrbotHelper oc = new OrbotHelper(this); if (!oc.isOrbotInstalled()) {
         * oc.promptToInstall(this); } else if (!oc.isOrbotRunning()) {
         * oc.requestOrbotStart(this); }
         */

    }

    public String checkHTTP(String url, Proxy.Type pType, String proxyHost, int proxyPort)
            throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, CertificateException, IOException
    {
        KeyStore trustStore = KeyStore.getInstance("BKS");
        // load our bundled cacerts from raw assets
        InputStream in = this.getResources().openRawResource(R.raw.gp_tlspretense_ca);
        trustStore.load(in, "changeit".toCharArray());

        StrongHttpsClient httpclient = new StrongHttpsClient(getApplicationContext(), trustStore);
        httpclient.getStrongTrustManager().setNotifyVerificationFail(true);
        httpclient.getStrongTrustManager().setNotifyVerificationSuccess(true);

        if (pType == null)
        {
            // do nothing
            httpclient.useProxy(false, null, null, -1);

        }
        else if (pType == Proxy.Type.SOCKS)
        {

            httpclient.useProxy(true, "SOCKS", proxyHost, proxyPort);

        }
        else if (pType == Proxy.Type.HTTP)
        {
            httpclient.useProxy(true, ConnRoutePNames.DEFAULT_PROXY, proxyHost, proxyPort);

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

        return sb.toString();

    }

    public void appendMsg(String msg) {
        String text = txtView.getText().toString();
        text += msg;
        txtView.setText(text);
        consoleScroll.scrollTo(0, txtView.getHeight());

    }

    public class TestQueue {

        int numTests = 0;
        int currentTest = 0;
        String url = "";
        boolean paused = false;

        public TestQueue() { }

        public void start(int numTests, String url) {
            reset();
            this.numTests = numTests;
            this.url = url;
            start();
        }

        public void reset() {
            currentTest = 0;
            paused = false;
        }

        public void start() {
            if( paused ) return;
            currentTest++;
            if( currentTest <= numTests ) {
                appendMsg("Starting test #" + currentTest);
                TlsConnectTask task = new TlsConnectTask(TLSPretenseClientActivity.this, this);
                task.execute(url);
            }
        }

        public void pause() {
            paused = true;
            appendMsg("paused\n");
        }

        public void stop() {
            reset();
        }

        public void testFinished(String result) {
            appendMsg(" ... "+result+"\n");
            start();
        }
    }

    public class TlsConnectTask extends AsyncTask <String,Void,String> {

        Context context;
        TestQueue callbackObj;
        public TlsConnectTask(Context c, TestQueue o) {
            context = c;
            callbackObj = o;
        }
        @Override
        protected String doInBackground(String... url) {
            Proxy.Type ptype = null;
            try {
                String resp = TLSPretenseClientActivity.this.checkHTTP(url[0], ptype, "", -1);
            } catch (Exception e ) {
                return e.toString();
            }
            return "complete";
        }
        @Override
        protected void onPostExecute(String result) {
            callbackObj.testFinished(result);
        }


    }


}
