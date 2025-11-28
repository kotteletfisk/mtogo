package mtogo.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordService {
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public boolean verify(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}