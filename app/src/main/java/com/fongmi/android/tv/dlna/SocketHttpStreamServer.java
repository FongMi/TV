package com.fongmi.android.tv.dlna;

import org.jupnp.model.message.Connection;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.Router;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamServer;
import org.jupnp.transport.spi.StreamServerConfiguration;
import org.jupnp.transport.spi.UpnpStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SocketHttpStreamServer implements StreamServer<SocketHttpStreamServer.Configuration> {

    private static final int MAX_BODY_BYTES = 1024 * 1024;
    private static final int MAX_HEADER_BYTES = 64 * 1024;
    private static final int MAX_LINE_BYTES = 8 * 1024;

    private final Configuration configuration;
    private final Set<Socket> sockets = new HashSet<>();
    private ServerSocket serverSocket;
    private volatile boolean stopped;
    private Router router;

    public SocketHttpStreamServer(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void init(InetAddress bindAddress, Router router) throws InitializationException {
        this.router = router;
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(bindAddress, configuration.getListenPort()), 50);
        } catch (IOException e) {
            throw new InitializationException("Could not bind HTTP server socket on " + bindAddress, e);
        }
    }

    @Override
    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : -1;
    }

    @Override
    public void stop() {
        synchronized (sockets) {
            stopped = true;
            for (Socket socket : sockets) closeSocket(socket);
            sockets.clear();
        }
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void run() {
        while (!stopped) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
                if (!dispatchSocket(socket)) break;
                socket = null;
            } catch (SocketException e) {
                break;
            } catch (IOException ignored) {
            } finally {
                if (socket != null) releaseSocket(socket);
            }
        }
    }

    private static void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private boolean dispatchSocket(Socket socket) throws IOException {
        synchronized (sockets) {
            if (stopped) return false;
            sockets.add(socket);
        }
        socket.setSoTimeout(30_000);
        router.received(new SocketUpnpStream(router.getProtocolFactory(), socket));
        return true;
    }

    private void releaseSocket(Socket socket) {
        synchronized (sockets) {
            sockets.remove(socket);
        }
        closeSocket(socket);
    }

    private class SocketUpnpStream extends UpnpStream {

        private final Socket socket;

        SocketUpnpStream(ProtocolFactory protocolFactory, Socket socket) {
            super(protocolFactory);
            this.socket = socket;
        }

        private String readLine(InputStream is) throws IOException {
            StringBuilder sb = new StringBuilder();
            int prev = -1;
            int b;
            while ((b = is.read()) != -1) {
                if (prev == '\r' && b == '\n') {
                    sb.deleteCharAt(sb.length() - 1);
                    return sb.toString();
                }
                sb.append((char) b);
                if (sb.length() > MAX_LINE_BYTES) throw new IOException("HTTP line too long");
                prev = b;
            }
            throw new EOFException("Truncated HTTP line");
        }

        private void writeStatusLine(OutputStream os, int code, String reason) throws IOException {
            os.write(("HTTP/1.1 " + code + " " + Objects.requireNonNullElse(reason, "") + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
        }

        private void writeHeader(OutputStream os, String name, String value) throws IOException {
            os.write((name + ": " + value + "\r\n").getBytes(StandardCharsets.ISO_8859_1));
        }

        private void writeEndHeaders(OutputStream os) throws IOException {
            os.write("\r\n".getBytes(StandardCharsets.ISO_8859_1));
        }

        @Override
        public void run() {
            try {
                StreamRequestMessage request = readRequest();
                if (request == null) return;
                sendResponse(process(request));
            } catch (Exception e) {
                responseException(e);
            } finally {
                synchronized (sockets) {
                    sockets.remove(socket);
                }
                closeSocket(socket);
            }
        }

        private StreamRequestMessage readRequest() throws IOException {
            InputStream is = socket.getInputStream();
            String requestLine = readLine(is);
            String[] parts = requestLine.split(" ", 3);
            if (requestLine.isEmpty() || parts.length < 2) return null;
            Map<String, List<String>> headers = readHeaders(is);
            StreamRequestMessage request = buildRequestMessage(parts[0], parts[1], headers);
            readBodyInto(is, request, headers);
            return request;
        }

        private void sendResponse(StreamResponseMessage response) throws IOException {
            OutputStream os = socket.getOutputStream();
            writeResponse(os, response);
            os.flush();
            responseSent(response);
        }

        private Map<String, List<String>> readHeaders(InputStream is) throws IOException {
            Map<String, List<String>> headers = new HashMap<>();
            String line;
            int size = 0;
            while (!(line = readLine(is)).isEmpty()) {
                size += line.length() + 2;
                if (size > MAX_HEADER_BYTES) throw new IOException("HTTP headers too large");
                addHeader(headers, line);
            }
            return headers;
        }

        private StreamRequestMessage buildRequestMessage(String method, String rawUri, Map<String, List<String>> headers) {
            StreamRequestMessage msg = new StreamRequestMessage(UpnpRequest.Method.getByHttpName(method), URI.create(rawUri));
            msg.setConnection(new SocketConnection(socket));
            msg.setHeaders(new UpnpHeaders(headers));
            return msg;
        }

        private void readBodyInto(InputStream is, StreamRequestMessage msg, Map<String, List<String>> headers) throws IOException {
            int len = readContentLength(headers);
            if (len == 0) return;
            byte[] body = new byte[len];
            int offset = 0, read;
            while (offset < len && (read = is.read(body, offset, len - offset)) != -1) offset += read;
            if (offset != len) throw new EOFException("Truncated HTTP body");
            if (msg.isContentTypeMissingOrText()) msg.setBodyCharacters(body);
            else msg.setBody(UpnpMessage.BodyType.BYTES, body);
        }

        private void addHeader(Map<String, List<String>> headers, String line) {
            int colon = line.indexOf(':');
            if (colon < 0) return;
            headers.computeIfAbsent(line.substring(0, colon).trim().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(line.substring(colon + 1).trim());
        }

        private int readContentLength(Map<String, List<String>> headers) throws IOException {
            List<String> length = headers.getOrDefault("content-length", List.of());
            if (headers.containsKey("transfer-encoding")) throw new IOException("Unsupported Transfer-Encoding");
            if (length.isEmpty()) return 0;
            long declared;
            try {
                declared = Long.parseLong(length.get(0));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid Content-Length", e);
            }
            if (length.size() != 1 || declared < 0 || declared > MAX_BODY_BYTES) throw new IOException("Invalid Content-Length");
            return (int) declared;
        }

        private void writeResponse(OutputStream os, StreamResponseMessage msg) throws IOException {
            if (msg == null) {
                writeStatusLine(os, 404, "Not Found");
                writeHeader(os, "Content-Length", "0");
                writeEndHeaders(os);
                return;
            }
            writeStatusLine(os, msg.getOperation().getStatusCode(), msg.getOperation().getStatusMessage());
            for (Map.Entry<String, List<String>> e : msg.getHeaders().entrySet()) {
                if (e.getKey() == null) continue;
                for (String v : e.getValue()) writeHeader(os, e.getKey(), v);
            }
            byte[] body = msg.hasBody() ? msg.getBodyBytes() : null;
            writeHeader(os, "Content-Length", String.valueOf(body != null ? body.length : 0));
            writeEndHeaders(os);
            if (body != null && body.length > 0) os.write(body);
        }
    }

    private record SocketConnection(Socket socket) implements Connection {

        @Override
        public boolean isOpen() {
            return !socket.isClosed();
        }

        @Override
        public InetAddress getRemoteAddress() {
            return socket.getInetAddress();
        }

        @Override
        public InetAddress getLocalAddress() {
            return socket.getLocalAddress();
        }
    }

    public record Configuration(int listenPort) implements StreamServerConfiguration {

        @Override
        public int getListenPort() {
            return listenPort;
        }
    }
}
