package perfix;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MethodNode {
    public final String name;
    public final List<MethodNode> children;
    public MethodNode parent;
    public Report report;

    public MethodNode(String name) {
        this.name = name;
        this.children = new ArrayList<>();
    }

    public void addChild(MethodNode child){
        children.add(child);
    }

    public String getName() {
        return name;
    }

    public Report getReport() {
        return report;
    }

    public List<MethodNode> getChildren() {
        return children;
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


}
