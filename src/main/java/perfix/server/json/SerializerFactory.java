package perfix.server.json;

public interface SerializerFactory {
    <T> JSONSerializer<T> createSerializer(Class<T> beanjavaClass);
}
