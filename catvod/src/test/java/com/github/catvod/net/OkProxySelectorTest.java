package com.github.catvod.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.github.catvod.bean.Proxy;

import org.junit.After;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkProxySelectorTest {

    private final OkProxySelector selector = new OkProxySelector();

    @After
    public void tearDown() {
        selector.clear();
    }

    @Test
    public void redirectChainCarriesProxyAcrossIpTargets() throws Exception {
        java.net.Proxy proxy = proxy(java.net.Proxy.Type.HTTP, 8080);
        addRule("ott.example.com", proxy);
        OkProxySelector.Policy source = selector.policy(new URI("http://ott.example.com/live"), null);
        OkProxySelector.Policy first = selector.policy(new URI("http://233.1.2.3/stream"), source);
        OkProxySelector.Policy second = selector.policy(new URI("http://233.1.2.4/stream"), first);

        assertEquals(List.of(proxy), first.select(new URI("http://233.1.2.3/stream")));
        assertEquals(List.of(proxy), second.select(new URI("http://233.1.2.4/stream")));
        assertSame(source.rule(), second.rule());
    }

    @Test
    public void redirectUsesTargetHostRule() throws Exception {
        java.net.Proxy sourceProxy = proxy(java.net.Proxy.Type.HTTP, 8080);
        java.net.Proxy targetProxy = proxy(java.net.Proxy.Type.SOCKS, 1080);
        addRule("ott.example.com", sourceProxy);
        addRule("cdn.example.com", targetProxy);
        OkProxySelector.Policy source = selector.policy(new URI("http://ott.example.com/live"), null);
        OkProxySelector.Policy target = selector.policy(new URI("http://cdn.example.com/stream"), source);
        OkProxySelector.Policy next = selector.policy(new URI("http://233.1.2.3/stream"), target);

        assertEquals(List.of(targetProxy), target.select(new URI("http://cdn.example.com/stream")));
        assertEquals(List.of(targetProxy), next.select(new URI("http://233.1.2.3/stream")));
        assertNotEquals(source, target);
    }

    @Test
    public void sameIpWithDifferentSourceRulesUsesDifferentPoolPolicy() throws Exception {
        java.net.Proxy firstProxy = proxy(java.net.Proxy.Type.HTTP, 8080);
        java.net.Proxy secondProxy = proxy(java.net.Proxy.Type.HTTP, 8081);
        addRule("first.example.com", firstProxy);
        addRule("second.example.com", secondProxy);
        URI target = new URI("http://233.1.2.3/stream");
        OkProxySelector.Policy first = selector.policy(target, selector.policy(new URI("http://first.example.com/live"), null));
        OkProxySelector.Policy second = selector.policy(target, selector.policy(new URI("http://second.example.com/live"), null));

        assertEquals(List.of(firstProxy), first.select(target));
        assertEquals(List.of(secondProxy), second.select(target));
        assertNotEquals(first, second);
    }

    @Test
    public void redirectDoesNotProxyLoopbackTarget() throws Exception {
        java.net.Proxy proxy = proxy(java.net.Proxy.Type.HTTP, 8080);
        addRule("ott.example.com", proxy);
        OkProxySelector.Policy source = selector.policy(new URI("http://ott.example.com/live"), null);
        OkProxySelector.Policy target = selector.policy(new URI("http://127.0.0.1/stream"), source);

        assertNull(target.rule());
        assertNotEquals(List.of(proxy), target.select(new URI("http://127.0.0.1/stream")));
    }

    @Test
    public void clearOnAnotherThreadInvalidatesInheritedRule() throws Exception {
        java.net.Proxy proxy = proxy(java.net.Proxy.Type.HTTP, 8080);
        addRule("ott.example.com", proxy);
        OkProxySelector.Policy source = selector.policy(new URI("http://ott.example.com/live"), null);
        Thread thread = new Thread(selector::clear);
        thread.start();
        thread.join();
        addRule("other.example.com", proxy);

        assertNull(selector.policy(new URI("http://233.1.2.3/stream"), source).rule());
    }

    @Test
    public void actualRedirectChainStaysOnHttpProxy() throws Exception {
        try (ProxyServer server = new ProxyServer(
                "HTTP/1.1 302 Found\r\nLocation: http://233.1.2.3/stream\r\n",
                "HTTP/1.1 302 Found\r\nLocation: http://233.1.2.4/stream\r\n",
                "HTTP/1.1 200 OK\r\n")) {
            addRule("ott.example.com", server.proxy());
            try (Response response = client().newCall(new Request.Builder().url("http://ott.example.com/live").build()).execute()) {
                assertEquals(200, response.code());
                assertEquals(302, response.priorResponse().code());
            }
            assertEquals(List.of("GET http://ott.example.com/live HTTP/1.1", "GET http://233.1.2.3/stream HTTP/1.1", "GET http://233.1.2.4/stream HTTP/1.1"), server.requests());
        }
    }

    @Test
    public void retryAfterRedirectStaysOnHttpProxy() throws Exception {
        try (ProxyServer server = new ProxyServer(
                "HTTP/1.1 302 Found\r\nLocation: http://233.1.2.3/stream\r\n",
                "HTTP/1.1 503 Service Unavailable\r\nRetry-After: 0\r\n",
                "HTTP/1.1 200 OK\r\n")) {
            addRule("ott.example.com", server.proxy());
            try (Response response = client().newCall(new Request.Builder().url("http://ott.example.com/live").build()).execute()) {
                assertEquals(200, response.code());
                assertEquals(503, response.priorResponse().code());
                assertEquals(302, response.priorResponse().priorResponse().code());
            }
            assertEquals(List.of("GET http://ott.example.com/live HTTP/1.1", "GET http://233.1.2.3/stream HTTP/1.1", "GET http://233.1.2.3/stream HTTP/1.1"), server.requests());
        }
    }

    @Test
    public void proxyAuthenticationAfterRedirectKeepsProxyRule() throws Exception {
        try (ProxyServer server = new ProxyServer(
                "HTTP/1.1 302 Found\r\nLocation: http://233.1.2.3/stream\r\n",
                "HTTP/1.1 407 Proxy Authentication Required\r\nProxy-Authenticate: Basic realm=proxy\r\n",
                "HTTP/1.1 200 OK\r\n")) {
            addRule("ott.example.com", server.proxy(), "user:password");
            try (Response response = client().newCall(new Request.Builder().url("http://ott.example.com/live").build()).execute()) {
                assertEquals(200, response.code());
            }
            assertEquals(List.of("GET http://ott.example.com/live HTTP/1.1", "GET http://233.1.2.3/stream HTTP/1.1", "GET http://233.1.2.3/stream HTTP/1.1"), server.requests());
            assertTrue(server.headers(2).contains("Proxy-Authorization: Basic dXNlcjpwYXNzd29yZA=="));
        }
    }

    @Test
    public void redirectToDifferentProxyDoesNotForwardOldCredentials() throws Exception {
        try (ProxyServer source = new ProxyServer(
                "HTTP/1.1 407 Proxy Authentication Required\r\nProxy-Authenticate: Basic realm=source\r\n",
                "HTTP/1.1 302 Found\r\nLocation: http://cdn.example.com/stream\r\n");
             ProxyServer target = new ProxyServer("HTTP/1.1 200 OK\r\n")) {
            addRule("ott.example.com", source.proxy(), "source:password");
            addRule("cdn.example.com", target.proxy(), "target:password");
            try (Response response = client().newCall(new Request.Builder().url("http://ott.example.com/live").build()).execute()) {
                assertEquals(200, response.code());
            }
            assertEquals(List.of("GET http://ott.example.com/live HTTP/1.1", "GET http://ott.example.com/live HTTP/1.1"), source.requests());
            assertTrue(source.headers(1).contains("Proxy-Authorization: Basic c291cmNlOnBhc3N3b3Jk"));
            assertEquals(List.of("GET http://cdn.example.com/stream HTTP/1.1"), target.requests());
            assertFalse(target.headers(0).stream().anyMatch(header -> header.startsWith("Proxy-Authorization:")));
        }
    }

    @Test
    public void warmDirectConnectionDoesNotBypassRedirectProxy() throws Exception {
        try (DirectServer direct = new DirectServer(); ProxyServer server = new ProxyServer(
                "HTTP/1.1 302 Found\r\nLocation: " + direct.url() + "\r\n",
                "HTTP/1.1 200 OK\r\n")) {
            OkHttpClient client = client();
            try (Response response = client.newCall(new Request.Builder().url(direct.url()).build()).execute()) {
                assertEquals(200, response.code());
            }
            addRule("ott.example.com", server.proxy());
            try (Response response = client.newCall(new Request.Builder().url("http://ott.example.com/live").build()).execute()) {
                assertEquals(200, response.code());
            }
            assertEquals(List.of("GET http://ott.example.com/live HTTP/1.1", "GET " + direct.url() + " HTTP/1.1"), server.requests());
            assertEquals(List.of("GET /stream HTTP/1.1"), direct.requests());
        }
    }

    @Test
    public void noRedirectClientReturnsRedirectResponse() throws Exception {
        try (ProxyServer server = new ProxyServer("HTTP/1.1 302 Found\r\nLocation: http://233.1.2.3/stream\r\n")) {
            addRule("ott.example.com", server.proxy());
            OkHttpClient.Builder builder = client().newBuilder();
            builder.interceptors().removeIf(item -> item instanceof ProxyRedirectInterceptor);
            try (Response response = builder.build().newCall(new Request.Builder().url("http://ott.example.com/live").build()).execute()) {
                assertEquals(302, response.code());
            }
            assertEquals(List.of("GET http://ott.example.com/live HTTP/1.1"), server.requests());
        }
    }

    @Test
    public void crossSchemeRedirectRespectsClientSetting() throws Exception {
        try (ProxyServer server = new ProxyServer("HTTP/1.1 302 Found\r\nLocation: https://233.1.2.3/stream\r\n")) {
            addRule("ott.example.com", server.proxy());
            OkHttpClient client = client().newBuilder().followSslRedirects(false).build();
            try (Response response = client.newCall(new Request.Builder().url("http://ott.example.com/live").build()).execute()) {
                assertEquals(302, response.code());
            }
            assertEquals(List.of("GET http://ott.example.com/live HTTP/1.1"), server.requests());
        }
    }

    @Test
    public void redirect302ChangesPostToGetAndDropsCrossHostAuthorization() throws Exception {
        try (ProxyServer server = new ProxyServer("HTTP/1.1 302 Found\r\nLocation: http://233.1.2.3/stream\r\n", "HTTP/1.1 200 OK\r\n")) {
            addRule("ott.example.com", server.proxy());
            Request request = new Request.Builder().url("http://ott.example.com/live").header("Authorization", "Bearer secret").post(RequestBody.create("", null)).build();
            try (Response response = client().newCall(request).execute()) {
                assertEquals(200, response.code());
            }
            assertEquals(List.of("POST http://ott.example.com/live HTTP/1.1", "GET http://233.1.2.3/stream HTTP/1.1"), server.requests());
            assertTrue(server.headers(0).contains("Authorization: Bearer secret"));
            assertFalse(server.headers(1).stream().anyMatch(header -> header.startsWith("Authorization:")));
        }
    }

    @Test
    public void redirect307PreservesPostMethod() throws Exception {
        try (ProxyServer server = new ProxyServer("HTTP/1.1 307 Temporary Redirect\r\nLocation: http://233.1.2.3/stream\r\n", "HTTP/1.1 200 OK\r\n")) {
            addRule("ott.example.com", server.proxy());
            Request request = new Request.Builder().url("http://ott.example.com/live").post(RequestBody.create("", null)).build();
            try (Response response = client().newCall(request).execute()) {
                assertEquals(200, response.code());
            }
            assertEquals(List.of("POST http://ott.example.com/live HTTP/1.1", "POST http://233.1.2.3/stream HTTP/1.1"), server.requests());
        }
    }

    private OkHttpClient client() {
        return new OkHttpClient.Builder().proxySelector(selector).proxyAuthenticator(new OkAuthenticator(selector)).addInterceptor(new ProxyRedirectInterceptor(selector)).followRedirects(false).connectTimeout(1, TimeUnit.SECONDS).build();
    }

    private void addRule(String host, java.net.Proxy javaProxy) {
        addRule(host, javaProxy, null);
    }

    private void addRule(String host, java.net.Proxy javaProxy, String userInfo) {
        selector.getProxy().add(new Proxy() {
            @Override
            public List<String> getHosts() {
                return List.of(host);
            }

            @Override
            public List<java.net.Proxy> getProxies() {
                return List.of(javaProxy);
            }

            @Override
            public String getUserInfo(String proxyHost, String scheme) {
                return userInfo;
            }
        });
    }

    private java.net.Proxy proxy(java.net.Proxy.Type type, int port) {
        return new java.net.Proxy(type, InetSocketAddress.createUnresolved("proxy.example.com", port));
    }

    private static String readRequest(Socket socket) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        String request = reader.readLine();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) { }
        return request;
    }

    private static final class ProxyServer implements AutoCloseable {

        private final ServerSocket server;
        private final ExecutorService executor;
        private final Future<List<String>> requests;
        private final List<List<String>> headers;

        private ProxyServer(String... replies) throws Exception {
            server = new ServerSocket(0, 2, InetAddress.getLoopbackAddress());
            server.setSoTimeout(3000);
            executor = Executors.newSingleThreadExecutor();
            headers = new ArrayList<>();
            requests = executor.submit(() -> {
                List<String> lines = new ArrayList<>();
                for (String reply : replies) {
                    try (Socket socket = server.accept()) {
                        socket.setSoTimeout(3000);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                        lines.add(reader.readLine());
                        List<String> requestHeaders = new ArrayList<>();
                        String header;
                        while ((header = reader.readLine()) != null && !header.isEmpty()) requestHeaders.add(header);
                        headers.add(requestHeaders);
                        socket.getOutputStream().write((reply + "Content-Length: 0\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                        socket.getOutputStream().flush();
                    }
                }
                return lines;
            });
        }

        private java.net.Proxy proxy() {
            return new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(InetAddress.getLoopbackAddress(), server.getLocalPort()));
        }

        private List<String> requests() throws Exception {
            return requests.get(5, TimeUnit.SECONDS);
        }

        private List<String> headers(int index) throws Exception {
            requests();
            return headers.get(index);
        }

        @Override
        public void close() throws Exception {
            server.close();
            executor.shutdownNow();
        }
    }

    private static final class DirectServer implements AutoCloseable {

        private final ServerSocket server;
        private final ExecutorService executor;
        private final Future<List<String>> requests;

        private DirectServer() throws Exception {
            server = new ServerSocket(0, 2, InetAddress.getByName("127.0.0.2"));
            executor = Executors.newSingleThreadExecutor();
            requests = executor.submit(() -> {
                List<String> lines = new ArrayList<>();
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(3000);
                    lines.add(readRequest(socket));
                    socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                    socket.setSoTimeout(1000);
                    try {
                        String request = readRequest(socket);
                        if (request != null) {
                            lines.add(request);
                            socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                        }
                    } catch (SocketTimeoutException ignored) { }
                }
                return lines;
            });
        }

        private String url() {
            return "http://127.0.0.2:" + server.getLocalPort() + "/stream";
        }

        private List<String> requests() throws Exception {
            return requests.get(5, TimeUnit.SECONDS);
        }

        @Override
        public void close() throws Exception {
            server.close();
            executor.shutdownNow();
        }
    }
}
