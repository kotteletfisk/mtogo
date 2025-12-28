package mtogo.auth.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


/**
 * @author MrJustMeDahl
 * Class is tasked with handling fetching the private RSA key for the auth-service. Dependent on environment variables it will either fetch from docker secrets or local files.
 */

public class KeyLoader {
    private static final Logger log = LoggerFactory.getLogger(KeyLoader.class);

    /**
     * @author MrJustMeDahl
     * @return String - representing the RSA key in pem format
     * This method is the one you'd call in order to retrieve the private key, it looks at environment variables to decide how the key is fetched - through docker secrets or local files.
     */
    public static String loadPrivateKey() {
        String env = System.getenv("ENV");
        boolean server = "prod".equalsIgnoreCase(env) || "test".equalsIgnoreCase(env);

        if (server) {
            return readSecret("/run/secrets/auth_private_pkcs8.pem");
        }
        return loadOrGenerateLocal();
    }

    /**
     * @author MrJustMeDal
     * @param path - represents the path to the key being read.
     * @return The key as a String in pem format.
     * Helper IO method handling reading the key from the file at the given path. Throws exception in case key can't be found.
     */
    private static String readSecret(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (Exception e) {
            log.debug("Private key secret not found: {}", e.getMessage());
            throw new RuntimeException("Missing private key secret");
        }
    }

    /**
     * @author MrJustMeDahl
     * @return The private key as a String in pem format.
     * Helper IO method handling reading of local keys. It will either read  the keys if they exist or make calls to ensure keys are generated.
     */
    private static String loadOrGenerateLocal() {
        String workspace = System.getenv().getOrDefault("MTOGO_WORKSPACE", ".");
        Path dir = Path.of(workspace, "keys");
        Path priv = dir.resolve("private.pem");

        try {
            if (!Files.exists(priv)) {
                Files.createDirectories(dir);
                keyPairGeneratorUtil(priv, dir.resolve("public.pem"));
            }
            return Files.readString(priv);
        } catch (Exception e) {
            throw new RuntimeException("Could not read/generate local dev keys", e);
        }
    }

    /**
     * @author MrJustMeDahl
     * @param privatePath - file path to where the private key should be located.
     * @param publicPath - file path to where the public key should be located.
     * @throws NoSuchAlgorithmException - exception is thrown if the algorithm given as attribute for the KeyPairGenerator is not recognized.
     * @throws IOException - exception is thrown if writing the keys to given path fails.
     * Helper method that handles generation of public and private keys in the given file locations.
     */
    private static void keyPairGeneratorUtil(Path privatePath, Path publicPath) throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        String privatePem = toPem(pair.getPrivate(), "PRIVATE KEY");
        String publicPem = toPem(pair.getPublic(), "PUBLIC KEY");

        Files.writeString(privatePath, privatePem);
        Files.writeString(publicPath, publicPem);
    }

    /**
     * @author MrJustMeDahl
     * @param key - A key file that is to be transformed to pem format.
     * @param type - String describing which type of key is being transformed.
     * @return String - the key as a String in pem format.
     * Helper method that transforms the keys into pem format.
     */
    private static String toPem(Object key, String type) {
        String base64 = Base64.getEncoder().encodeToString(((java.security.Key) key).getEncoded());
        return "-----BEGIN " + type + "-----\n" +
                base64 + "\n-----END " + type + "-----\n";
    }
}