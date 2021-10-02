package edu.uwm.cs.util;

import java.util.Iterator;

/**
 * Prevent remove from having effect.
 * @param E element type
 */
public class RemovelessIterator<E> extends FilterIterator<E> {

	public RemovelessIterator(Iterator<E> it) {
		super(it);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("remove not supported");
	}

}
