package perfix;

public class MutableBoolean {
    private boolean value;

    MutableBoolean(boolean value) {
        this.value = value;
    }

    void set(boolean value) {
        this.value = value;
    }

    boolean get() {
        return value;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
