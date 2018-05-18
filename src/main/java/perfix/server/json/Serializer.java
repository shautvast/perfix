package perfix.server.json;

public class Serializer {
    private static SerializerFactory instance = new SynthSerializerFactory();

    public static String toJSONString(boolean b) {
        return Boolean.toString(b);
    }

    public static String toJSONString(short s) {
        return Short.toString(s);
    }

    public static String toJSONString(int i) {
        return Integer.toString(i);
    }

    public static String toJSONString(float f) {
        return Float.toString(f);
    }

    public static String toJSONString(double d) {
        return Double.toString(d);
    }

    public static String toJSONString(long l) {
        return Long.toString(l);
    }

    public static String toJSONString(char c) {
        return "\"" + Character.toString(c) + "\"";
    }

    @SuppressWarnings("unchecked")
    public static <T> String toJSONString(T o) {
        if (o == null) {
            return "null";
        }
        return instance.createSerializer((Class<T>) o.getClass()).toJSONString(o);
    }

    public static void setInstance(SerializerFactory instance) {
        Serializer.instance = instance;
    }
}
