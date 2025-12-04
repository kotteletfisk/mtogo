package mtogo.auth.dto;

public class LoginRequest {

    public String email;
    public String password;
    public serviceRequester service;

    private enum serviceRequester {
        CUSTOMER,
        SUPPLIER,
        COURIER,
        SUPPORT,
        MANAGEMENT
    }
}

