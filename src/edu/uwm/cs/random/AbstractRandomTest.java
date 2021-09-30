package edu.uwm.cs.random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import edu.uwm.cs.util.TriConsumer;
import edu.uwm.cs.util.TriFunction;
import edu.uwm.cs.util.Union;
import junit.framework.TestCase;

/**
 * An abstract class for doing random testing of a class against
 * a reference implementation.  The basic idea is that this
 * class generates sequences of random operations that are executed 
 * in a reference implementation and also in the system under test (SUT).
 * If the results ever differ, then a JUnit test case is generated for 
 * the particular test that failed and printed so that someone
 * can perform debugging with this test.
 * <p>
 * This basis idea is complicated by a number of factors:
 * <ul>
 * <li> A very long series of tests is hard to debug.  It is best to try to find
 * a failure with a shorter sequence of operations first.  Thus we start
 * generating short tests and after each, reset the system and start again.
 * This assumes (sometimes incorrectly) that behavior does not "leak"
 * from former to current instances of the SUT.
 * <li> Constant values.  The SUT may interact with custom value objects.
 * In order to generate a test, we need a way to indicate which value
 * is being used.  The {@link Object#toString()} method doesn't always
 * generate executable text (e.g. {@link String#toString()} does not
 * generate a properly escaped string literal.
 * <li> Abrupt termination. If the operation is supposed to
 * throw an exception, we check that the SUT also generates an exception
 * in the same situations, ideally we check that the same class of 
 * exception is thrown.
 * <li> Multiplicity. We need to generate multiple instances of SUTs
 * to test possible interference.
 * <li> Heterogenity.  Sometimes the behavior generates objects that
 * need testing, for example, a collection can generate iterators, and 
 * these subsidiary object need testing as well. 
 * <li> Mutable parameters.  The SUT may involve passing mutable objects 
 * (ones generated earlier) as parameters, we need a way to distinguish
 * object identity and name these objects correctly in the printed test.
 * <li> Non-determinism.  If the implementation is allowed 
 * different possible behaviors (e.g. hash table iteration), 
 * then the reference implementation needs to provide a way to
 * handle any of the possibilities.  Then if the SUT provides
 * an acceptable behavior at one point, the reference implementation 
 * can discard the behaviors that didn't match and proceed.
 * <li> Equality.  Value objects should be compared with {@link TestCase#assertEquals} 
 * but mutable objects with {@link TestCase#assertSame}.
 * <li> Non-termination.  If a test execution takes a long time, we need a way 
 * to time out and generate a test case with a suspected infinite loop at the end.
 * <li> Bugs in the reference implementation.
 * @param R type of the reference class
 * @param S type of the SUT class
 */
public abstract class AbstractRandomTest<R,S> implements LiteralBuilder {

	protected final Class<R> refClass;
	protected final Class<S> sutClass;
	
	class RegisteredClass {
		Class<?> clazz;
		String prefix;
		String typeName;
		List<Object> objects; // reference objects
		RegisteredClass(Class <?> c, String p, String t) {
			clazz = c;
			prefix = p;
			typeName = t;
			objects = new ArrayList<>();
		}
	}
	
	private List<RegisteredClass> registeredClasses = new ArrayList<>();
	private Map<Object,String> registry = new HashMap<>();
	private Map<String,Object> testObjects = new HashMap<>();
		
	@Override
	public boolean isMutableObject(Object x) {
		if (x instanceof Union<?,?>) return true;
		String name = registry.get(x);
		if (name != null) return true;
		for (RegisteredClass rc : registeredClasses) {
			if (rc.clazz.isInstance(x)) return true;
		}
		return false;
	}

	/** Return a string for the object.  
	 * This implementation handles null, a Number, String or Character.
	 * Otherwise if the object is in the registry, return registered name.
	 * If not, but is of a class for which the registry is prepared,
	 * return null.  Otherwise, return the result of {@link Object#toString()}.
	 * @see edu.uwm.cs.random.LiteralBuilder#toString(java.lang.Object)
	 */
	@Override
	public String toString(Object x) {
		if (x instanceof Union<?,?>) {
			x = ((Union<?,?>)x).get();
		}
		if (x == null || x instanceof String || x instanceof Character || x instanceof Number) return edu.uwm.cs.junit.Util.toString(x);
		String name = registry.get(x);
		if (name != null) return name;
		for (RegisteredClass rc : registeredClasses) {
			if (rc.clazz.isInstance(x)) return null;
		}
		return x.toString();
	}

	/**
	 * Return a string naming the type of this object.
	 * The implementation returns the canonical type name of the class of the object.
	 * Generic types lead to raw types.
	 * @see edu.uwm.cs.random.LiteralBuilder#toTypeString(java.lang.Object)
	 */
	@Override
	public String toTypeString(Object x) {
		if (x instanceof Union<?,?>) {
			x = ((Union<?,?>)x).get();
		}
		for (RegisteredClass rc : registeredClasses) {
			if (rc.clazz.isInstance(x)) return rc.typeName;
		}
		return x.getClass().getCanonicalName().replace('/', '.');
	}

	@Override
	public String registerMutableObject(Object ref, Object test) {
		if (ref instanceof Union<?,?>) {
			ref = ((Union<?,?>)ref).get();
		}
		if (test instanceof Union<?,?>) {
			test = ((Union<?,?>)test).get();
		}
		if (ref == null || test == null) throw new NullPointerException("cannot register null");
		for (RegisteredClass rc : registeredClasses) {
			if (rc.clazz.isInstance(ref)) {
				String prefix = rc.prefix;
				String result = prefix + registry.size();
				registry.put(ref, result);
				rc.objects.add(ref);
				testObjects.put(result, test);
				return result;
			}
		}
		throw new IllegalArgumentException("No registered class for " + ref);
	}
	
	/**
	 * Register a class of mutable objects.
	 * @param clazz class of mutable objects from the reference implementation.
	 * @param prefix short  string used to name mutable objects, if null, use first letter of class name.
	 * @param typeName string used to type the SUT implementation, if null, use reference class canonical name.
	 */
	protected void registerMutableClass(Class<?> clazz, String typeName, String prefix) {
		if (typeName == null) typeName = clazz.getCanonicalName().replace('/', '.');
		if (prefix == null) prefix = ""+Character.toLowerCase(clazz.getSimpleName().charAt(0));
		registeredClasses.add(new RegisteredClass(clazz, prefix, typeName));
	}

	@Override
	public Object getTestObject(String registeredName) {
		return testObjects.get(registeredName);
	}

	public static final int DEFAULT_TIMEOUT = 1000; // milliseconds

	protected final int maxTests;
	protected final int maxTestSize;
	protected final int timeout = DEFAULT_TIMEOUT;
	
	/**
	 * Create a random testing situation.
	 * @param rClass class of the reference type
	 * @param sClass class of the SUT type
	 * @param typename name of the SUT class to use for SUT variables
	 * @param prefix name to use for local variables of the SUT in the test
	 * @param total total number of commands to generate (unless
	 * a failure is found earlier).
	 * @param testSize maximum number of commands in a single test
	 */
	protected AbstractRandomTest(Class<R> rClass, Class<S> sClass, String typename, String prefix, int total, int testSize) {
		refClass = rClass;
		sutClass = sClass;
		maxTests = total;
		maxTestSize = testSize;
		registerMutableClass(rClass, typename, prefix);
	}
	
	private List<String> tests = new ArrayList<>();
	private Random random = new Random();
	
	/**
	 * Return a previously created (reference) mutable object
	 * @param clazz registered class of object
	 * @return random previously created object of this class, or null
	 * if none have been created yet.
	 */
	protected <T> T getReferenceObject(Class<T> clazz) {
		for (RegisteredClass rc : registeredClasses) {
			if (rc.clazz.equals(clazz)) {
				int n = rc.objects.size();
				if (n == 0) return null;
				return clazz.cast(rc.objects.get(random.nextInt(n)));
			}
		}
		throw new IllegalArgumentException("Class not registered: " + clazz);
	}
	
	@SuppressWarnings("unchecked")
	protected <T> Class<java.util.Iterator<T>> iteratorClass() {
		return (Class<java.util.Iterator<T>>)(Class<?>)Iterator.class;
	}
	
	protected <A,T> Function<A,Result<T>> lift(Function<A,T> func) {
		return (a) -> {
			try {
				return new NormalResult<T>(func.apply(a));
			} catch (Exception|Error e) {
				return new ExceptionResult<T>(e);
			}
		};
	}
	protected <A,B,T> BiFunction<A,B,Result<T>> lift(BiFunction<A,B,T> func) {
		return (a,b) -> {
			try {
				return new NormalResult<T>(func.apply(a,b));
			} catch (Exception|Error e) {
				return new ExceptionResult<T>(e);
			}
		};
	}
	protected <A,B,C,T> TriFunction<A,B,C,Result<T>> lift(TriFunction<A,B,C,T> func) {
		return (a,b,c) -> {
			try {
				return new NormalResult<T>(func.apply(a,b,c));
			} catch (Exception|Error e) {
				return new ExceptionResult<T>(e);
			}
		};
	}

	protected <A> Function<A,Result<Void>> lift(Consumer<A> func) {
		return (a) -> {
			try {
				func.accept(a);
				return NormalResult.voidResult();
			} catch (Exception|Error e) {
				return new ExceptionResult<Void>(e);
			}
		};
	}
	protected <A,B> BiFunction<A,B,Result<Void>> lift(BiConsumer<A,B> func) {
		return (a,b) -> {
			try {
				func.accept(a, b);
				return NormalResult.voidResult();
			} catch (Exception|Error e) {
				return new ExceptionResult<Void>(e);
			}
		};
	}
	protected <A,B,C> TriFunction<A,B,C,Result<Void>> lift(TriConsumer<A,B,C> func) {
		return (a,b,c) -> {
			try {
				func.accept(a, b, c);
				return NormalResult.voidResult();
			} catch (Exception|Error e) {
				return new ExceptionResult<Void>(e);
			}
		};
	}

	protected Command<Union<R,S>> newCommand() {
		String sutName = null;
		for (RegisteredClass rc : registeredClasses) {
			if (rc.clazz.equals(refClass)) sutName = rc.typeName;
		}
		assert sutName != null;
		return new Command.NewInstance<>(this, refClass, sutClass, sutName);
	}
	
	protected static <T> Result<T> newInstance(Class<T> clazz) {
		try {
			return new NormalResult<>(clazz.newInstance());
		} catch (Exception|Error ex) {
			System.out.println("Cannot instantiate " + clazz);
			ex.printStackTrace();
			return new ExceptionResult<>(ex);
		}
	}
	
	protected <T> Command<T> newCommand(Class<T> clazz) {
		String sutName = null;
		for (RegisteredClass rc : registeredClasses) {
			if (rc.clazz.equals(refClass)) sutName = rc.typeName;
		}
		assert sutName != null;
		return new Command.Default<>(
				() -> newInstance(clazz), 
				() -> newInstance(clazz), 
				"new " + sutName + "()");
	}
	
	protected <A,T> Function<A, Command<T>> new_(Function<A,Result<T>> cons, String typeName) {
		return new Command.NewInstance1<>(this, refClass, sutClass, cons, typeName);
	}
	protected <A,B,T> BiFunction<A, B, Command<T>> new__(BiFunction<A,B,Result<T>> cons, String typeName) {
		return new Command.NewInstance2<>(this, refClass, sutClass, cons, typeName);
	}
	
	protected <A,T> Function<A, Command<T>> build(Class<A> clazz, Function<A,Result<T>> func, String mname) {
		return new Command.Builder0<>(this, clazz, clazz, func, func, mname);
	}
	protected <T> Function<R, Command<T>> build(Function<R,Result<T>> rfunc, Function<S,Result<T>> sfunc, String mname) {
		return new Command.Builder0<>(this, refClass, sutClass, rfunc, sfunc, mname);
	}
	protected <T,U> BiFunction<R, U, Command<T>> build_(BiFunction<R,U,Result<T>> rfunc, BiFunction<S,U,Result<T>> sfunc, String mname) {
		return new Command.Builder1<>(this, refClass, sutClass, rfunc, sfunc, mname);		
	}
	protected <T> BiFunction<R, R, Command<T>> buildR(BiFunction<R,R,Result<T>> rfunc, BiFunction<S,S,Result<T>> sfunc, String mname) {
		return new Command.BuilderR<>(this, refClass, sutClass, rfunc, sfunc, mname);		
	}
	protected <T,U,V> TriFunction<R, U, V, Command<T>> build__(TriFunction<R,U,V,Result<T>> rfunc, TriFunction<S,U,V,Result<T>> sfunc, String mname) {
		return new Command.Builder2<>(this, refClass, sutClass, rfunc, sfunc, mname);		
	}
	protected <T,U> TriFunction<R, U, R, Command<T>> build_R(TriFunction<R,U,R,Result<T>> rfunc, TriFunction<S,U,S,Result<T>> sfunc, String mname) {
		return new Command.Builder1R<>(this, refClass, sutClass, rfunc, sfunc, mname);		
	}
	protected <T,V> TriFunction<R, R, V, Command<T>> buildR_(TriFunction<R,R,V,Result<T>> rfunc, TriFunction<S,S,V,Result<T>> sfunc, String mname) {
		return new Command.BuilderR1<>(this, refClass, sutClass, rfunc, sfunc, mname);		
	}
	protected <T> TriFunction<R, R, R, Command<T>> buildRR(TriFunction<R,R,R,Result<T>> rfunc, TriFunction<S,S,S,Result<T>> sfunc, String mname) {
		return new Command.BuilderRR<>(this, refClass, sutClass, rfunc, sfunc, mname);		
	}
	
	
	static enum TestState {
		REFERENCE, SUT, FRAMEWORK;
	}
	private TestState currentState;
	private Command<?> currentCommand;
	private TimeoutExecutor timer = new TimeoutExecutor(() -> doTimeout(), timeout);

	/**
	 * Compute a random command
	 * @param r random number source
	 * @return command that can be executed at the current point.
	 */
	protected abstract Command<?> randomCommand(Random r);
	
	/**
	 * Start a new test with all new objects.
	 */
	public void clear() {
		for (RegisteredClass rc : registeredClasses) {
			rc.objects.clear();
		}
		registry.clear();
		testObjects.clear();
		tests.clear();
	}
	
	protected <T> boolean test(Command<T> command) {
		if (!timer.defer(timeout)) return false;
		currentState = TestState.REFERENCE;
		currentCommand = command;
		Result<T> expected = command.execute(true);
		if (!timer.defer(timeout)) return false;
		currentState = TestState.SUT;
		Result<T> actual = command.execute(false);
		if (!timer.defer(timeout)) return false;
		currentState = TestState.FRAMEWORK;
		boolean result = expected.includes(actual, this);
		String extra = "";
		if (result == false) {
			extra = " // got " + actual.toString();
		}
		tests.add(expected.genAssert(this, command.code(this)) + extra);
		return result;
	}
	
	protected void test(int testSize) {
		int count = 0;
		clear();
		System.out.println("// Testing sequences of " + testSize + " commands.");
		while (count < maxTests) {
			currentState = TestState.FRAMEWORK;
			if (!timer.defer(timeout)) return;
			if ((++count % 100000) == 0) {
				System.out.println("// " + count + " tests passed");
			}
			if (tests.size() > testSize) {
				clear();
			}
			Command<?> command = randomCommand(random);
			if (!test(command)) return;
		}
		clear();
	}
	
	private void doTimeout() {
		switch (currentState) {
		case FRAMEWORK:
			throw new AssertionError("framework has timeout?");
		case REFERENCE:
			System.out.println("//! Timeout in reference implementation.  Please submit this test to instructor.");
			// fall through
		case SUT:
			tests.add(currentCommand.code(this) + "; // timeout");
			print();
			System.exit(0);
			break;
		default:
			break;
		}
	}
	
	protected void printImports() {
		System.out.println("import junit.framework.TestCase;\n");		
	}
	
	public void print() {
		if (tests.isEmpty()) {
			System.out.println("import junit.framework.TestCase;\n");
			System.out.println("public class TestGen extends TestCase {");
			System.out.println("  public void test() {");
			System.out.println("    assertTrue(\"Everything is fine\",true);");
			System.out.println("  }");
			System.out.println("}");
			System.out.println("\n// Congratulations: no bugs found!");
			return;	
		}
		printImports();
		System.out.println("public class TestGen extends TestCase {");
		System.out.println("  protected void assertException(Class<?> exc, Runnable r) {");
		System.out.println("     try {");
		System.out.println("       r.run();");
		System.out.println("       assertFalse(\"should have thrown an exception.\",true);");
		System.out.println("     } catch (RuntimeException e) {");
		System.out.println("       if (exc == null) return;");
		System.out.println("       assertTrue(\"threw wrong exception type.\",exc.isInstance(e));");
		System.out.println("     }");
		System.out.println("  }\n");
		System.out.println("  public void test() {");
		for (String test : tests) {
			System.out.println("    " + test);
		}
		System.out.println("  }");
		System.out.println("}");
	}
	
	public void run() {
		try {
			int testSize = 10;
			while (testSize < maxTestSize) {
				test(testSize);
				if (timer.executed()) return;
				if (!tests.isEmpty()) break;
				testSize *= 2;
			}
		} finally {
			timer.cancel();
		}
		if (!timer.executed()) print();
	}
}
