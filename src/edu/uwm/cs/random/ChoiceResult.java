package edu.uwm.cs.random;

import java.util.Set;
import java.util.function.Consumer;

import edu.uwm.cs.random.LiteralBuilder;
import edu.uwm.cs.random.NormalResult;
import edu.uwm.cs.random.Result;

/**
 * A result that could be one of a number of possibilities.
 * If the actual result matches, we proceed assuming that value.  If it
 * doesn't match any of the possibilities, we generate a match failure.
 * @param T element type
 */
public class ChoiceResult<T> implements Result<T> {

	private Set<T> possibilities;
	private Consumer<T> notifier;
	private Result<T> delegate = null;
	
	/**
	 * Create a choice result.
	 * @param poss possibilities to chose from, must not be empty
	 * @param not notifier to be called when choice is fixed
	 */
	public ChoiceResult(Set<T> poss, Consumer<T> not) {
		if (poss.isEmpty()) throw new IllegalArgumentException("no possibilities!");
		possibilities = poss;
		notifier = not;
	}
	
	@Override
	public String genAssert(LiteralBuilder arg0, String arg1) {
		if (delegate == null) throw new IllegalStateException("Not checked yet!");
		return delegate.genAssert(arg0, arg1);
	}

	@Override
	public T getValue() {
		if (delegate == null) throw new IllegalStateException("Not checked yet!");
		return delegate.getValue();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean includes(Result<T> x) {
		if (!(x instanceof NormalResult<?>)) {
			if (x instanceof ChoiceResult<?>) throw new IllegalArgumentException("cannot have choices on both sides!");
			delegate = (NormalResult<T>)NormalResult.voidResult;
			return false;
		}
		NormalResult<?> other = (NormalResult<?>)x;
		if (possibilities.contains(other.getValue())) {
			T answer = (T)other.getValue();
			delegate = new NormalResult<>(answer);
			if (notifier != null) notifier.accept(answer);
			return true;
		} else {
			delegate = new NormalResult<T>(possibilities.iterator().next());
		}
		return false;
	}

}
