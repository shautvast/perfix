package perfix;


import java.util.Objects;

public class MethodNode {
    private final String name;
    private final long timestamp;
    private final String threadName;
    private MethodNode parent;
    private long duration;
    private long invocationid;


    public MethodNode(String name) {
        this.name = name;
        this.timestamp = System.nanoTime();
        this.threadName = Thread.currentThread().getName();
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return Objects.hash(Thread.currentThread().getId(), timestamp);
    }

    @Override
    public String toString() {
        return "MethodNode{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodNode that = (MethodNode) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public long getParentId() {
        if (parent == null) {
            return 0;
        } else {
            return parent.getId();
        }
    }

    public long getDuration() {
        return duration;
    }

    public void registerEndingTime(long t1) {
        duration = t1 - timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public MethodNode getParent() {
        return parent;
    }

    public void setParent(MethodNode parent) {
        this.parent = parent;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setInvocationId(long invocationid) {
        this.invocationid = invocationid;
    }

    public long getInvocationId() {
        if (parent != null) {
            return parent.getInvocationId();
        } else {
            return getId();
        }
    }
}
