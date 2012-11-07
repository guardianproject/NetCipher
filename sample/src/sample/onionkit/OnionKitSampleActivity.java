package sample.onionkit;

import info.guardianproject.onionkit.proxy.SocksHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
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

import net.sourceforge.jsocks.socks.Socks5Proxy;
import net.sourceforge.jsocks.socks.SocksSocket;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Bundle;
import android.os.Handler;
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
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        txtUrl = (EditText)findViewById(R.id.txtUrl);
        txtView = (TextView)findViewById(R.id.WizardTextBody);
        Button btn = ((Button)findViewById(R.id.btnWizard1));
        
        btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
			 
				Runnable runnable = new Runnable ()
				{
					public void run()
					{
						//this opens a direct socks socket on port 80
						URL url;
						try {
							url = new URL(txtUrl.getText().toString());

							openSocksSocket(url.getHost(),url.getPort());
						} catch (MalformedURLException e) {
							Log.e(TAG,"bad url",e);
        					txtView.setText("bad urL: " + e.getLocalizedMessage());
						}
						
					}
				};
				
				Handler handle = new Handler();
				handle.post(runnable);
			}
        });

        btn = ((Button)findViewById(R.id.btnWizard2));
        
        btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
			 
				String url = txtUrl.getText().toString();
				
				try
				{
					String resp = checkHTTP(url, Proxy.Type.HTTP, "localhost", 8118);
					txtView.setText("connection response: " + resp);
				}
				catch (Exception e)
				{
					Log.e(TAG,"error connecting to: " + url,e);
					txtView.setText(e.getLocalizedMessage());
				}

			}
        });
        
        
        btn = ((Button)findViewById(R.id.btnWizard3));
        
        btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
			 
				//use the direct SOCKS5 proxy built into Tor itself (more secure)
				
				//SOCKS with HTTP/S not quite working yet (DNS leaks)
				//checkHTTP("https://check.torproject.org:443/", Proxy.Type.SOCKS, "localhost", 9050);
				
				/// so use HTTP proxy for now, it will work just fine!
				String url = txtUrl.getText().toString();
				
				try
				{
					String resp = checkHTTP(url, Proxy.Type.SOCKS, "localhost", 9050);
					txtView.setText("connection response: " + resp);
				}
				catch (Exception e)
				{
					Log.e(TAG,"error connecting to: " + url,e);
					txtView.setText(e.getLocalizedMessage());
				}
			}
        });
        
    }
    
    
    public void openSocksSocket (String checkHost, int checkPort)
    {
    	txtView.setText("Opening socket to " + checkHost + " on port " + checkPort + "\n");
    	
    	try
    	{
    		
    		Socks5Proxy proxy = new Socks5Proxy("localhost",9050);
    		proxy.resolveAddrLocally(false);
   
    		SocksSocket ss = null;
    		
    		PrintWriter out = null;
    		BufferedReader in = null;
    		
    		 try {
    	       
    			 ss = new SocksSocket(proxy,checkHost, checkPort);
    			 
    	            out = new PrintWriter(ss.getOutputStream(), true);
    	            in = new BufferedReader(new InputStreamReader(
    	                                        ss.getInputStream()));
    	           
    	    		
    	            out.println("GET / HTTP/1.0");
    	            out.println("Host: check.torproject.org");
    	            out.println();
    	            
    	            String line = null;
    	            
    	            while ((line = in.readLine())!=null)
    	            {
    	            	txtView.append(line);
    	            	txtView.append("\n");
    	            }
    	            
    	            
    	        } catch (UnknownHostException e) {
    	            Log.i(TAG,"Could not find host",e);
    	        } catch (IOException e) {
    	            Log.i(TAG,"Error reading and writing",e);
    	           
    	        }

    		

    		out.close();
    		in.close();
    		
    	}
    	catch (Exception e)
    	{
    		txtView.append(e.getMessage());
    		
    		Log.e(TAG, "Unable to connect to torproject",e);
    	}
    }
    
    
    public String checkHTTP (String url, Proxy.Type proxyType, String host, int port) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException
    {

    		HttpClient httpclient = null;
    		
    		if (proxyType == Proxy.Type.SOCKS)
    		{
    			//get an HTTP client configured to work with local Tor SOCKS5 proxy
    			httpclient = new SocksHttpClient(this, host, port);
    		}
    		else if (proxyType == Proxy.Type.HTTP)
    		{
    		
    			httpclient = new DefaultHttpClient();
        		HttpHost proxy = new HttpHost(host, port);
        		httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
    		}
    		
        	HttpGet httpget = new HttpGet(url);
    		HttpResponse response = httpclient.execute(httpget);

    		return response.getStatusLine().getStatusCode() + "";
    	
    }
}
