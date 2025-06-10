package edu.uwm.cs.random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import edu.uwm.cs.util.Consumer4;
import edu.uwm.cs.util.Function4;
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
	
	private Map<Object,String> registry = new IdentityHashMap<>();
	private Map<String,Object> testObjects = new HashMap<>();
	private Map<Object,Integer> registeredIndex = new IdentityHashMap<>();

	public class RegisteredClass<T,U> implements TestClass<T,U> {
		Class<T> refClass;
		Class<U> sutClass;
		String prefix;
		String typeName;
		List<T> refs; // reference objects
		List<U> tests;
		
		RegisteredClass(Class <T> rc, Class<U> sc, String p, String t) {
			refClass = rc;
			sutClass = sc;
			prefix = p;
			typeName = t;
			refs = new ArrayList<>();
			tests = new ArrayList<>();
		}
		
		@Override
		public Class<T> getRefClass() {
			return refClass;
		}

		@Override
		public Class<U> getSUTClass() {
			return sutClass;
		}

		@Override
		public String getTypeName() {
			return typeName;
		}

		@Override
		public int indexOf(T ref) {
			return registeredIndex.getOrDefault(ref, -1);
		}

		@Override
		public String getIdentifier(int index) {
			if (index < 0) return "null";
			if (index >= refs.size()) throw new IllegalStateException("no ref for this index: " + index);
			return prefix + index;
		}
		
		@Override
		public T getRefObject(int i) {
			if (i < 0) return null;
			return refs.get(i);
		}

		@Override
		public U getSUTObject(int i) {
			if (i < 0) return null;
			return tests.get(i);
		}

		@Override
		public void clear() {
			refs.clear();
			tests.clear();
		}
		
		@Override
		public int size() {
			return refs.size();
		}
		
		@Override
		public void register(T ref, U test) {
			int i=size();
			// String name = prefix + i;
			refs.add(ref);
			tests.add(test);
			//registry.put(ref, name);
			//testObjects.put(name, test);
			registeredIndex.put(ref, i);
		}
		
		public void register(Union<T,U> u1, Union<T,U> u2) {
			register(u1.getR(),u2.getS());
		}
	}
	
	protected final RegisteredClass<R,S> mainClass;
	
	private List<RegisteredClass<?,?>> registeredClasses = new ArrayList<>();
		
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
		if (x instanceof String) {
			String lit = edu.uwm.cs.junit.Util.toString(x);
			return "new String(" + lit + ")";
		}
		if (x == null || x instanceof String || x instanceof Character || x instanceof Number) return edu.uwm.cs.junit.Util.toString(x);
		String name = registry.get(x);
		if (name != null) return name;
		Integer index = registeredIndex.get(x);
		if (index != null) {
			for (RegisteredClass<?,?> rc : registeredClasses) {
				if (rc.getRefClass().isInstance(x)) {
					name = rc.prefix + index;
					registry.put(x, name);
					return name;
				}
			}
		}
		return x.toString();
	}

	/**
	 * Register a class of mutable objects.
	 * @param refClass class of mutable objects from the reference implementation.
	 * @param sutClass class of mutable objects from the SUT
	 * @param typeName string used to type the SUT implementation, if null, use reference class canonical name.
	 * @param prefix short  string used to name mutable objects, if null, use first letter of class name.
	 */
	protected <T,U> RegisteredClass<T,U> registerMutableClass(Class<T> refClass, Class<U> sutClass, String typeName, String prefix) {
		if (typeName == null) typeName = refClass.getCanonicalName().replace('/', '.');
		if (prefix == null) prefix = ""+Character.toLowerCase(refClass.getSimpleName().charAt(0));
		AbstractRandomTest<R, S>.RegisteredClass<T, U> result = new RegisteredClass<T,U>(refClass, sutClass, prefix, typeName);
		registeredClasses.add(result);
		return result;
	}

	public static final int DEFAULT_TIMEOUT = 1000; // milliseconds

	protected final int maxTests;
	protected final int maxTestSize;
	protected final int timeout = DEFAULT_TIMEOUT;
	private Random random = new Random();
	
	/**
	 * Create a random testing situation, where assertions are required to be enabled for the SUT.
	 * @param rClass class of the reference type
	 * @param sClass class of the SUT type
	 * @param typename name of the SUT class to use for SUT variables
	 * @param prefix name to use for local variables of the SUT in the test
	 * @param total total number of commands to generate (unless
	 * a failure is found earlier).
	 * @param testSize maximum number of commands in a single test
	 */
	protected AbstractRandomTest(Class<R> rClass, Class<S> sClass, String typename, String prefix, int total, int testSize) {
		this(rClass, sClass, typename, prefix, total, testSize, true);
	}

	/**
	 * Create a random testing situation.
	 * @param rClass class of the reference type
	 * @param sClass class of the SUT type
	 * @param typename name of the SUT class to use for SUT variables
	 * @param prefix name to use for local variables of the SUT in the test
	 * @param total total number of commands to generate (unless
	 * a failure is found earlier).
	 * @param testSize maximum number of commands in a single test
	 * @param assertsRequired whether to require assertions to be enabled.
	 */
	protected AbstractRandomTest(Class<R> rClass, Class<S> sClass, String typename, String prefix, int total, int testSize, boolean assertsRequired) {
		refClass = rClass;
		sutClass = sClass;
		maxTests = total;
		maxTestSize = testSize;
		mainClass = registerMutableClass(rClass, sClass, typename, prefix);
		if (assertsRequired && !sClass.desiredAssertionStatus()) {
			System.err.println("Turn on assertions to run random testing.");
			System.err.println("e.g., in Eclipse, add -ea in the VM Arguments box of the Arguments tab of the");
			System.err.println("Run Configuration for this application.");
			System.exit(1);
		}
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
	protected <A,B,C,D,T> Function4<A,B,C,D,Result<T>> lift(Function4<A,B,C,D,T> func) {
		return (a,b,c,d) -> {
			try {
				return new NormalResult<T>(func.apply(a,b,c,d));
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
	protected <A,B,C,D> Function4<A,B,C,D,Result<Void>> lift(Consumer4<A,B,C,D> func) {
		return (a,b,c,d) -> {
			try {
				func.accept(a, b, c, d);
				return NormalResult.voidResult();
			} catch (Exception|Error e) {
				return new ExceptionResult<Void>(e);
			}
		};
	}
	
	protected <A,T> Function<A,Result<Union<T,T>>> lift(TestClass<T,T> desc, Function<A,T> func) {
		return (a) -> {
			try {
				return ObjectResult.create(desc, func.apply(a));
			} catch (Exception|Error e) {
				return new ExceptionResult<>(e);
			}
		};
	}
	protected <A,B,T> BiFunction<A,B,Result<Union<T,T>>> lift(TestClass<T,T> desc, BiFunction<A,B,T> func) {
		return (a,b) -> {
			try {
				return ObjectResult.create(desc, func.apply(a,b));
			} catch (Exception|Error e) {
				return new ExceptionResult<>(e);
			}
		};
	}
	protected <A,B,C,T> TriFunction<A,B,C,Result<Union<T,T>>> lift(TestClass<T,T> desc, TriFunction<A,B,C,T> func) {
		return (a,b,c) -> {
			try {
				return ObjectResult.create(desc, func.apply(a,b,c));
			} catch (Exception|Error e) {
				return new ExceptionResult<>(e);
			}
		};
	}

	protected Command<Union<R,S>> newCommand() {
		return newCommand(mainClass);
	}
	
	protected <U,V> Command<Union<U,V>> newCommand(TestClass<U,V> desc) {
		return new Command.NewCommand<>(desc);
	}
	
	/**
	 * Construct a command that creates an object on which we
	 * can do further tests.  The result type must be registered.
	 * @param desc description of the registered type
	 * @param supplier constructor with no parameters
	 * @return command to create an instance that will be registered for further method calls.
	 */
	protected <T> Command<?> create(TestClass<T,T> desc, Supplier<T> supplier) {
		return new Command.NewCommand<>(desc,supplier,supplier);
	}	
	
	/**
	 * Construct a command that creates an object on which we
	 * can do further tests.  The result type must be registered.
	 * @param desc description of the registered type
	 * @param supplier constructor with no parameters
	 * @return command to create an instance that will be registered for further method calls.
	 */
	protected <T,U> Command<?> create(TestClass<T,U> desc, Supplier<T> supp1, Supplier<U> supp2) {
		return new Command.NewCommand<>(desc, supp1, supp2);
	}
	
	/**
	 * Construct a command that creates an object on which we
	 * can do further tests.  The result type must be registered.
	 * @param desc description of the registered type
	 * @param func constructor with one pure parameter
	 * @return function to create a command to create an instance that will be registered
	 */
	protected <T,A> Function<A,Command<?>> create(TestClass<T,T> desc, Function<A,T> func) {
		return (a) -> new Command.NewCommand1<>(desc, a, func, func);
	}
		
	/**
	 * Construct a command that creates an object on which we
	 * can do further tests.  The result type must be registered.
	 * @param desc description of the registered type
	 * @param func1 constructor for reference type with one pure parameter
	 * @param func2 constructor with SUT type with one pure parameter
	 * @return function to create a command to create an instance that will be registered
	 */
	protected <T,U,A> Function<A,Command<?>> create(TestClass<T,U> desc, Function<A,T> func1, Function<A,U> func2) {
		return (a) -> new Command.NewCommand1<>(desc, a, func1, func2);
	}
		
	/**
	 * Construct a command that creates an object on which we
	 * can do further tests.  The result type must be registered.
	 * @param desc description of the registered type
	 * @param func constructor with two pure parameters
	 * @return command to create an instance that will be registered
	 */
	protected <T,A,B> BiFunction<A,B,Command<?>> create(TestClass<T,T> desc, BiFunction<A,B,T> func) {
		return (a,b) -> new Command.NewCommand2<>(desc, a, b, func, func);
	}
	
	/**
	 * Construct a command that creates an object on which we
	 * can do further tests.  The result type must be registered.
	 * @param desc description of the registered type
	 * @param func1 constructor for reference type with one pure parameter
	 * @param func2 constructor with SUT type with one pure parameter
	 * @return function to create a command to create an instance that will be registered
	 */
	protected <T,U,A,B> BiFunction<A,B,Command<?>> create(TestClass<T,U> desc, BiFunction<A,B,T> func1, BiFunction<A,B,U> func2) {
		return (a,b) -> new Command.NewCommand2<>(desc, a, b, func1, func2);
	}
		
	/**
	 * Construct a command that calls a method that returns the same type (e.g. clone)
	 * @param desc description of the registered type
	 * @param func1 first clone method
	 * @param func2 second clone method
	 * @return command to create an instance using clone.
	 */
	protected <U,V> Function<Integer,Command<?>> clone(TestClass<U,V> desc, Function<U,U> func1, Function<V,V> func2, String mname) {
		return build(desc, desc, func1, func2, mname);
	}
	
	/**
	 * Construct a command that calls a method on the main classes that returns
	 * the same type (e.g "clone").
	 * @param func1 unlifted method
	 * @param func2 unlifted method
	 * @param mname method name, e.g. "clone"
	 * @return
	 */
	protected Function<Integer,Command<?>> clone(Function<R,R> func1, Function<S,S> func2, String mname) {
		return clone(mainClass,func1,func2,mname);
	}
	
	/**
	 * Construct a command builder for a method that is run on the main class.  If the result is
	 * an object result, the lifting needs to take the test class description.
	 * @param rfunc lifted method in reference implementation
	 * @param sfunc lifted method in SUT implementation
	 * @param mname name of method (when generating tests)
	 * @return function taking an index of the object to run the method on and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <T> Function<Integer,Command<?>> build(Function<R,Result<T>> rfunc, Function <S,Result<T>> sfunc, String mname) {
		return build(mainClass,rfunc,sfunc,mname);
	}

	/**
	 * Construct a command builder for a method that is run on some object class.  If the result is
	 * an object result, the lifting needs to take the test class description.
	 * @param desc description of the type of the object
	 * @param rfunc lifted method in reference implementation
	 * @param sfunc lifted method in SUT implementation
	 * @param mname name of method (when generating tests)
	 * @return function taking an index of the object to run the method on and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <T,U,V> Function<Integer,Command<?>> build(TestClass<U,V> desc, Function<U,Result<T>> rfunc, Function <V,Result<T>> sfunc, String mname) {
		return (i) -> new Command.Command0<>(desc,i,rfunc,sfunc,mname);
	}

	/**
	 * Construct a command builder for a method that is run on some object class.  If the result is
	 * an object result, the lifting needs to take the test class description.
	 * @param desc description of the type of the object (same for ref and SUT)
	 * @param rfunc lifted method
	 * @param mname name of method (when generating tests)
	 * @return function taking an index of the object to run the method on and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <T,U> Function<Integer,Command<?>> build(TestClass<U,U> desc, Function<U,Result<T>> rfunc, String mname) {
		return build(desc,rfunc,rfunc,mname);
	}
	
	/**
	 * Construct a command that calls a method on objects classes that returns
	 * of different object types
	 * @param inDesc input object types
	 * @param outDesc output object types
	 * @param func1 unlifted method
	 * @param func2 unlifted method
	 * @param mname method name, e.g. "iterator"
	 * @return command to call the respective methods
	 */
	protected <U,V,W,X> Function<Integer,Command<?>> build(TestClass<U,V> inDesc, TestClass<W,X> outDesc, Function<U,W> func1, Function<V,X> func2, String mname) {
		Function<U,Result<Union<W,X>>> lift1 = (u) -> {
			try {
				return new ObjectResult<>(outDesc,func1.apply(u));
			} catch (Error|Exception ex) {
				return new ExceptionResult<>(ex);
			}
		};
		Function<V,Result<Union<W,X>>> lift2 = (v) -> {
			try {
				return new ObjectResult<>(func2.apply(v), outDesc);
			} catch (Error|Exception ex) {
				return new ExceptionResult<>(ex);
			}
		};
		return (i) -> new Command.Command0<U,V,Union<W,X>>(inDesc,i,lift1,lift2,mname);
	}

	
	/**
	 * Construct a command builder for a method on the main class
	 * that takes a pure parameter.
	 * @param rfunc lifted reference implementation
	 * @param sfunc lifted SUT implementation
	 * @param mname method name (when generating tests)
	 * @return function taking an index of the object to run the method on,
	 * and the argument object, and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <A,T> BiFunction<Integer,A,Command<?>> build(BiFunction<R,A,Result<T>> rfunc, BiFunction<S,A,Result<T>> sfunc, String mname) {
		return build(mainClass, rfunc, sfunc, mname);
	}
	
	/**
	 * Construct a command builder for a method on some object
	 * that takes a pure parameter.
	 * @param desc description of object running method
	 * @param rfunc lifted reference implementation
	 * @param sfunc lifted SUT implementation
	 * @param mname method name (when generating tests)
	 * @return function taking an index of the object to run the method on,
	 * and the argument object, and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <A,T,U,V> BiFunction<Integer,A,Command<?>> build(TestClass<U,V> desc, BiFunction<U,A,Result<T>> rfunc, BiFunction<V,A,Result<T>> sfunc, String mname) {
		return (i,a) -> new Command.Command1<>(desc, i, a, rfunc, sfunc, mname);
	}

	/**
	 * Construct a command builder for a method on some object
	 * that takes a pure parameter.
	 * @param desc description of object running method
	 * @param rfunc lifted implementation
	 * @param mname method name (when generating tests)
	 * @return function taking an index of the object to run the method on and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <A,T,U> BiFunction<Integer,A,Command<?>> build(TestClass<U,U> desc, BiFunction<U,A,Result<T>> rfunc, String mname) {
		return build(desc, rfunc, rfunc, mname);
	}

	/**
	 * Construct a command builder for a method on the main class
	 * that takes two pure parameters.
	 * @param rfunc lifted reference implementation
	 * @param sfunc lifted SUT implementation
	 * @param mname method name (when generating tests)
	 * @return function taking an index of the object to run the method on,
	 * and the two argument objects, and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <A,B,T,U,V> TriFunction<Integer,A,B,Command<?>> build(TriFunction<R,A,B,Result<T>> rfunc, TriFunction<S,A,B,Result<T>> sfunc, String mname) {
		return build(mainClass, rfunc, sfunc, mname);
	}

	/**
	 * Construct a command builder for a method on some object
	 * that takes two pure parameters.
	 * @param desc description of object running method
	 * @param rfunc lifted reference implementation
	 * @param sfunc lifted SUT implementation
	 * @param mname method name (when generating tests)
	 * @return function taking an index of the object to run the method on,
	 * and the two argument objects, and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <A,B,T,U,V> TriFunction<Integer,A,B,Command<?>> build(TestClass<U,V> desc, TriFunction<U,A,B,Result<T>> rfunc, TriFunction<V,A,B,Result<T>> sfunc, String mname) {
		return (i,a,b) -> new Command.Command2<>(desc, i, a, b, rfunc, sfunc, mname);
	}

	/**
	 * Construct a command builder for a method on some object
	 * that takes two pure parameters.
	 * @param desc description of object running method
	 * @param func lifted implementation
	 * @param mname method name (when generating tests)
	 * @return function taking an index of the object to run the method on,
	 * and the two argument objects, and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <A,B,T,U> TriFunction<Integer,A,B,Command<?>> build(TestClass<U,U> desc, TriFunction<U,A,B,Result<T>> func, String mname) {
		return build(desc, func, func, mname);
	}

	/**
	 * Construct a command builder for a method on the main class
	 * that takes three pure parameters.
	 * @param rfunc lifted reference implementation
	 * @param sfunc lifted SUT implementation
	 * @param mname method name (when generating tests)
	 * @return function taking an index of the object to run the method on,
	 * and the two argument objects, and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <A,B,C,T,U,V> Function4<Integer,A,B,C,Command<?>> build(Function4<R,A,B,C,Result<T>> rfunc, Function4<S,A,B,C,Result<T>> sfunc, String mname) {
		return build(mainClass, rfunc, sfunc, mname);
	}

	/**
	 * Construct a command builder for a method on some object
	 * that takes three pure parameters.
	 * @param desc description of object running method
	 * @param rfunc lifted reference implementation
	 * @param sfunc lifted SUT implementation
	 * @param mname method name (when generating tests)
	 * @return function taking an index of the object to run the method on,
	 * and the two argument objects, and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <A,B,C,T,U,V> Function4<Integer,A,B,C,Command<?>> build(TestClass<U,V> desc, Function4<U,A,B,C,Result<T>> rfunc, Function4<V,A,B,C,Result<T>> sfunc, String mname) {
		return (i,a,b,c) -> new Command.Command3<>(desc, i, a, b, c, rfunc, sfunc, mname);
	}

	/**
	 * Construct a command builder for a method on some object
	 * that takes three pure parameters.
	 * @param desc description of object running method
	 * @param func lifted implementation
	 * @param mname method name (when generating tests)
	 * @return function taking an index of the object to run the method on,
	 * and the two argument objects, and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <A,B,C,T,U> Function4<Integer,A,B,C,Command<?>> build(TestClass<U,U> desc, Function4<U,A,B,C,Result<T>> func, String mname) {
		return build(desc, func, func, mname);
	}


	/**
	 * Construct a command builder for a method on the main object
	 * that takes an object parameter.
	 * @param argDesc description of the parameter object
	 * @param rfunc lifted reference implementation
	 * @param sfunc lifted SUT implementation
	 * @param mname method name (when generating tests)
	 * @return function taking an index of the object to run the method on and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <A,B,T> BiFunction<Integer,Integer,Command<?>> build(BiFunction<R,A,Result<T>> rfunc, BiFunction<S,B,Result<T>> sfunc, TestClass<A,B> argDesc, String mname) {
		return build(mainClass, argDesc, rfunc, sfunc, mname);
	}

	/**
	 * Construct a command builder for a method on some object
	 * that takes an object parameter.
	 * @param desc description of object running method
	 * @param argDesc description of the parameter object
	 * @param rfunc lifted reference implementation
	 * @param sfunc lifted SUT implementation
	 * @param mname method name (when generating tests)
	 * @return function taking an index of the object to run the method on and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <A,B,T,U,V> BiFunction<Integer,Integer,Command<?>> build(TestClass<U,V> desc, TestClass<A,B> argDesc, BiFunction<U,A,Result<T>> rfunc, BiFunction<V,B,Result<T>> sfunc, String mname) {
		return (i,j) -> new Command.CommandM<>(desc, argDesc, i, j, rfunc, sfunc, mname);
	}

	/**
	 * Construct a command builder for a method on some object
	 * that takes an object parameter.
	 * @param desc description of object running method
	 * @param argDesc description of the parameter object
	 * @param func lifted implementation
	 * @param mname method name (when generating tests)
	 * @return function taking an index of the object to run the method on and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <A,T,U> BiFunction<Integer,Integer,Command<?>> build(TestClass<U,U> desc, TestClass<A,A> argDesc, BiFunction<U,A,Result<T>> func, String mname) {
		return build(desc, argDesc, func, func, mname);
	}	
	
	/**
	 * Construct a command builder for a method on some object
	 * that takes an object parameter and a pure parameter.
	 * @param desc description of object running method
	 * @param argDesc description of the parameter object
	 * @param rfunc lifted reference implementation
	 * @param sfunc lifted SUT implementation
	 * @param mname method name (when generating tests)
	 * @return function taking an index of the object to run the method on and returning a command.
	 * @see {@link #lift(Function)} for pure results
	 * @see {@link #lift(TestClass, Function) for object results
	 */
	protected <A,B,C,T,U,V> TriFunction<Integer,Integer,C,Command<?>> build(TestClass<U,V> desc, TestClass<A,B> argDesc, TriFunction<U,A,C,Result<T>> rfunc, TriFunction<V,B,C,Result<T>> sfunc, String mname) {
		return (i,j,c) -> new Command.CommandMP<>(desc, argDesc, i, j, c, rfunc, sfunc, mname);
	}

	static enum TestState {
		REFERENCE, SUT, FRAMEWORK;
	}
	private List<Supplier<String>> tests = new ArrayList<>();	
	private volatile TestState currentState = TestState.FRAMEWORK;
	private volatile Command<?> currentCommand;
	private TimeoutExecutor timer = new TimeoutExecutor(() -> doTimeout(), timeout*5); // startup can be slow

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
		for (RegisteredClass<?,?> rc : registeredClasses) {
			rc.clear();
		}
		registeredIndex.clear();
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
		boolean result = expected.includes(actual);
		tests.add(() -> expected.genAssert(this, command.code(this)));
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
			tests.add(() -> currentCommand.code(this) + "; // timeout");
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
	
	/**
	 * Print out methods at the beginning of the test suite.
	 * This implementation prints "assertException" and
	 * a version of "assertEquals" that allows an Integer to be compared to an int.
	 */
	protected void printHelperMethods() {
		System.out.println("\tprotected void assertException(Class<?> exc, Runnable r) {");
		System.out.println("\t\ttry {");
		System.out.println("\t\t\tr.run();");
		System.out.println("\t\t\tassertFalse(\"should have thrown an exception.\",true);");
		System.out.println("\t\t} catch (RuntimeException e) {");
		System.out.println("\t\t\tif (exc == null) return;");
		System.out.println("\t\t\tassertTrue(\"threw wrong exception type: \" + e.getClass(),exc.isInstance(e));");
		System.out.println("\t\t}");
		System.out.println("\t}\n");
		System.out.println("\tprotected void assertEquals(int expected, Integer result) {");
		System.out.println("\t\tsuper.assertEquals(Integer.valueOf(expected),result);");
		System.out.println("\t}\n");		
	}
	
	public void print() {
		if (tests.isEmpty()) {
			System.out.println("import junit.framework.TestCase;\n");
			System.out.println("public class TestGen extends TestCase {");
			System.out.println("\tpublic void test() {");
			System.out.println("\t\tassertTrue(\"Everything is fine\",true);");
			System.out.println("\t}");
			System.out.println("}");
			System.out.println("\n// Congratulations: no bugs found!");
			return;	
		}
		printImports();
		System.out.println("\npublic class TestGen extends TestCase {");
		printHelperMethods();
		System.out.println("\tpublic void test() {");
		for (Supplier<String> test : tests) {
			System.out.println("\t\t" + test.get());
		}
		System.out.println("\t}");
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
		clear();
	}
}
