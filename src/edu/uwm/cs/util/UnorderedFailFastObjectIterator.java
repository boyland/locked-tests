package edu.uwm.cs.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import edu.uwm.cs.random.ExceptionResult;
import edu.uwm.cs.random.ObjectChoiceResult;
import edu.uwm.cs.random.ObjectResult;
import edu.uwm.cs.random.Result;
import edu.uwm.cs.random.TestClass;
import edu.uwm.cs.util.Union;

public class UnorderedFailFastObjectIterator<R,S> implements UnorderedObjectIterator<R,S> {
	private final TestClass<R,S> desc;
	private final List<R> elements;
	private final Collection<? extends R> source;
	private final Supplier<Integer> versionSupplier;
	private final Consumer<R> remover;
	
	private R current;
	private boolean canRemove;
	private int colVersion;

	public UnorderedFailFastObjectIterator(TestClass<R,S> d, Collection<? extends R> col, Supplier<Integer> v, Consumer<R> rem) {
		desc = d;
		source = col;
		versionSupplier = v;
		remover = rem;
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
	public R next() {
		checkVersion();
		Iterator<R> it = elements.iterator();
		current = it.next();
		it.remove();
		canRemove = true;
		return current;
	}

	@Override 
	public void remove() {
		checkVersion();
		if (!canRemove) throw new IllegalStateException("nothing to remove");
		remover.accept(current);
		colVersion = versionSupplier.get();
		current = null;
		canRemove = false;
	}

	@Override
	public Result<Union<R,S>> nextChoice() {
		if (versionSupplier.get() != colVersion) return new ExceptionResult<>(new ConcurrentModificationException("stale"));
		if (elements.isEmpty()) return new ExceptionResult<>(new NoSuchElementException("no more"));
		if (elements.size() == 1) {
			return new ObjectResult<>(desc,next());
		}
		canRemove = false; // cannot remove until we know what the value is
		return new ObjectChoiceResult<R,S>(desc, new HashSet<>(elements), e -> {
			current = e;
			canRemove = true;
			elements.remove(current);
		});
	}
}
