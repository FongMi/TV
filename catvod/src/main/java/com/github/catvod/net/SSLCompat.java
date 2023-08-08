package com.github.catvod.net;

import org.conscrypt.Conscrypt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.Provider;
import java.security.Security;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SSLCompat extends SSLSocketFactory {

    private final SSLSocketFactory socketFactory;
    public static final X509TrustManager TM;

    static {
        try {
            TM = Conscrypt.getDefaultX509TrustManager();
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public SSLCompat(SSLSocketFactory factory) {
        HttpsURLConnection.setDefaultSSLSocketFactory(factory);
        socketFactory = factory;
    }

    public static SSLCompat get() {
        try {
            Provider provider = Conscrypt.newProvider();
            Security.insertProviderAt(provider, 1);
            SSLContext context = SSLContext.getInstance("TLS", provider);
            context.init(null, new TrustManager[]{TM}, null);
            return new SSLCompat(context.getSocketFactory());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return socketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return socketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return enableTLSOnSocket(socketFactory.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(socketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return enableTLSOnSocket(socketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return enableTLSOnSocket(socketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(socketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(socketFactory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
        if (socket instanceof SSLSocket) ((SSLSocket) socket).setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
        return socket;
    }
}
