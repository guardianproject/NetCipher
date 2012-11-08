package sample.onionkit;

import info.guardianproject.onionkit.proxy.SocksHttpClient;
import info.guardianproject.onionkit.trust.StrongHttpsClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

import net.sourceforge.jsocks.socks.Socks5Proxy;
import net.sourceforge.jsocks.socks.SocksSocket;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.support.v4.app.NavUtils;

public class OnionKitSampleActivity extends Activity {

	private final static String TAG = "OrlibSample";
	private TextView txtView = null;
	private EditText txtUrl = null;
	
	private String proxyHost = "localhost"; //test the local device proxy provided by Orbot/Tor
	private int proxyHttpPort = 8118; //default for Orbot/Tor
	private int proxySocksPort = 9050; //default for Orbot/Tor
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        txtUrl = (EditText)findViewById(R.id.txtUrl);
        txtView = (TextView)findViewById(R.id.WizardTextBody);
        
        Button btn;
        
        btn = ((Button)findViewById(R.id.btnWizard2));
        
        btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
			 
				new Thread(runnableNetHttp).start();
			}
        });
        
        
        btn = ((Button)findViewById(R.id.btnWizard3));
        
        btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
			 

				new Thread(runnableNetSocks).start();
			}
        });
        
    }
    
    public String checkHTTP (String url, Proxy.Type proxyType, String host, int port) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException
    {

    		HttpClient httpclient = null;

			//get an HTTP client configured to work with local Tor SOCKS5 proxy
			httpclient = new StrongHttpsClient(this);
			
    		if (proxyType == Proxy.Type.SOCKS)
    		{
    			HttpHost proxy = new HttpHost(host, port);
        		httpclient.getParams().setParameter("SOCKS", proxy);
    			
    		}
    		else if (proxyType == Proxy.Type.HTTP)
    		{
        		HttpHost proxy = new HttpHost(host, port);
        		httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    		}
    		
        	HttpGet httpget = new HttpGet(url);
    		HttpResponse response = httpclient.execute(httpget);

    		StringBuffer sb = new StringBuffer();
    		sb.append(response.getStatusLine()).append("\n\n");
    		
    		InputStream is = response.getEntity().getContent();
    		List<String> lines = IOUtils.readLines(is);
    		sb.append(lines);
    		return sb.toString();
    	
    }
    
   
    
    Runnable runnableNetHttp = new Runnable ()
	{
		
		public void run ()
		{
			String url = txtUrl.getText().toString();
			
			try
			{
				Message msg = new Message();
				msg.getData().putString("status", "connecting through HTTP proxy to: " + url);
				handler.sendMessage(msg);
				String resp = checkHTTP(url, Proxy.Type.HTTP, proxyHost, proxyHttpPort);
				msg = new Message();
				msg.getData().putString("status", resp);
				handler.sendMessage(msg);

			}
			catch (Exception e)
			{
				String err = "error connecting to: " + url + "=" + e.toString();
				Log.e(TAG,err,e);
				Message msg = new Message();
				msg.getData().putString("status", err);
				handler.sendMessage(msg);
			}
		}
	};
	
	 Runnable runnableNetSocks = new Runnable ()
		{
			
			public void run ()
			{
				String url = txtUrl.getText().toString();
				
				try
				{
					Message msg = new Message();
					msg.getData().putString("status", "connecting through SOCKS proxy to: " + url);
					handler.sendMessage(msg);
					
					String resp = checkHTTP(url, Proxy.Type.SOCKS, proxyHost, proxySocksPort);
					msg = new Message();
					msg.getData().putString("status", resp);
					handler.sendMessage(msg);
				}
				catch (Exception e)
				{
					String err = "error connecting to: " + url + "=" + e.toString();
					Log.e(TAG,err,e);
					Message msg = new Message();
					msg.getData().putString("status", err);
					handler.sendMessage(msg);
				}
			}
		};
		
		Handler handler = new Handler ()
		{

			@Override
			public void handleMessage(Message msg) {
				
				String msgText = msg.getData().getString("status");
				
				txtView.setText(msgText);
			}
			
		};
}
