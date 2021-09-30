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
	public abstract boolean isR();
	public boolean isS() { return !isR(); }
	
	@Override
	public String toString() {
		Object x = get();
		return "Union(" + x + ":" + x.getClass() + ")";
	}
	
	public static <R,S> Union<R,S> makeR(R x) { return new ChoiceR<R,S>(x); }
	public static <R,S> Union<R,S> makeS(S x) { return new ChoiceS<R,S>(x); }
	
	private static class ChoiceR<R,S> extends Union<R,S> {
		final R value;
		
		ChoiceR(R val) {
			value = val;
		}
		
		@Override
		public Object get() { return value; }
		
		@Override
		public R getR() { return value; }
		
		@Override
		public boolean isR() { return true; }
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
		
		@Override
		public boolean isR() { return false; }
	}

}
