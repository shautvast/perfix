package perfix.server.json;

public interface SerializerFactory {
    public <T> JSONSerializer<T> createSerializer(Class<T> beanjavaClass);
}
