package edu.uwm.cs.util;

import java.util.Iterator;

/**
 * A decorator on an iterator.
 * @param E element type
 */
public class FilterIterator<E> implements Iterator<E> {

	private Iterator<E> wrapped;
	
	public FilterIterator(Iterator<E> it) {
		wrapped = it;
	}
	
	@Override
	public boolean hasNext() {
		return wrapped.hasNext();
	}

	@Override
	public E next() {
		return wrapped.next();
	}
	
	@Override
	public void remove() {
		wrapped.remove();
	}
}
