package perfix.instrument;

public class StatementText {

    private static final char BOUNDVAR_MARK = '?';

    private final Object[] vars;
    private final StringBuilder sql;

    public StatementText(String sql) {
        vars = new Object[countVars(sql)];
        this.sql = new StringBuilder(sql);
    }


    public static void set(StatementText statementText, int index, Object value) {
        if (statementText != null) {
            if (index < 1 || index > statementText.vars.length + 1) {
                throw new IndexOutOfBoundsException("" + index);
            }
            statementText.vars[index - 1] = value;
        }
    }

    @SuppressWarnings("unused") //used in generated code
    public static String toString(StatementText statementText) {
        return statementText != null ? statementText.toString() : "[none]";
    }

    public static void set(StatementText statementText, int index, int value) {
        set(statementText, index, Integer.toString(value));
    }

    public static void set(StatementText statementText, int index, long value) {
        set(statementText, index, Long.toString(value));
    }

    public static void set(StatementText statementText, int index, double value) {
        set(statementText, index, Double.toString(value));
    }

    public static void set(StatementText statementText, int index, boolean value) {
        set(statementText, index, Boolean.toString(value));
    }

    public static void set(StatementText statementText, int index, float value) {
        set(statementText, index, Float.toString(value));
    }

    public static void set(StatementText statementText, int index, short value) {
        set(statementText, index, Short.toString(value));
    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder(sql);
        int found = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (output.charAt(i) == BOUNDVAR_MARK) {
                output.deleteCharAt(i);
                output.insert(i, vars[found++]);
            }
        }
        return output.toString();
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
