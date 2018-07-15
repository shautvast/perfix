import org.junit.Assert;
import org.junit.Test;
import perfix.instrument.StatementText;

public class StatementTextTest {

    @Test
    public void testReplace(){
        StatementText b = new StatementText("select ? from ?");
        StatementText.set(b,1, "alpha");
        StatementText.set(b,2, "beta");
        Assert.assertEquals("select alpha from beta",b.toString());
    }
}
