package mtogo.auth.server.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * @author MrJustMeDahl
 * Class tasked with loading the public key needed to validate JWT, either from docker config or local files depending on environment variables.
 */
public class PublicKeyLoader {

    /**
     * @author MrJustMeDahl
     * @return RSAPublicKey - retrieved either from docker config or local files.
     * @throws Exception Can throw several exceptions, IO if file reading goes wrong, NoSuchAlgorithm if algorithm passed to the KeyFactory is not recognised or InvalidKeySpecException if the KeyFactory is not passed a valid public key format. All exceptions have the same result of the public key being invalid and unavailable.
     * Method - used to retrieve the public key needed for validation of JWT.
     */
    public static RSAPublicKey loadPublicKey() throws Exception {

        String env = System.getenv("ENV");
        boolean server = "prod".equalsIgnoreCase(env) || "test".equalsIgnoreCase(env);

        String path;
        if (server) {
            path = ("/run/configs/jwt_public.pem");
        } else {
            String workspace = System.getenv().getOrDefault("MTOGO_WORKSPACE", ".");
            path = Path.of(workspace, "keys", "public.pem").toString();
        }

        String pem = Files.readString(Paths.get(path))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(pem);
        var keySpec = new X509EncodedKeySpec(decoded);
        var kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(keySpec);
    }
}
