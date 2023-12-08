package com.github.catvod.net;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

public class SSLCompat extends SSLSocketFactory {

    private SSLSocketFactory factory;
    private String[] cipherSuites;
    private String[] protocols;

    public SSLCompat() {
        try {
            List<String> list = new LinkedList<>();
            SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
            for (String protocol : socket.getSupportedProtocols()) if (!protocol.toUpperCase().contains("SSL")) list.add(protocol);
            protocols = list.toArray(new String[0]);
            List<String> allowedCiphers = Arrays.asList("TLS_RSA_WITH_AES_256_GCM_SHA384", "TLS_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_ECHDE_RSA_WITH_AES_128_GCM_SHA256", "TLS_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA", "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
            List<String> availableCiphers = Arrays.asList(socket.getSupportedCipherSuites());
            HashSet<String> preferredCiphers = new HashSet<>(allowedCiphers);
            preferredCiphers.retainAll(availableCiphers);
            preferredCiphers.addAll(new HashSet<>(Arrays.asList(socket.getEnabledCipherSuites())));
            cipherSuites = preferredCiphers.toArray(new String[0]);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new X509TrustManager[]{TM}, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(factory = context.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return cipherSuites;
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return cipherSuites;
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        Socket ssl = factory.createSocket(s, host, port, autoClose);
        if (ssl instanceof SSLSocket) upgradeTLS((SSLSocket) ssl);
        return ssl;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket ssl = factory.createSocket(host, port);
        if (ssl instanceof SSLSocket) upgradeTLS((SSLSocket) ssl);
        return ssl;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        Socket ssl = factory.createSocket(host, port, localHost, localPort);
        if (ssl instanceof SSLSocket) upgradeTLS((SSLSocket) ssl);
        return ssl;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket ssl = factory.createSocket(host, port);
        if (ssl instanceof SSLSocket) upgradeTLS((SSLSocket) ssl);
        return ssl;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket ssl = factory.createSocket(address, port, localAddress, localPort);
        if (ssl instanceof SSLSocket) upgradeTLS((SSLSocket) ssl);
        return ssl;
    }

    private void upgradeTLS(SSLSocket ssl) {
        if (protocols != null) ssl.setEnabledProtocols(protocols);
        if (cipherSuites != null) ssl.setEnabledCipherSuites(cipherSuites);
    }

    @SuppressLint({"TrustAllX509TrustManager", "CustomX509TrustManager"})
    public static final X509TrustManager TM = new X509TrustManager() {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    };
}
