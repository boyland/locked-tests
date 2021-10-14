package edu.uwm.cs.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Enforce strict fail-fast for all iterator methods.
 * @param E element type
 */
public class FailFastIterator<E> extends FilterIterator<E> {

	private Supplier<Integer> containerVersion;
	private Supplier<Integer> newContainerVersion;
	private int version;
	
	public FailFastIterator(Iterator<E> it, Supplier<Integer> getVersion) {
		super(it);
		containerVersion = getVersion;
		newContainerVersion = getVersion;
		version = containerVersion.get();
	}

	public FailFastIterator(Iterator<E> it, Supplier<Integer> getVersion, Supplier<Integer> getNewVersion) {
		super(it);
		containerVersion = getVersion;
		newContainerVersion = getNewVersion;
		version = containerVersion.get();
	}

	protected void checkVersion() {
		if (version != containerVersion.get()) {
			throw new ConcurrentModificationException("stale!");
		}
	}
	
	@Override
	public boolean hasNext() {
		checkVersion();
		return super.hasNext();
	}

	@Override
	public E next() {
		checkVersion();
		return super.next();
	}

	@Override
	public void remove() {
		checkVersion();
		super.remove();
		version = newContainerVersion.get();
	}
	
}
