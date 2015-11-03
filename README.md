NetCipher: Secured Networking for Android
========

*Better TLS and Tor App Integration*

NetCipher is a library for Android that provides multiple means to improve
network security in mobile applications.  It provides best practices TLS
settings using the standard Android HTTP methods, [HttpURLConnection] and
[Apache HTTP Client], provides simple Tor integration, makes it easy to
configure proxies for HTTP connections and `WebView` instances.

READ THIS! NEW REQUIREMENTS:
To resolve an issue with SSL certificates on hosting sites such as cloudflare (javax.net.ssl.SSLException: hostname in certificate didn't match), the StrongHttpsClient class now uses a custom verifier (SMVerifier) which matches the domain in the certificate against the domain specified in the new file libnetcipher/res/values/domain.xml

This is an Android Library Project that provides multiple means to improve
network security in mobile applications.

More specifically this library provides:

- Hardening of TLS protocol support and cipher suites, especially on older
  versions of Android (e.g. 4.4 and older)
- Proxied Connection Support: HTTP and SOCKS proxy connection support for HTTP
  and HTTPS traffic through specific configuration
- OrbotHelper: a utility class to support application integration with Orbot
  (Tor for Android). Check if its installed, automatically start it, etc.
- Optional, custom certificate store based on the open Debian root CA trust
  store, which is built with Mozilla's CA collection.

IT MUST BE NOTED, that you can use this library without using Orbot/Tor, but
obviously we think using strong TLS/SSL connections over Tor is just about the
best thing in the world.

Developers can create their own CACert store using the information provided by
our CACertMan project: https://github.com/guardianproject/cacert

It can be used in combination with the MemorizingTrustManager, to support user
prompted override for non-validating certificates.


# Proxied Connections (aka Orlib)

Once Orbot connects successfully to the Tor network, it offers two proxy
servers running on localhost that applications can route their traffic
through.

HTTP Proxy: localhost:8118
SOCKS 4/5 Proxy: localhost:9050

The sample project shows the basics of how to use this library to open sockets
and make HTTP requests via the SOCKS and HTTP proxies available from
Orbot. The standard `HttpURLConnection` and Apache HTTP Client libraries
provide calls to setup proxying. This sample code demonstrates that.  All
applications using the SOCKS proxy should not resolve their DNS locally, and
instead should pass the hostnames through the SOCKS proxy.


# Orbot Helper

Provides simple helper to check if Orbot is installed, and whether it is
currently running or not. Allows your app to request Orbot to start (user is
optionally prompted whether to start or not). Finally, it can show a user
prompt to install Orbot, either from [Google Play], [F-Droid], or via direct
APK download as a last resort.

For apps with on-device servers, it can also assists in requesting a Tor
Hidden Service from Orbot, and discovering the assigned `.onion` address.

# Downloads

The binary jar, source jar, and javadoc jar are all available on `jcenter()`,
and they all include GPG signatures.  To include this library using gradle,
add this line to your *build.gradle*:

    compile 'info.guardianproject.netcipher:netcipher:1.2'

Otherwise, the files can also be [downloaded directly] from bintray.com.


# Get help

Do not hesitate to contact us with any questions.  The best place to start is
our [community forums] and https://devsq.net.  To send a direct
message, email support@guardianproject.info

We want your feedback!  Please report any problems, bugs or feature requests
to our [issue tracker]:

[issue tracker]: https://dev.guardianproject.info/projects/netcipher/issues
[downloaded directly]: https://dl.bintray.com/guardianproject/CipherKit/info/guardianproject/netcipher/netcipher/
[HttpURLConnection]: https://developer.android.com/reference/java/net/HttpURLConnection.html
[Apache HTTP Client]: https://hc.apache.org/httpcomponents-client-4.3.x/index.html
[Google Play]: https://play.google.com/store/apps/details?id=org.torproject.android
[F-Droid]: https://f-droid.org/repository/browse/?fdid=org.torproject.android
[community forums]: https://guardianproject.info/contact
