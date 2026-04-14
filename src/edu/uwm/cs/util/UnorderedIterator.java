package edu.uwm.cs.util;

import java.util.Iterator;

import edu.uwm.cs.random.Result;

/** An iterator that doesn't restrict the SUT a particular order. */
public interface UnorderedIterator<E> extends Iterator<E> {
	/**
	 * Return another element from the iterator.  It can be nondeterministic.
	 * @return result already lifted to show various outcomes, including nondeterminism
	 */
	public Result<E> nextChoice();
}
