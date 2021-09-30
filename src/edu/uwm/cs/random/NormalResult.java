package edu.uwm.cs.random;

/**
 * An execution that terminates normally
 * with a result value.
 * For normal termination of a void method, see {@link voidResult()}.
 * @param T the result type
 */
public class NormalResult<T> implements Result<T> {

	private T value;
	private boolean newMutableResult = false;
	
	/**
	 * A normal result that returns a value
	 * @param value value returned.
	 */
	public NormalResult(T value) {
		this.value = value;
	}

	@Override
	public T getValue() {
		return value;
	}
	
	@Override
	public boolean includes(Result<T> x, LiteralBuilder lb) {
		if (!(x instanceof NormalResult<?>)) return false;
		NormalResult<?> other = (NormalResult<?>)x;
		if (value == null) return other.value == null;
		if (other.value == null) return false;
		if (lb.isMutableObject(value)) {
			String name = lb.toString(value);
			if (name == null) {
				newMutableResult = true;
				lb.registerMutableObject(value, other.value);
				return true;
			} else {
				return other.value == lb.getTestObject(name);
			}
		}
		return value.equals(other.value);
	}

	@Override
	public String genAssert(LiteralBuilder lb, String code) {
		if (value == null) {
			return "assertNull(" + code + ");";
		} else if (newMutableResult) {
			String name = lb.toString(value);
			return lb.toTypeString(value) + " " + name + " = " + code + ";";
		} else if (lb.isMutableObject(value)) {
			return "assertSame(" + lb.toString(value) + "," + code + ");";
		}
		return "assertEquals(" + lb.toString(value) + "," + code + ");";
	}
	
	private static final NormalResult<?> nullResult = new NormalResult<Object>(null);

	/**
	 * Return a shared object that handles normal termination with a null result.
	 * @see {@link voidResult()}
	 * @return shared void result object.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Result<T> nullResult() {
		return (Result<T>) nullResult;
	}
	
	/**
	 * The shared void return result object.
	 */
	public static final Result<Void> voidResult = new NormalResult<Void>(null) {
		@Override
		public String genAssert(LiteralBuilder lb, String code) {
			return code + "; // should terminate normally";
		}
	};
	
	/**
	 * Return the shared void result object.
	 * @return shared void result object
	 */
	public static Result<Void> voidResult() {
		return voidResult;
	}
	
	@Override
	public String toString() {
		return "NormalResult(" + value + ")";
	}
}
