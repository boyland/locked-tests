package edu.uwm.cs.util;

import java.util.Set;

public interface UnorderedSet<E> extends Set<E>, Unordered<E> {
	public UnorderedIterator<E> unorderedIterator();
}
