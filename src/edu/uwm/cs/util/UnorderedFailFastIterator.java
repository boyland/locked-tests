package edu.uwm.cs.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import edu.uwm.cs.random.ChoiceResult;
import edu.uwm.cs.random.ExceptionResult;
import edu.uwm.cs.random.NormalResult;
import edu.uwm.cs.random.Result;

public class UnorderedFailFastIterator<E> implements UnorderedIterator<E> {
	private final List<E> elements;
	private final Collection<? extends E> source;
	private final Supplier<Integer> versionSupplier;
	
	private E current;
	private boolean canRemove;
	private int colVersion;

	public UnorderedFailFastIterator(Collection<? extends E> col, Supplier<Integer> v) {
		source = col;
		versionSupplier = v;
		elements = new ArrayList<>(source);
		colVersion = versionSupplier.get();
	}

	protected void checkVersion() {
		if (versionSupplier.get() != colVersion) throw new ConcurrentModificationException("stale");
	}
	
	@Override
	public boolean hasNext() {
		checkVersion();
		return !elements.isEmpty();
	}

	@Override
	public E next() {
		checkVersion();
		Iterator<E> it = elements.iterator();
		current = it.next();
		it.remove();
		canRemove = true;
		return current;
	}

	@Override 
	public void remove() {
		checkVersion();
		if (!canRemove) throw new IllegalStateException("nothing to remove");
		source.remove(current);
		colVersion = versionSupplier.get();
		current = null;
		canRemove = false;
	}

	@Override
	public Result<E> nextChoice() {
		if (versionSupplier.get() != colVersion) return new ExceptionResult<E>(new ConcurrentModificationException("stale"));
		if (elements.isEmpty()) return new ExceptionResult<E>(new NoSuchElementException("no more"));
		if (elements.size() == 1) {
			return new NormalResult<>(next());
		}
		canRemove = false; // cannot remove until we know what the value is
		return new ChoiceResult<E>(new HashSet<>(elements), e -> {
			current = e;
			canRemove = true;
			elements.remove(current);
		});
	}
}
