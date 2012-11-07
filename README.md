OnionKit
========

Android Library Project for Multi-Layer Network Connections (Better TLS/SSL and Tor)

Once Orbot connects successfully to the Tor network, it offers two proxy servers running
on localhost that applications can route their traffic through.

HTTP Proxy: localhost:8118
SOCKS 4/5 Proxy: localhost:9050

src/orlib/sample/OrlibMainActivity.java:
for the basics of how to use this library to open sockets and make HTTP requests via the
SOCKS and HTTP proxies available from Orbot

The standard Apache HTTPClient libraries provide calls to setup proxying. This sample code
demonstrates that.

All applications using the SOCKS proxy should not resolve their DNS locally,
and instead should pass the hostnames through the SOCKS proxy. 

