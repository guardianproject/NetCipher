package info.guardianproject.onionkit.trust;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.security.auth.x500.X500Principal;

import ch.boye.httpclientandroidlib.conn.ssl.X509HostnameVerifier;
import info.guardianproject.onionkit.R;

/**
 * Created by mnbogner on 3/23/15.
 */
public class SMVerifier implements X509HostnameVerifier
{

    private String[] hosts;

    public SMVerifier(Context context) {
        super();
        String hostsString = context.getString(R.string.sm_domains);
        if ((hostsString != null) && (hostsString.length() > 0)) {
            hosts = hostsString.split(",");
        } else {
            hosts = new String[0];
        }
    }

    @Override
    public boolean verify(String host, SSLSession session) {

        Log.d("VERIFIER", "METHOD verify(String host, SSLSession session) NOT IMPLEMENTED");
        return false;
    }

    @Override
    public void verify(String host, SSLSocket ssl) throws IOException {

        X500Principal peerPrincipal = (X500Principal) ssl.getSession().getPeerPrincipal();

        String dn = peerPrincipal.getName("CANONICAL");
        String cn = null;
        String[] dnParts = dn.split(",");

        for (String dnPart : dnParts) {
            if (dnPart.startsWith("cn=")) {
                cn = dnPart.substring(3);
            }
        }

        if (cn == null) {
            throw new IOException("COULD NOT EXTRACT INFORMATION FROM CERTIFICATE: " + dn);
        } else {
            for (String compareHost : hosts) {
                if (cn.equals(compareHost)) {
                    Log.d("VERIFIER", "FOUND A MATCH: " + cn + " = " + compareHost);
                    return;
                }
            }
        }

        throw new IOException("COULD NOT FIND A MATCH FOR " + Arrays.toString(hosts) + " IN CERTIFICATE: " + dn);
    }

    @Override
    public void verify(String host, X509Certificate cert) throws SSLException {

        Log.d("VERIFIER", "METHOD verify(String host, X509Certificate cert) NOT IMPLEMENTED");
        throw new SSLException("METHOD verify(String host, X509Certificate cert) NOT IMPLEMENTED");
    }

    @Override
    public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {

        Log.d("VERIFIER", "METHOD verify(String host, String[] cns, String[] subjectAlts) NOT IMPLEMENTED");
        throw new SSLException("METHOD verify(String host, String[] cns, String[] subjectAlts) NOT IMPLEMENTED");
    }
}