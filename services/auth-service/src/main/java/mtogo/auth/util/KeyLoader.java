package mtogo.auth.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import java.security.*;
import java.util.Base64;

public class KeyLoader {
    private static final Logger log = LoggerFactory.getLogger(KeyLoader.class);

    public static String loadPrivateKey() {
        String env = System.getenv("ENV");
        boolean server = "prod".equalsIgnoreCase(env) || "test".equalsIgnoreCase(env);

        if (server) {
            return readSecret("/run/secrets/auth_private_pkcs8.pem");
        }
        return loadOrGenerateLocal();
    }

    private static String readSecret(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (Exception e) {
            log.debug("Private key secret not found: {}", e.getMessage());
            throw new RuntimeException("Missing private key secret");
        }
    }

    private static String loadOrGenerateLocal() {
        Path dir = Path.of("keys");
        Path priv = dir.resolve("private.pem");

        try {
            if (!Files.exists(priv)) {
                Files.createDirectories(dir);
                keyPairGeneratorUtil(priv, dir.resolve("public.pem"));
            }
            return Files.readString(priv);
        } catch (Exception e) {
            throw new RuntimeException("Could not read/generate local dev keys");
        }
    }

    private static void keyPairGeneratorUtil(Path privatePath, Path publicPath) throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        String privatePem = toPem(pair.getPrivate(), "PRIVATE KEY");
        String publicPem = toPem(pair.getPublic(), "PUBLIC KEY");

        Files.writeString(privatePath, privatePem);
        Files.writeString(publicPath, publicPem);
    }

    private static String toPem(Object key, String type) {
        String base64 = Base64.getEncoder().encodeToString(((java.security.Key) key).getEncoded());
        return "-----BEGIN " + type + "-----\n" +
                base64 + "\n-----END " + type + "-----\n";
    }
}