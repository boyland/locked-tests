package edu.uwm.cs.util;

public interface TriFunction<A,B,C,U> {
	public U apply(A a, B b, C c);
}
