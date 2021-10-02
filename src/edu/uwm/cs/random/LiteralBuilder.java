package edu.uwm.cs.random;

/**
 * A tool for converting Java values into Java code
 * that when executed returns this value.
 */
public interface LiteralBuilder {
	/**
	 * Return a string that evaluates to this value.
	 * If the value is mutable, return the registered name,
	 * or null is no name yet registered.
	 * @param x object to generate code for
	 * @return code to evaluate to this result, or null if not yet registered.
	 */
	public String toString(Object x);
}
