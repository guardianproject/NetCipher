NetCipher: Secured Networking for Android
========

*Better TLS and Tor App Integration*

NetCipher is a library for Android that provides multiple means to improve
network security in mobile applications.  It provides best practices TLS
settings using the standard Android HTTP methods, [HttpURLConnection] and
[Apache HTTP Client], provides simple Tor integration, makes it easy to
configure proxies for HTTP connections and `WebView` instances.

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

# The Strong Builders

The simplest way to use NetCipher to integrate with Tor via Orbot is
to use the `StrongBuilder` implementations. There is one of these for
each of the four most popular HTTP client APIs for Android:

|HTTP Client API                                                   |`StrongBuilder` Implementation|
|:----------------------------------------------------------------:|:----------------------------:|
|`HttpUrlConnection`                                               |`StrongConnectionBuilder`     |
|[OkHttp3](http://square.github.io/okhttp/)                        |`StrongOkHttpClientBuilder`   |
|[Volley](https://developer.android.com/training/volley/index.html)|`StrongVolleyQueueBuilder`    |
|Apache HttpClient                                                 |`StrongHttpClientBuilder`     |

(HttpClient is supported by means of the `cz.msebera.android:httpclient` artifact,
not the discontinued HttpClient implementation in the Android SDK)

## Requesting the Dependency

You will need up to three dependencies to pull in the right bits for your
project.

At minimum, you will need the `netcipher` base artifact. The `StrongBuilder`
classes are in 2.0.0 and higher:

```groovy
compile 'info.guardianproject.netcipher:netcipher:2.0.0-alpha1'
```

If you are planning on using `HttpURLConnection` and `StrongConnectionBuilder`,
that is all you need.

If you plan on using one of the other supported HTTP client APIs and its
associated builder, you need to *also* request the appropriate artifact
*in addition to* requesting the `netcipher` artifact:

|HTTP Client API      |NetCipher Artifact      |
|:-------------------:|:----------------------:|
|OkHttp3              | `info.guardianproject.netcipher:netcipher-okhttp3`    |
|HttpClient           | `info.guardianproject.netcipher:netcipher-httpclient` |
|Volley               | `info.guardianproject.netcipher:netcipher-volley`     |

Plus, you will need whatever artifact contains your HTTP client API:

|HTTP Client API      |Library Module          |
|:-------------------:|:----------------------:|
|OkHttp3              | `com.squareup.okhttp3:okhttp:3.4.2`    |
|HttpClient           | `cz.msebera.android:httpclient:4.4.1.2` |
|Volley               | `com.android.volley:volley:1.0.0`     |

So, for example, a project wishing to use OkHttp3 and NetCipher together
would have these dependencies, in addition to any others that the
project needs:

```groovy
compile 'info.guardianproject.netcipher:netcipher:2.0.0-alpha1'
compile 'info.guardianproject.netcipher:netcipher-okhttp3:2.0.0-alpha1'
compile 'com.squareup.okhttp3:okhttp:3.4.2'
```

## Creating the OrbotHelper

`OrbotHelper` is a singleton that manages a lot of the asynchronous
communication between your app and Orbot. It is designed to be initialized
fairly early on in your app's lifecycle. One likely candidate is to have
a custom `Application` subclass, where you override `onCreate()` and
set up `OrbotHelper`.

So, you might have something like this:

```java
public class SampleApplication extends Application {
  @Override
  public void onCreate() {
    super.onCreate();

    OrbotHelper.get(this).init();
  }
}
```

`SampleApplication` would need to be registered in your manifest via
the `<application>` tag:

```xml
<application
    android:name=".SampleApplication"
    ...
    >
```

## Creating a Builder

Each of the four builder classes has a `public` constructor, taking a `Context`
as a parameter, that you could use.

A better choice is to call the static `forMaxSecurity()` method, which also
takes a `Context` as a parameter:

```java
StrongOkHttpClientBuilder builder=StrongOkHttpClientBuilder.forMaxSecurity(this)
```

(assuming that `this` is a `Context`, such as an `Activity`)

Note that the `StrongBuilder` classes will hold onto the `Application`
context to avoid memory leaks, so you do not have to worry about that
yourself.

The `forMaxSecurity()` method will ensure that your builder is configured
with defaults that maximize security. In particular, it pre-configures
the builder with `withBestProxy()`, described below.

## Configuring the Builder

If you want, you can call a series of methods on the builder to further
configure its behavior. As the name suggests, methods on these builder
classes return the builder object itself, implementing a builder-style
API.

The key methods are:

- `withBestProxy()`, which chooses either the HTTP or the SOCKS proxy
offered by Orbot, based on which is available for use by the HTTP
client API you are trying to use (e.g., OkHttp3 does not support SOCKS)

- `withHttpProxy()` or `withSocksProxy()`, if you are really sure that
you want to not use `withBestProxy()`

- `withTrustManagers()`, if you have a `TrustManager[]` that you wish
to use to tailor the behavior of any SSL connections made through the
HTTP client API

- `withWeakCiphers()`, if you are running into compatibility issues
with the stock selection of supported ciphers

- `withTorValidation()`, if you want to confirm that not only we
use Orbot, but that the communications via Orbot appear to be
happening over Tor itself

Of these, `withTrustManagers()` is the most likely one to be used,
and then only if you are implementing special SSL handling (e.g.,
certificate pinning).

In addition, if you are using `HttpURLConnection`, you need to call
`connectTo()`, passing in the URL that you wish to connect to
(either as a `String` or a `URL`). This pre-configuration of the URL
is not required for the other three builders, making them much more
flexible and reusable.

## Requesting a Connection

To get a connection, call `build()` on the builder. This takes a
`StrongBuilder.Callback<C>` parameter, where `C` depends on which 
of the four HTTP client APIs you are using:

|HTTP Client API    |`StrongBuilder` Implementation|`Callback Type`                            |
|:------------------|:----------------------------:|:-----------------------------------------:|
|`HttpUrlConnection`|`StrongConnectionBuilder`     |`StrongBuilder.Callback<HttpURLConnection>`|
|OkHttp3            |`StrongOkHttpClientBuilder`   |`StrongBuilder.Callback<OkHttpClient>`     |
|Volley             |`StrongVolleyQueueBuilder`    |`StrongBuilder.Callback<RequestQueue>`     |
|Apache HttpClient  |`StrongHttpClientBuilder`     |`StrongBuilder.Callback<HttpClient>`       |

Your `Callback` needs to implement four methods.

The big one is `void onConnected(C client)`, where you are handed an instance
of your designated HTTP API connection (e.g., an `OkHttpClient` for OkHttp3).
At this point, the client object is set up to communicate through Tor
by means of Orbot, and you are free to start using it for your HTTP requests.
However, do not make any assumptions about the thread on which `onConnected()`
is called; please do your HTTP I/O on your own background thread.

You also need to implement:

- `void onConnectionException(Exception e)`, which is called if we ran
into some problem, so you can report it to the user, log it to your
crash reporting server, etc.

- `void onTimeout()`, which is called if we were unable to talk to Orbot
within 30 seconds

- `void onInvalid()`, which is called if you requested that we validate
the Tor connection and that test failed

Note that `build()` itself may throw an `Exception` as well, which you will
need to address. Otherwise, `build()` is asynchronous; you will find out
the results via your `Callback`. Note that the `Callback` methods may be
invoked on any thread &mdash; do not assume that the methods will be
called on any particular thread.

For example, assuming that `this` implements
`StrongBuilder.Callback<OkHttpClient>`, you could have code like:

```java
private void doThatHttpThing() {
  try {
    StrongOkHttpClientBuilder
      .forMaxSecurity(this)
      .build(this);
  }
  catch (Exception e) {
    // do something useful
  }
}

@Override
public void onConnected(final OkHttpClient client) {
  // use the OkHttpClient on a background thread
}

@Override
public void onConnectionException(Exception e) {
  // do something useful
}

@Override
public void onTimeout() {
  // do something useful
}
```

## Sample Apps

This project contains a sample app for each of the four HTTP client APIs:

|HTTP Client API    |Sample App         |
|:------------------|:-----------------:|
|`HttpUrlConnection`|`sample-hurl`      |
|OkHttp3            |`sample-okhttp3`   |
|Volley             |`sample-volley`    |
|Apache HttpClient  |`sample-httpclient`|

Each of the four apps does the same thing: request the latest Stack Overflow
`android` questions and show them in a list. What differs between the
samples is which dependency and HTTP client API that they use.

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
