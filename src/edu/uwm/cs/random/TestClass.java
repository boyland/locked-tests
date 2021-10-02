package edu.uwm.cs.random;

/**
 * A description of a class used in random testing.
 * This class keeps track of instances created
 * @param R type used by the reference class
 * @param S type used by the SUT
 */
public interface TestClass<R, S> {
	public Class<R> getRefClass();
	public Class<S> getSUTClass();
	
	/**
	 * Get a string that can be used as the type in 
	 * generated test code.  The type should be the one 
	 * used by the SUT.
	 * @return string for the type used here
	 */
	public String getTypeName();
	
	/**
	 * How many instances have been registered for this type?
	 * @return number of instances registered.
	 */
	public int size();
	
	/**
	 * Register an instance of this type.
	 * @param ref object for the reference implementation
	 * @param sut object for the SUT
	 */
	public void register(R ref, S sut);
	
	/**
	 * Return the index of this reference object in the 
	 * list of elements created already.  Returns -1 if not
	 * yet registered.
	 * @param ref reference object
	 * @return index so that getRefObject(index) == ref, or -1
	 */
	public int indexOf(R ref);
	
	/**
	 * Remove all the registered objects. 
	 */
	public void clear();
	
	/**
	 * Return a reference implementation object registered.
	 * @param i index, must be less than size()
	 * @return reference implementation object or null if i is negative
	 */
	public R getRefObject(int i);
	
	/**
	 * Return a SUT object registered
	 * @param i index, must be less than size()
	 * @return SUT object, or null if i is negative
	 */
	public S getSUTObject(int i);
}
