package edu.uwm.cs.junit;

import java.util.LinkedHashMap;
import java.util.Map;

import edu.uwm.cs.util.TimeoutExecutor;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

public class TestRunner implements TestListener {
	private static final String VERSION = "1.0.0";

	public enum Disposition {
		PASSED, TIMEOUT, FAILURE, ERROR;
	}
	
	private boolean verbose = false;
	private long timeoutMillis = 0;
	private TimeoutExecutor timeout = null;
	private Map<String,Disposition> results = new LinkedHashMap<>();
	
	private Test currentTest;
	private volatile Disposition currentDisposition;
	
	@Override
	public void startTest(Test test) {
		currentTest = test;
		if (timeout != null && !timeout.defer(timeoutMillis)) return;
		currentDisposition = Disposition.PASSED;
	}

	@Override
	public void addError(Test test, Throwable e) {
		if (verbose) System.out.println("Error in " + test);
		if (timeout != null && !timeout.defer(timeoutMillis)) return;
		currentDisposition = Disposition.ERROR;
	}

	@Override
	public void addFailure(Test test, AssertionFailedError e) {
		if (verbose) System.out.println("Failure in " + test);
		if (timeout != null && !timeout.defer(timeoutMillis)) return;
		currentDisposition = Disposition.FAILURE;
	}

	@Override
	public void endTest(Test test) {
		if (timeout != null && !timeout.defer(timeoutMillis)) return;
		results.put(test.toString(), currentDisposition);
	}

	private void printResults() {
		System.out.println("==========");
		for (Map.Entry<String,Disposition> e : results.entrySet()) {
			String testName = e.getKey();
			int paren = testName.indexOf('(');
			if (paren > 0) testName = testName.substring(0, paren);
			System.out.println(testName + ": " + e.getValue());
		}
	}
	
	private void timeoutTest() {
		currentDisposition = Disposition.TIMEOUT;
		results.put(currentTest.toString(), Disposition.TIMEOUT);
		printResults();
		System.exit(1);
	}

	private void doRun(Class<?> testClass) {
		TestSuite suite = new TestSuite(testClass);
		TestResult result = new TestResult();
		result.addListener(this);
		if (timeoutMillis > 0) {
			timeout = new TimeoutExecutor(() -> timeoutTest(), timeoutMillis);
		}
		suite.run(result);
		if (timeout != null) {
			if (!timeout.cancel()) return;
		}
		printResults();
	}
	
	private void start(String[] args) {
		Class<?> testClass = null;
		for (int i=0; i < args.length; ++i) {
			if (args[i].startsWith("-")) {
				switch(args[i]) {
				case "--timeout":
					if (++i >= args.length) {
						System.err.println("--timeout needs argument");
						System.exit(1);
					}
					timeoutMillis = Integer.parseInt(args[i]);
					break;
				case "--version":
					System.out.println("edu.uwm.cs.junit.TestRunner version " + VERSION);
					System.exit(0);
					break;
				case "--verbose":
					verbose = true;
					break;
				default:
					System.err.println("Unknown option: " + args[i]);
					System.exit(1);
				}
			} else {
				if (testClass != null) {
					System.err.println("Too many classes: " + args[i]);
					System.exit(1);
				}
				try {
					testClass = Class.forName(args[i]);
				} catch (ClassNotFoundException e) {
					System.err.println("Could not find class " + args[i]);
					System.exit(1);
				}
			}
		}
		if (testClass == null) {
			System.err.println("Expected a class name to run tests from.");
			System.exit(1);
		}
		System.out.println("TestRunner version " + VERSION + " with timeout = " + timeoutMillis + " ms.");
		doRun(testClass);
	}
	
	public static void main(String[] args) {
		TestRunner r= new TestRunner();
		r.start(args);
	}
}
