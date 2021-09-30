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
	
	/**
	 * Return the string representation of the type of this object.
	 * @param x object whose type will be represented
	 * @return string for type
	 */
	public String toTypeString(Object x);
	
	/**
	 * Return whether the object (from the reference implementation)
	 * is a mutable value which will be different in the SUT implementations,
	 * For these objects, equality should be reference equality.
	 * @param x object to check, from the reference implementation
	 * @return whether this is a mutable object
	 */
	public boolean isMutableObject(Object x);
	
	/**
	 * Return a string for a new mutable result that needs a name.
	 * @param ref mutable object from the reference implementation, must not be null
	 * @param test mutable object from the SUT, must not be null
	 * @return string to name this object, must be unique for the test.
	 */
	public String registerMutableObject(Object ref, Object test);
	
	/**
	 * Return the corresponding mutable value from the test implementation.
	 * @param registeredName name of the mutable value
	 * @return the test object for this name.
	 */
	public Object getTestObject(String registeredName);
}
