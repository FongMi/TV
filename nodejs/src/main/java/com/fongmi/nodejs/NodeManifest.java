package com.fongmi.nodejs;

import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Json;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Locale;

final class NodeManifest {

    static final String FILE_NAME = "index.manifest.json";
    private static final String ALGORITHM = "SHA256withRSA";
    private static final String DOMAIN = "catvod-node-bundle-v1\n";

    private final String raw;
    private final String indexSha256;
    private final String configSha256;
    private final byte[] publicKey;
    private final byte[] signature;

    private NodeManifest(String raw, String indexSha256, String configSha256, byte[] publicKey, byte[] signature) {
        this.raw = raw;
        this.indexSha256 = indexSha256;
        this.configSha256 = configSha256;
        this.publicKey = publicKey;
        this.signature = signature;
    }

    static NodeManifest parse(String text) throws IOException {
        try {
            JsonObject object = Json.parse(text).getAsJsonObject();
            if (object.get("version").getAsInt() != 1) throw new IOException("Unsupported Node manifest version");
            if (!ALGORITHM.equalsIgnoreCase(object.get("algorithm").getAsString())) throw new IOException("Unsupported Node manifest algorithm");
            String indexSha256 = digest(object, "indexSha256");
            String configSha256 = digest(object, "configSha256");
            byte[] publicKey = Base64.getDecoder().decode(object.get("publicKey").getAsString());
            byte[] signature = Base64.getDecoder().decode(object.get("signature").getAsString());
            if (publicKey.length == 0 || signature.length == 0) throw new IOException("Node manifest key or signature is empty");
            return new NodeManifest(text, indexSha256, configSha256, publicKey, signature);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Invalid Node manifest", e);
        }
    }

    void verify(File index, File config, String trustedFingerprint) throws IOException {
        if (!MessageDigest.isEqual(indexSha256.getBytes(StandardCharsets.US_ASCII), sha256(index).getBytes(StandardCharsets.US_ASCII))) throw new IOException("Node manifest index SHA-256 mismatch");
        if (!MessageDigest.isEqual(configSha256.getBytes(StandardCharsets.US_ASCII), sha256(config).getBytes(StandardCharsets.US_ASCII))) throw new IOException("Node manifest config SHA-256 mismatch");
        String fingerprint = fingerprint();
        if (!trustedFingerprint.isEmpty() && !MessageDigest.isEqual(trustedFingerprint.getBytes(StandardCharsets.US_ASCII), fingerprint.getBytes(StandardCharsets.US_ASCII))) throw new IOException("Node bundle signing key changed");
        try {
            PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
            if (!(key instanceof RSAPublicKey rsa) || rsa.getModulus().bitLength() < 2048) throw new IOException("Node manifest RSA key is too small");
            Signature verifier = Signature.getInstance(ALGORITHM);
            verifier.initVerify(key);
            verifier.update(payload(indexSha256, configSha256));
            if (!verifier.verify(signature)) throw new IOException("Invalid Node manifest signature");
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Unable to verify Node manifest", e);
        }
    }

    String fingerprint() {
        MessageDigest digest = Crypto.newDigest("SHA-256");
        return hex(digest.digest(publicKey));
    }

    String raw() {
        return raw;
    }

    static byte[] payload(String indexSha256, String configSha256) {
        return (DOMAIN + indexSha256.toLowerCase(Locale.ROOT) + "\n" + configSha256.toLowerCase(Locale.ROOT) + "\n").getBytes(StandardCharsets.US_ASCII);
    }

    static String sha256(File file) throws IOException {
        MessageDigest digest = Crypto.newDigest("SHA-256");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return hex(digest.digest());
    }

    private static String digest(JsonObject object, String name) throws IOException {
        String value = object.get(name).getAsString().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-f0-9]{64}")) throw new IOException("Invalid Node manifest " + name);
        return value;
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) builder.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return builder.toString();
    }
}
