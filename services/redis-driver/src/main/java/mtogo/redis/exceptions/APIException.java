package mtogo.redis.exceptions;

public class APIException extends Exception{

    private final int statusCode;

    public APIException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {return statusCode;}

}