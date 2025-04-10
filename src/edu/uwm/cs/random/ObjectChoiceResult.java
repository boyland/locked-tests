package edu.uwm.cs.random;

import java.util.Set;
import java.util.function.Consumer;

import edu.uwm.cs.util.Union;

/**
 * A result which combines the aspects of 
 * {@link ObjectResult} and {@link ChoiceResult},
 * except that it does not handle when one of the values
 * is newly created.  It can only handle objects that are pre-existing.
 * It also only can handle if the reference implementation creates this object result.
 * @param R Reference type
 * @param S type of the situation under test.
 */
public class ObjectChoiceResult<R, S> implements Result<Union<R, S>> {
	private TestClass<R,S> desc;
	private Set<R> possibilities;
	private Consumer<R> notifier;
	private Result<Union<R,S>> delegate = null;
	
	/**
	 * Create a choice result.
	 * @param d description of the connection between REF and SUT.
	 * @param poss possibilities to chose from, must not be empty
	 * @param not notifier to be called when choice is fixed
	 */
	public ObjectChoiceResult(TestClass<R,S> d, Set<R> poss, Consumer<R> not) {
		if (poss.isEmpty()) throw new IllegalArgumentException("no possibilities!");
		desc = d;
		possibilities = poss;
		notifier = not;
	}
	
	@Override
	public String genAssert(LiteralBuilder arg0, String arg1) {
		if (delegate == null) throw new IllegalStateException("Not checked yet!");
		return delegate.genAssert(arg0, arg1);
	}

	@Override
	public Union<R,S> getValue() {
		if (delegate == null) throw new IllegalStateException("Not checked yet!");
		return delegate.getValue();
	}

	@Override
	public boolean includes(Result<Union<R,S>> x) {
		if (delegate != null) return delegate.includes(x);
		if (possibilities.size() == 1) {
			R answer = possibilities.iterator().next();
			delegate = new ObjectResult<>(desc, answer);
			if (notifier != null) notifier.accept(answer);
			return delegate.includes(x);
		}
		if (!(x instanceof NormalResult<?>)) {
			if (x instanceof ChoiceResult<?>) throw new IllegalArgumentException("cannot have choices on both sides!");
			R answer = possibilities.iterator().next();
			delegate = new ObjectResult<>(desc,answer);
			if (notifier != null) notifier.accept(answer);
			return false;
		}
		if (x.getValue() == null) {
			if (possibilities.contains(null)) {
				delegate = new NormalResult<>(null);
				if (notifier != null) notifier.accept(null);
				return true;
			}
		} else {
			ObjectResult<R,S> other = (ObjectResult<R,S>)x;
			S sObject = other.getValue().getS();
			for (R rObject : possibilities) {
				int i = desc.indexOf(rObject);
				if (sObject == desc.getSUTObject(i)) {
					delegate = new ObjectResult<R,S>(desc, rObject);
					if (notifier != null) notifier.accept(rObject);
					return true;
				}
			}
		}
		delegate = new ObjectResult<>(desc, possibilities.iterator().next());
		return false;
	}

}
