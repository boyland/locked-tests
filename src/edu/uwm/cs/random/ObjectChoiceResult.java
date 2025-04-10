package edu.uwm.cs.random;

import java.util.Set;
import java.util.function.Consumer;

import edu.uwm.cs.util.Union;

public class ObjectChoiceResult<R, S> implements Result<Union<R, S>> {
	private TestClass<R,S> desc;
	private Set<R> possibilities;
	private Consumer<R> notifier;
	private Result<Union<R,S>> delegate = null;
	
	/**
	 * Create a choice result.
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
			// XXX: fix the remainder to use indexes etc.
			if (possibilities.contains(other.getValue())) {
				Union<R,S> answer = other.getValue();
				delegate = new NormalResult<>(answer);
				if (notifier != null) notifier.accept(answer.getR());
				return true;
			}
		}
		delegate = new ObjectResult<>(desc, possibilities.iterator().next());
		return false;
	}

}
