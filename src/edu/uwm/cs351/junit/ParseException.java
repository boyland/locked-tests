package edu.uwm.cs351.junit;

public class ParseException extends RuntimeException {
	/**
	 * Keep Eclipse happy
	 */
	private static final long serialVersionUID = 1L;

	public ParseException(String s) { super(s); }
}