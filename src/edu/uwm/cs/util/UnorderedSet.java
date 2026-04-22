package edu.uwm.cs.util;

import java.util.Set;

/**
 * Set with an option for unordered iteration.
 * @param <E> element type
 */
public interface UnorderedSet<E> extends Set<E>, Unordered<E> {
	/**
	 * Return an unordered iterator for this set.
	 * This permits the set to be iterated in any order
	 * in a reference implementation.
	 * @return new unordered iterator
	 */
	public UnorderedIterator<E> unorderedIterator();
}
