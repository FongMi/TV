package com.github.catvod.net;

import com.github.catvod.bean.Proxy;
import com.github.catvod.utils.Util;

import java.io.IOException;
import java.net.Authenticator;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class OkProxySelector extends ProxySelector {

    private final List<Proxy> proxy;
    private final ProxySelector system;
    private volatile int generation;
    private boolean authSet;

    public OkProxySelector() {
        proxy = new CopyOnWriteArrayList<>();
        system = ProxySelector.getDefault();
        Authenticator.setDefault(new ProxyAuthenticator(this));
    }

    public synchronized void addAll(List<Proxy> items) {
        if (items.isEmpty()) return;
        items.forEach(Proxy::init);
        proxy.addAll(items);
        proxy.sort(null);
    }

    public synchronized void clear() {
        Authenticator.setDefault(null);
        generation++;
        proxy.clear();
    }

    public List<Proxy> getProxy() {
        return proxy;
    }

    private List<java.net.Proxy> fallback(URI uri) {
        return system != null ? system.select(uri) : List.of(java.net.Proxy.NO_PROXY);
    }

    @Override
    public List<java.net.Proxy> select(URI uri) {
        if (proxy.isEmpty() || uri.getHost() == null || "127.0.0.1".equals(uri.getHost())) return fallback(uri);
        Proxy item = find(uri);
        if (item != null) return !item.getProxies().isEmpty() ? item.getProxies() : fallback(uri);
        return fallback(uri);
    }

    Policy policy(URI uri, Policy previous) {
        Proxy item = find(uri);
        if (item == null && previous != null && previous.rule != null && previous.generation == generation && proxy.contains(previous.rule) && !previous.rule.getProxies().isEmpty() && uri.getHost() != null && !"127.0.0.1".equals(uri.getHost())) item = previous.rule;
        List<java.net.Proxy> proxies = item != null && !item.getProxies().isEmpty() ? item.getProxies() : fallback(uri);
        return new Policy(uri, item, proxies, system, generation);
    }

    Proxy find(URI uri) {
        if (uri == null || uri.getHost() == null || "127.0.0.1".equals(uri.getHost())) return null;
        for (Proxy item : proxy) for (String host : item.getHosts()) if (Util.containOrMatch(uri.getHost(), host)) return item;
        return null;
    }

    @Override
    public void connectFailed(URI uri, SocketAddress socketAddress, IOException e) {
        if (system != null) system.connectFailed(uri, socketAddress, e);
    }

    static final class Policy extends ProxySelector {

        private final Proxy rule;
        private final List<java.net.Proxy> proxies;
        private final ProxySelector system;
        private final int generation;
        private final String scheme;
        private final String host;
        private final int port;

        private Policy(URI uri, Proxy rule, List<java.net.Proxy> proxies, ProxySelector system, int generation) {
            this.scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            this.host = uri.getHost().toLowerCase(Locale.ROOT);
            this.port = getPort(uri);
            this.rule = rule;
            this.proxies = List.copyOf(proxies);
            this.system = system;
            this.generation = generation;
        }

        Proxy rule() {
            return rule;
        }

        boolean sharesProxyCredentials(Policy other) {
            return other != null && rule == other.rule && system == other.system && generation == other.generation && proxies.equals(other.proxies);
        }

        @Override
        public List<java.net.Proxy> select(URI uri) {
            return proxies;
        }

        @Override
        public void connectFailed(URI uri, SocketAddress socketAddress, IOException e) {
            if (system != null) system.connectFailed(uri, socketAddress, e);
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Policy other)) return false;
            return rule == other.rule && system == other.system && generation == other.generation && port == other.port && scheme.equals(other.scheme) && host.equals(other.host) && proxies.equals(other.proxies);
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(rule), System.identityHashCode(system), generation, scheme, host, port, proxies);
        }

        private static int getPort(URI uri) {
            if (uri.getPort() != -1) return uri.getPort();
            if ("http".equalsIgnoreCase(uri.getScheme())) return 80;
            if ("https".equalsIgnoreCase(uri.getScheme())) return 443;
            return -1;
        }
    }
}
