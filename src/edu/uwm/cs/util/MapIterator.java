package edu.uwm.cs.util;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Map an iterator from one type to one on a different type.
 * @param S source type
 * @param T target type
 */
public class MapIterator<S,T> implements Iterator<T> {
	private final Function<S,T> map;
	private final Iterator<S> source;
	
	/**
	 * Create a new map iterator with given function and source
	 * @param m function to map over iterator, must not be null
	 * @param s source of elements, must not be null
	 */
	public MapIterator(Function<S,T> m, Iterator<S> s) {
		map = m;
		source = s;
	}
	
	@Override
	public boolean hasNext() {
		return source.hasNext();
	}
	
	@Override
	public T next() {
		return map.apply(source.next());
	}
	
	@Override
	public void remove() {
		source.remove();
	}
}
