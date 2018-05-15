import org.junit.Test;
import perfix.MethodInvocation;
import perfix.Registry;

public class TestMethod {
    @Test
    public void testAddMethodToRegistry() {
        MethodInvocation method = MethodInvocation.start("somename");
        method.stop();
        MethodInvocation method2 = MethodInvocation.start("somename");
        method2.stop();

        Registry.report(System.out);
    }

}
