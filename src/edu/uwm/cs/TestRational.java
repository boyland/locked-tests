package edu.uwm.cs;

import edu.uwm.cs.junit.LockedTestCase;

public class TestRational extends LockedTestCase {

	Rational one = new Rational(1);
	Rational half = new Rational(1,2);
	
	public void test0() {
		assertEquals(Ts(898146272),one.toString());
		assertEquals(Ts(1325465892),half.toString());
		assertEquals(Tb(187695053),one.equals(new Rational(1)));
		assertEquals(Tb(1266981796),one.equals(half));
		assertEquals(true,half.equals(half));
		assertEquals(Tb(125008712),half.equals(one));
		assertEquals(Tb(1175806565),half.equals(new Rational(2,4)));
	}
	
	public void test1() {
		assertEquals(Ts(1549167507),half.add(half).toString());
		assertEquals(Tc(753076128),half.toString().charAt(1));
		Rational two = one.add(one);
		assertEquals(Ts(1985951271),two.toString());
		assertEquals(T(973866810),half.add(half));
		assertEquals(T(2083203738),half.add(one));
	}
	
	public static void main(String[] args) {
	  LockedTestCase.unlockAll("edu.uwm.cs.TestRational");
	}
}
