package mtogo.auth.dto;

public class LoginResponse {

    public String token;
    public String token_type = "Bearer";
    public long expires_in;
}
