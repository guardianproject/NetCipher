/**
 * Shadow - Anonymous web browser for Android devices
 * Copyright (C) 2009 Connell Gauld
 * 
 * Thanks to University of Cambridge,
 * 		Alastair Beresford and Andrew Rice
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package info.guardianproject.onionkit.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import net.sourceforge.jsocks.socks.Socks5Proxy;
import net.sourceforge.jsocks.socks.SocksSocket;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpParams;


/**
 * Provides sockets for an HttpClient connection.
 * @author cmg47
 *
 */
public class SocksSocketFactory implements SocketFactory {

	SocksSocket server = null;
	private static Socks5Proxy sProxy = null;

	/**
	 * Construct a SocksSocketFactory that uses the provided SOCKS proxy.
	 * @param proxyaddress the IP address of the SOCKS proxy
	 * @param proxyport the port of the SOCKS proxy
	 */
	public SocksSocketFactory(String proxyaddress, int proxyport) throws  UnknownHostException {

		sProxy = new Socks5Proxy(proxyaddress, proxyport);
		
		//set this to false - we want the SOCKS proxy to handle DNS for us
		sProxy.resolveAddrLocally(false);
	
	}
	
	@Override
	public Socket connectSocket(Socket sock, String host, int port,
			InetAddress localAddress, int localPort, HttpParams params) throws IOException,
			UnknownHostException, ConnectTimeoutException {
		
		if (host == null) {
            throw new IllegalArgumentException("Target host may not be null.");
        }
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null.");
        }

        if (sock == null)
            sock = createSocket();

        if ((localAddress != null) || (localPort > 0)) {

            // we need to bind explicitly
            if (localPort < 0)
                localPort = 0; // indicates "any"

            InetSocketAddress isa =
                new InetSocketAddress(localAddress, localPort);
            sock.bind(isa);
        }

        return new SocksSocket(sProxy,host, port);
		
	}
	
	public Socket createSocket(Socket sock,
			InetAddress localAddress, int localPort, HttpParams params) throws IOException,
			UnknownHostException, ConnectTimeoutException {
		
		
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null.");
        }

        if (sock == null)
            sock = createSocket();

        if ((localAddress != null) || (localPort > 0)) {

            // we need to bind explicitly
            if (localPort < 0)
                localPort = 0; // indicates "any"

            InetSocketAddress isa =
                new InetSocketAddress(localAddress, localPort);
            sock.bind(isa);
        }

        return new SocksSocket(sProxy);
		
	}
	
    
    
	@Override
	public Socket createSocket() throws IOException {
		return new Socket();
	}

	@Override
	public boolean isSecure(Socket sock) throws IllegalArgumentException {
		return false;
	}
	
	private static SocksSocketFactory _instance;
	
	public static SocksSocketFactory getSocketFactory (String host, int port) throws UnknownHostException
	{
		if (_instance == null)
			_instance = new SocksSocketFactory (host, port);

		return _instance;
	}

}
