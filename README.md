NetCipher: Network Security Library for Android
========

This is an Android Library Project that provides multiple means to improve
network security in mobile applications.

More specifically this library provides:

- A built-in certificate store based on the open Debian root CA trust store (doesn't use the devices CA cert store)
- Hardening of TLS support and Cipher Suites
- Proxied Connection Support: HTTP and SOCKS proxy connection support for HTTP and HTTP/S traffic through specific configuration of the Apache HTTPClient library
- OrbotHelper: a utility class to support application integration with Orbot: Tor for Android. Check if its installed, running, etc.

// REMOVED FOR NOW (UNDER REVIEW FOR VERIFICATION BUG) --> 1. StrongTrustManager: a robust implementation of an TLS/SSL certificate verifier, that can be customized with any set of certificate authorities

IT MUST BE NOTED, that you can use this library without using Orbot/Tor, but obviously we think using strong TLS/SSL connections over Tor is just about the best thing in the world.

Developers can create their own CACert store using the information provided by our CACertMan project:
https://github.com/guardianproject/cacert

It can be used in combination with the MemorizingTrustManager, to support user prompted override for non-validating certificates.

# Proxied Connections (aka Orlib)

Once Orbot connects successfully to the Tor network, it offers two proxy servers running
on localhost that applications can route their traffic through.

HTTP Proxy: localhost:8118
SOCKS 4/5 Proxy: localhost:9050

The sample project shows the basics of how to use this library to open sockets and make HTTP requests via the
SOCKS and HTTP proxies available from Orbot The standard Apache HTTPClient libraries provide calls to setup proxying. This sample code
demonstrates that.  All applications using the SOCKS proxy should not resolve their DNS locally,
and instead should pass the hostnames through the SOCKS proxy. 

# Orbot Helper 

Provides simple helper to check if Orbot (Tor for Android) is installed, and whether it is currently running or not. Allows your app to request Orbot to start (user is prompted whether to start or not). Finally, it can show a user prompt to install Orbot, either from Google Play, or via direct APK download from torproject.org or the guardianproject.info site.

For apps with on-device servers, it can also assists in requesting a Tor Hidden Service from Orbot, and discovering the assigned .ONION address.

# Add this to your AndroidManifest.xml

To have the CertDisplayActivity show up, you need to add this to your app's AndroidManifest.xml:

    <application android:label="@string/app_name" >
       <activity android:name="info.guardianproject.onionkit.ui.CertDisplayActivity"
                android:configChanges="locale|orientation"
                android:theme="@android:style/Theme.Dialog"
                android:taskAffinity="" />
    </application>

If you are targetting android-13 as the minimum, then add in "screenSize" to
configChanges:

                android:configChanges="locale|screenSize|orientation"

