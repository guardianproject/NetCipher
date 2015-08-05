package info.guardianproject.netcipher.client;

import ch.boye.httpclientandroidlib.HttpException;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.HttpRequest;
import ch.boye.httpclientandroidlib.conn.SchemePortResolver;
import ch.boye.httpclientandroidlib.impl.conn.DefaultRoutePlanner;
import ch.boye.httpclientandroidlib.protocol.HttpContext;

public abstract class SocksAwareProxyRoutePlanner extends DefaultRoutePlanner {
    public SocksAwareProxyRoutePlanner(SchemePortResolver schemePortResolver) {
        super(schemePortResolver);
    }

    @Override
    protected HttpHost determineProxy(
            HttpHost target,
            HttpRequest request,
            HttpContext context) throws HttpException {
        HttpHost proxy = determineRequiredProxy(target, request, context);
        if (isSocksProxy(proxy))
            proxy = null;
        return proxy;
    }

    /**
     * Determine the proxy required for the provided target.
     *
     * @param target see {@link #determineProxy(HttpHost, HttpRequest, HttpContext) determineProxy()}
     * @param request see {@link #determineProxy(HttpHost, HttpRequest, HttpContext) determineProxy()}.
     *                Will be null when called from {@link SocksAwareClientConnOperator} to
     *                determine if target requires a SOCKS proxy, so don't rely on it in this case.
     * @param context see {@link #determineProxy(HttpHost, HttpRequest, HttpContext) determineProxy()}
     * @return the proxy required for this target, or null if should connect directly.
     */
    protected abstract HttpHost determineRequiredProxy(
            HttpHost target,
            HttpRequest request,
            HttpContext context);

    /**
     * Checks if the provided target is a proxy we define.
     *
     * @param target to check
     * @return true if this is a proxy, false otherwise
     */
    protected abstract boolean isProxy(HttpHost target);

    /**
     * Checks if the provided target is a SOCKS proxy we define.
     *
     * @param target to check
     * @return true if this target is a SOCKS proxy, false otherwise.
     */
    protected abstract boolean isSocksProxy(HttpHost target);
}
