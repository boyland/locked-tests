package edu.uwm.cs351;

import edu.uwm.cs351.junit.LockedTestCase;

public class TestRational extends LockedTestCase {

	Rational one = new Rational(1);
	Rational half = new Rational(1,2);
	
	public void testSimple() {
		assertEquals(Ts(898146272),one.toString());
		assertEquals(Ts(1325465892),half.toString());
		assertEquals(Tb(187695053),one.equals(new Rational(1)));
		assertEquals(Tb(1266981796),one.equals(half));
		assertEquals(true,half.equals(half));
		assertEquals(Tb(125008712),half.equals(one));
		assertEquals(Tb(1175806565),half.equals(new Rational(2,4)));
	}
	
	public void testAdd() {
		assertEquals(Ts(1549167507),half.add(half).toString());
		Rational two = one.add(one);
		assertEquals(Ts(1985951271),two.toString());
		assertEquals(T(973866810),half.add(half));
	}
}
