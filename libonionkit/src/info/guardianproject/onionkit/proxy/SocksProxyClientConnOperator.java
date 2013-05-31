
package info.guardianproject.onionkit.proxy;

import info.guardianproject.onionkit.trust.StrongSSLSocketFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.conn.ConnectTimeoutException;
import ch.boye.httpclientandroidlib.conn.HttpHostConnectException;
import ch.boye.httpclientandroidlib.conn.OperatedClientConnection;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.conn.scheme.SocketFactory;
import ch.boye.httpclientandroidlib.impl.conn.DefaultClientConnectionOperator;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.protocol.HttpContext;

public class SocksProxyClientConnOperator extends DefaultClientConnectionOperator {

    private String mProxyHost;
    private int mProxyPort;

    public SocksProxyClientConnOperator(final SchemeRegistry schemes, String proxyHost,
            int proxyPort) {
        super(schemes);

        mProxyHost = proxyHost;
        mProxyPort = proxyPort;
    }

    @Override
    public void openConnection(final OperatedClientConnection conn, final HttpHost target,
            final InetAddress local, final HttpContext context, final HttpParams params)
            throws IOException {
        if (conn == null) {
            throw new IllegalArgumentException("Connection may not be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("Target host may not be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        if (conn.isOpen()) {
            throw new IllegalStateException("Connection must not be open");
        }

        Scheme schm = schemeRegistry.getScheme(target.getSchemeName());
        SocketFactory sf = schm.getSocketFactory();

        InetAddress[] addresses = InetAddress.getAllByName(target.getHostName());

        int port = schm.resolvePort(target.getPort());

        InetSocketAddress socksAddr = new InetSocketAddress(mProxyHost, mProxyPort);
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksAddr);
        Socket sock = new Socket(proxy);

        if (sf instanceof StrongSSLSocketFactory)
        {
            sock.connect(new InetSocketAddress(target.getHostName(), port));
            sock = ((StrongSSLSocketFactory) sf).createSocket(sock, target.getHostName(), port,
                    true);
        }

        conn.opening(sock, target);

        for (int i = 0; i < addresses.length; i++) {
            InetAddress address = addresses[i];
            boolean last = i == addresses.length - 1;

            try {

                if (!sock.isConnected())
                {
                    Socket connsock = sf.connectSocket(sock, address.getHostAddress(), port, local,
                            0, params);

                    if (sock != connsock) {
                        sock = connsock;
                        conn.opening(sock, target);
                    }
                }

                prepareSocket(sock, context, params);
                conn.openCompleted(sf.isSecure(sock), params);
                break;
            } catch (ConnectException ex) {
                if (last) {
                    throw new HttpHostConnectException(target, ex);
                }
            } catch (ConnectTimeoutException ex) {
                if (last) {
                    throw ex;
                }
            }
        }
    }
}
