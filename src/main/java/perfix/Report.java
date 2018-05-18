package perfix;

public class Report {
    private final String name;
    private final int invocations;
    private final long totalDuration;
    private final double average;

    Report(String name, int invocations, long totalDuration) {
        this.name = name;
        this.invocations = invocations;
        this.totalDuration = totalDuration;
        this.average = (double) totalDuration / invocations;
    }

    public double getAverage() {
        return average;
    }

    public int getInvocations() {
        return invocations;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public String getName() {
        return name;
    }
}