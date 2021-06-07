package nl.hsac.fitnesse.fixture.util;

import org.junit.Test;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.*;


public class LambdaMetaHelperTest {
    private final LambdaMetaHelper helper = new LambdaMetaHelper();

    @Test
    public void testGetNoArg() throws Throwable {
        Supplier<LambdaMetaHelperTest> f = helper.getConstructor(getClass());
        assertNotNull(f.get());
        assertTrue(f.get() instanceof LambdaMetaHelperTest);
    }

    @Test
    public void testGetIntFromString() throws Throwable {
        Function<String, Integer> f = helper.getConstructor(Integer.class, String.class);
        assertEquals(Integer.valueOf(2), f.apply("2"));
    }


}
