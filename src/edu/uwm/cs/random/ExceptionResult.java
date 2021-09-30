package edu.uwm.cs.random;

/**
 * The result of a command execution that terminates
 * abruptly with an exception.
 * @param T type the result would be (that is not returned)
 */
public class ExceptionResult<T> implements Result<T> {

	private Throwable reason;
	
	/**
	 * Construct a result in which termination is abrupt.
	 * The exception's class indicates which the exception was.
	 * @param exc exception thrown, if null, then not specific.
	 */
	public ExceptionResult(Throwable exc) {
		reason = exc;
	}

	@Override
	public T getValue() {
		return null;
	}

	@Override
	public boolean includes(Result<T> x, LiteralBuilder lb) {
		if (!(x instanceof ExceptionResult<?>)) return false;
		if (reason == null) return true;
		ExceptionResult<T> other = (ExceptionResult<T>) x;
		return reason.getClass().isInstance(other.reason);
	}

	@Override
	public String genAssert(LiteralBuilder lb, String code) {
		String expected = "null";
		if (reason != null) {
			expected = reason.getClass().getCanonicalName().replace('/', '.') + ".class";
		}
		return "assertException(" + expected + ", () -> " + code + ");";
	}

	@Override
	public String toString() {
		return "ExceptionResult(" + reason.getLocalizedMessage() + ")";
	}
}
