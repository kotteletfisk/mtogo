package mtogo.sql.ports.out;

public interface IRpcResponderFactory<T> {
        IRpcResponder create(T delivery);
}
