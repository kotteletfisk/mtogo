package mtogo.sql.env;

public class SystemEnvProvider implements IEnvProvider {

    @Override
    public String getenv(String key) {
        return System.getenv(key);
    }

    
}
