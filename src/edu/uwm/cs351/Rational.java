package edu.uwm.cs351;

public class Rational implements Comparable<Rational> {

	private final int den, num;
	
	public Rational(int n) {
		num = n;
		den = 1;
	}
	
	public static int gcd(int a, int b) {
		if (b < 0) return gcd(a,-b);
		if (a < 0) return gcd(-a,b);
		if (a < b) return gcd(b,a);
		if (a == 0 || b == 0) return 0;
		int q = a / b;
		int r = a - b*q;
		if (r == 0) return b;
		return gcd(b,r);
	}
	
	public Rational (int m, int n) {
		if (n == 0) {
			throw new IllegalArgumentException("rational number cannot have zero denominator");
		}
		if (n < 0) {
			m = -m;
			n = -n;
		}
		if (m == 0) {
			n = 1;
		} else {
			int r = gcd(m,n);
			m = m/r;
			n = n/r;
		}
		num = m;
		den = n;
	}
	
	@Override
	public String toString() {
		return num + "/" + den;
	}
	
	public static Rational fromString(String s) {
		int i = s.indexOf('/');
		return new Rational(Integer.parseInt(s.substring(0,i)),Integer.parseInt(s.substring(i+1)));
	}
	
	public int hashCode() {
		return num * 1021 + den;
	}
	
	public boolean equals(Object x) {
		if (x instanceof Rational) {
			Rational r = (Rational)x;
			return num == r.num && den == r.den;
		}
		return false;
	}
	
	@Override
	public int compareTo(Rational r) {
		long l1 = num * (long)r.den;
		long l2 = den * (long)r.num;
		return Long.compare(l1, l2);
	}

	public Rational add(Rational r) {
		return new Rational(den*r.num+num*r.den,den*r.den);
	}
	
	public int roundDown() {
		return num / den;
	}
}
