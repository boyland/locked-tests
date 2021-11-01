package edu.uwm.cs.random;

import java.util.Arrays;

/**
 * An execution that terminates normally
 * with a result value.
 * For normal termination of a void method, see {@link voidResult()}.
 * @param T the result type
 */
public class NormalResult<T> implements Result<T> {

	protected final T value;
	
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
	public boolean includes(Result<T> x) {
		if (!(x instanceof NormalResult<?>)) return false;
		NormalResult<?> other = (NormalResult<?>)x;
		if (value == null) return other.value == null;
		if (other.value == null) return false;
		if (value instanceof Object[]) return Arrays.equals((Object[])value,(Object[])other.value);
		return value.equals(other.value);
	}

	@Override
	public String genAssert(LiteralBuilder lb, String code) {
		if (value == null) {
			return "assertNull(" + code + ");";
		} else if (value instanceof Object[]) {
			StringBuilder sb = new StringBuilder();
			Object[] array = (Object[])value;
			for (Object x : array) {
				if (sb.length() > 0) sb.append(',');
				else if (array.length == 1 && (array[0] == null || array[0] instanceof Object[])) sb.append("(Object)");
				sb.append(lb.toString(x));
			}
			return "assertEquals(java.util.Arrays.asList(" + sb + "),java.util.Arrays.asList(" + code + "));";
		} else {
			return "assertEquals(" + lb.toString(value) + "," + code + ");";
		} 
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
