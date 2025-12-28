package mtogo.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author MrJustMeDahl
 * The class holds logic concerned with hashing passwords and verifying those password hashes using bcrypt.
 */
public class PasswordService {
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(12);

    /**
     * @author MrJustMeDahl
     * @param rawPassword - password as received externally in plain text.
     * @param hash - hashed password, retrieved internally.
     * @return boolean indicating if the 2 given passwords match or not.
     * Method used for verifying that an entered password matches with the hashed password from our own system.
     */
    public boolean verify(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }

    /**
     * @author MrJustMeDahl
     * @param rawPassword - password as received externally in plain text.
     * @return The password after having been hashed using bcrypt.
     * Method used when new auths are added to the system, it hashes the password so it isn't stored in plain text.
     */
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}