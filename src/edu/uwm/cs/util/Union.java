package edu.uwm.cs.util;

/**
 * A class for values that can be one of two types.
 * @param R type 1
 * @param S type 2
 */
public abstract class Union<R, S> {

	public R getR() { throw new IllegalStateException("not R"); }
	public S getS() { throw new IllegalStateException("not S"); }
	
	public abstract Object get();
	
	@Override
	public String toString() {
		Object x = get();
		return "Union(" + x + ":" + x.getClass() + ")";
	}
	
	public static <R,S> Union<R,S> makeR(R x) { 
		if (x == null) return null;
		return new ChoiceR<R,S>(x); 
	}
	
	public static <R,S> Union<R,S> makeS(S x) { 
		if (x == null) return null;
		return new ChoiceS<R,S>(x); 
	}
	
	public static <T> Union<T,T> make(T x) {
		if (x == null) return null;
		return new Both<>(x);
	}
	
	@SuppressWarnings("unchecked")
	public static <R,S> Union<R,S> make(R x, S y) {
		if (x == null || y == null) return null;
		if (x != y) throw new IllegalArgumentException("can only union identical objects");
		return (Union<R, S>) new Both<R>(x);
	}
	
	private static class ChoiceR<R,S> extends Union<R,S> {
		final R value;
		
		ChoiceR(R val) {
			value = val;
		}
		
		@Override
		public Object get() { return value; }
		
		@Override
		public R getR() { return value; }
	}

	private static class ChoiceS<R,S> extends Union<R,S> {
		final S value;
		
		ChoiceS(S val) {
			value = val;
		}
		
		@Override
		public Object get() { return value; }

		@Override
		public S getS() { return value; }
	}

	private static class Both<T> extends Union<T,T> {
		final T value;
		
		Both(T val) {
			value = val;
		}
		
		
		@Override
		public Object get() { return value; }
		
		@Override
		public T getR() { return value; }
		
		@Override
		public T getS() { return value; }
	}
}
