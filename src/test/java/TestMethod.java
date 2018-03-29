import org.junit.Test;
import perfix.Method;
import perfix.Registry;

public class TestMethod {
    @Test
    public void testAddMethodToRegistry() {
        Method method = Method.start("somename");
        method.stop();
        Method method2 = Method.start("somename");
        method2.stop();

        Registry.report();
    }

}
