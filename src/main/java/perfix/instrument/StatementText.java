package perfix.instrument;

public class StatementText {

    private static final char BOUNDVAR_MARK = '?';

    private final Object[] vars;
    private final StringBuilder sql;

    public StatementText(String sql) {
        vars = new Object[countVars(sql)];
        this.sql = new StringBuilder(sql);
    }

    public void set(int index, Object value) {
        if (index < 1 || index > vars.length + 1) {
            throw new IndexOutOfBoundsException("" + index);
        }
        vars[index - 1] = value;
    }

    public void set(int index, int value){
        set(index, Integer.toString(value));
    }

    public void set(int index, long value){
        set(index, Long.toString(value));
    }

    public void set(int index, double value){
        set(index, Double.toString(value));
    }

    public void set(int index, boolean value){
        set(index, Boolean.toString(value));
    }

    public void set(int index, float value){
        set(index, Float.toString(value));
    }

    public void set(int index, short value){
        set(index, Short.toString(value));
    }

    @Override
    public String toString() {
        int found = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == BOUNDVAR_MARK) {
                sql.deleteCharAt(i);
                sql.insert(i, vars[found++]);
            }
        }
        return sql.toString();
    }

    private int countVars(String sql) {
        int count = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == BOUNDVAR_MARK) {
                count++;
            }
        }
        return count;
    }
}
