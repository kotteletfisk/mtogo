package mtogo.auth.controller;

import io.javalin.http.Context;

public class AuthController {

    private static AuthController instance;

    public static AuthController getInstance(){
        if (instance == null){
            instance = new AuthController();
        }
        return instance;
    }

    public void login(Context ctx) {

    }
}
