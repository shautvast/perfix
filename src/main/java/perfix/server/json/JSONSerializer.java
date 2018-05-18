package perfix.server.json;

import java.util.Formatter;

public abstract class JSONSerializer<T> {
    protected abstract String handle(T object);

    protected Formatter formatter = new Formatter();

    public String toJSONString(T object) {
        if (object == null) {
            return "";
        } else if (object instanceof Number || object instanceof Boolean) {
            return "" + object.toString();
        } else if (object instanceof CharSequence || object instanceof Character) {
            return "\"" + object.toString() + "\"";
        } else {
            return handle(object);
        }
    }
}
