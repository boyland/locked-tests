package edu.uwm.cs.junit;

import junit.framework.TestCase;

public class EfficiencyTestCase extends TestCase {

	private long startMillis = 0;
	
	@Override
	protected void setUp() {
		try {
			assert 1/(startMillis/Integer.MAX_VALUE) < 0;
		} catch (ArithmeticException ex) {
			System.err.println("You must disable assertions to run this test.");
			assertFalse("Assertions must NOT be enabled while running efficiency tests.",true);
		}
		startMillis = System.currentTimeMillis();
	}
	
	@Override
	protected void tearDown() {
		if (startMillis > 0 && System.getProperty("edu.uwm.cs.showTime") != null) {
			long millis = System.currentTimeMillis() - startMillis;
			String name = this.getName();
			System.out.println(name + " (" + String.format("%5.3f", millis/1000.0) + " s)");
		}
		startMillis = 0;
	}
}
