import org.junit.Assert;
import org.junit.Test;
import perfix.instrument.StatementText;

public class StatementTextTest {

    @Test
    public void testReplace(){
        StatementText b = new StatementText("select ? from ?");
        b.set(1, "alpha");
        b.set(2, "beta");
        Assert.assertEquals("select alpha from beta",b.toString());
    }
}
