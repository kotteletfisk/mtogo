package mtogo.redis.exceptions;

import io.javalin.http.Context;
import io.javalin.validation.ValidationException;

public class ExceptionHandler {

    public static void apiExceptionHandler(APIException e, Context context) {
        context.status(e.getStatusCode());
        context.json(new ErrorMessage(e.getStatusCode(), e.getMessage(), java.time.LocalDateTime.now().toString()));
    }

    public static void validationExceptionHandler(ValidationException e, Context context) {
        context.json(e.getErrors()).status(400); // Aggregate messages to APIException instead?
    }

    private record ErrorMessage(int statusCode, String message, String timestamp) {
    }

}