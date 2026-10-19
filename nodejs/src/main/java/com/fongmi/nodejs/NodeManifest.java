package com.fongmi.nodejs;

import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Json;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Locale;

import okio.ByteString;

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
            return readManifest(text, object);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Invalid Node manifest", e);
        }
    }

    private static NodeManifest readManifest(String text, JsonObject object) throws IOException {
        String indexSha256 = digest(object, "indexSha256");
        String configSha256 = digest(object, "configSha256");
        byte[] publicKey = Base64.getDecoder().decode(object.get("publicKey").getAsString());
        byte[] signature = Base64.getDecoder().decode(object.get("signature").getAsString());
        if (publicKey.length == 0 || signature.length == 0) throw new IOException("Node manifest key or signature is empty");
        return new NodeManifest(text, indexSha256, configSha256, publicKey, signature);
    }

    private static void verifyDigest(File file, String expected, String name) throws IOException {
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), sha256(file).getBytes(StandardCharsets.US_ASCII))) throw new IOException("Node manifest " + name + " SHA-256 mismatch");
    }

    static byte[] payload(String indexSha256, String configSha256) {
        return (DOMAIN + indexSha256.toLowerCase(Locale.ROOT) + "\n" + configSha256.toLowerCase(Locale.ROOT) + "\n").getBytes(StandardCharsets.US_ASCII);
    }

    static String sha256(File file) throws IOException {
        return ByteString.of(Crypto.sha256(file)).hex();
    }

    private static String digest(JsonObject object, String name) throws IOException {
        String value = object.get(name).getAsString().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-f0-9]{64}")) throw new IOException("Invalid Node manifest " + name);
        return value;
    }

    void verify(File index, File config, String trustedFingerprint) throws IOException {
        verifyDigest(index, indexSha256, "index");
        verifyDigest(config, configSha256, "config");
        String fingerprint = fingerprint();
        if (!trustedFingerprint.isEmpty() && !MessageDigest.isEqual(trustedFingerprint.getBytes(StandardCharsets.US_ASCII), fingerprint.getBytes(StandardCharsets.US_ASCII))) throw new IOException("Node bundle signing key changed");
        verifySignature();
    }

    private void verifySignature() throws IOException {
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
        return ByteString.of(Crypto.newDigest("SHA-256").digest(publicKey)).hex();
    }

    String raw() {
        return raw;
    }
}
