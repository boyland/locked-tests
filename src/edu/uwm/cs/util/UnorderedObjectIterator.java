package edu.uwm.cs.util;

import java.util.Iterator;

import edu.uwm.cs.random.Result;

public interface UnorderedObjectIterator<R, S> extends Iterator<R> {
	/**
	 * Return another element from the iterator.  It can be nondeterministic.
	 * @return result already lifted to show various outcomes, including nondeterminism
	 */
	public Result<Union<R,S>> nextChoice();
}
