package mtogo.auth.exceptions;

import io.javalin.http.Context;
import mtogo.auth.exceptions.APIException;


public class ExceptionHandler {

    public static void apiExceptionHandler(APIException e, Context context) {
        context.status(e.getStatusCode());
        context.json(new ErrorMessage(e.getStatusCode(), e.getMessage(), java.time.LocalDateTime.now().toString()));
    }

    private record ErrorMessage(int statusCode, String message, String timestamp) {
    }

}