package edu.uwm.cs.random;

/**
 * The representation of the result of executing a command.
 * We use this class for both expected and actual results,
 * but these behave somewhat differently.
 * @param T the type of resulting values
 */
public interface Result<T> {

	/**
	 * Return the value (if any) associated with this result.
	 * A null result is not distinguished from abrupt or void
	 * termination.
	 * @return he value (if any) associated with this result.
	 */
	public T getValue();
	
	/**
	 * Generate code that can be placed in an JUnit test case.
	 * @param code code to generate the actual that should be tested.
	 * @return code that can be added to a test case.
	 */
	public String genAssert(LiteralBuilder lb, String code);
	
	/**
	 * Check if the parameter matches this one.
	 * As a side-effect of this operation, the expected 
	 * value may remove other possibilities.
	 * @param other (typically the expected Result)
	 * @return true if the object matches.
	 */
	public boolean includes(Result<T> other);
}
