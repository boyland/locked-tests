package edu.uwm.cs.util;

public interface Unordered<E> {
	/**
	 * Return an unordered iterator on the elements of this container
	 * @return new unordered iterator
	 */
	public UnorderedIterator<E> unorderedIterator();
}
