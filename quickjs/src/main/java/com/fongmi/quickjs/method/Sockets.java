package com.fongmi.quickjs.method;

import com.fongmi.quickjs.QuickJS;
import com.fongmi.quickjs.utils.Connect;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.Net;
import com.github.catvod.net.NetSocket;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class Sockets {

    private static final int MAX_PENDING = 256;

    private final Global global;
    private final Network network;
    private final AtomicInteger nextId = new AtomicInteger();
    private final Map<Integer, Connection> connections = new ConcurrentHashMap<>();

    Sockets(Global global, Network network) {
        this.global = global;
        this.network = network;
    }

    Integer connect(String url, QuickJS.Json options, QuickJS.Callback callback) throws Exception {
        JsonObject json;
        Net client;
        try {
            global.checkInvocation();
            json = Network.parseOptions(options);
            client = network.client(json);
        } catch (Exception error) {
            callback.close();
            throw error;
        }
        int id = nextId.incrementAndGet();
        Connection connection = new Connection(id, client, callback);
        connections.put(id, connection);
        start(connection, url, json);
        return id;
    }

    private void start(Connection connection, String url, JsonObject json) throws Exception {
        try {
            Invocation current = global.current();
            if (current != null) current.track(connection.lifetime);
            connection.socket = connection.owner.connect(url, json.toString(), connection::receive);
            if (connection.lifetime.isCancelled()) connection.socket.cancel();
        } catch (Exception e) {
            connections.remove(connection.id);
            connection.lifetime.complete(null);
            connection.release();
            throw e;
        }
    }

    Boolean send(Integer id, String text, byte[] bytes) {
        Connection connection = connections.get(id);
        if (connection == null || connection.socket == null) return false;
        return text == null ? connection.socket.send(bytes) : connection.socket.send(text);
    }

    void close(Integer id, Boolean cancel) {
        Connection connection = connections.get(id);
        if (connection == null || connection.socket == null) return;
        connection.closing = true;
        if (Boolean.TRUE.equals(cancel)) connection.socket.cancel();
        else connection.socket.close();
    }

    void markClosing() {
        for (Connection connection : connections.values()) connection.closing = true;
    }

    void markClosing(Net owner) {
        for (Connection connection : connections.values()) if (connection.owner == owner) connection.closing = true;
    }

    void release() {
        for (Connection connection : connections.values()) connection.release();
        connections.clear();
    }

    private final class Connection {

        private final int id;
        private final Net owner;
        private final QuickJS.Callback callback;
        private final AtomicInteger pending = new AtomicInteger();
        private final CompletableFuture<Void> lifetime = new CompletableFuture<>();
        private volatile NetSocket socket;
        private volatile boolean closing;
        private volatile boolean overflow;
        private boolean released;

        private Connection(int id, Net owner, QuickJS.Callback callback) {
            this.id = id;
            this.owner = owner;
            this.callback = callback;
            lifetime.whenComplete((value, error) -> {
                if (lifetime.isCancelled() && socket != null) socket.cancel();
            });
        }

        private void receive(NetSocket socket, NetSocket.Event event) {
            this.socket = socket;
            boolean terminal = event.type.equals("error") || event.type.equals("close");
            if (event.type.equals("close")) lifetime.complete(null);
            if (pending.incrementAndGet() > MAX_PENDING && !terminal) {
                pending.decrementAndGet();
                overflow = true;
                socket.cancel();
                return;
            }
            if (!global.submit(() -> dispatchQueued(event, terminal))) {
                pending.decrementAndGet();
                socket.cancel();
            }
        }

        private void dispatchQueued(NetSocket.Event event, boolean terminal) {
            pending.decrementAndGet();
            if (released || global.isDestroyed() || !terminal && (closing || overflow || lifetime.isCancelled())) return;
            dispatch(event);
        }

        private void dispatch(NetSocket.Event event) {
            Invocation previous = global.setInvocation(null);
            try {
                if (overflow && event.type.equals("close")) deliver(new QuickJS.Json("{\"type\":\"error\",\"error\":\"WebSocket listener queue exceeded 256 events\"}"));
                deliver(Connect.event(event));
            } finally {
                global.setInvocation(previous);
                if (event.type.equals("close")) {
                    connections.remove(id);
                    release();
                }
            }
        }

        private void release() {
            if (released) return;
            released = true;
            callback.close();
        }

        private void deliver(Object event) {
            callback.call(event).whenComplete((result, error) -> {
                if (error != null) {
                    SpiderDebug.log(error);
                    socket.cancel();
                }
            });
        }
    }
}
