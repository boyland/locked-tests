package edu.uwm.cs.random;

import edu.uwm.cs.util.Union;

/**
 * A result that is an object for which identity is important.
 * The type of objects must be registered in a description.
 * The reference and SUT types can be the same (e.g. an iteratpr of
 * some other type), but we still use {@link Union} objects 
 * to handle the case that they could be different.
 * @param R type of object used in reference implementation
 * @param S type of object used in SUT
 */
public class ObjectResult<R, S> extends NormalResult<Union<R, S>> {
	private final TestClass<R,S> desc;
	private boolean newObjectResult;
	private boolean sutHasValue;
	private boolean sutHasException;
	private boolean sutIsNull;
	
	public ObjectResult(TestClass<R,S> d, R value) {
		super(Union.makeR(value));
		desc = d;
	}
	
	public ObjectResult(S value, TestClass<R,S> d) {
		super(Union.makeS(value));
		desc = d;
	}
	
	public ObjectResult(TestClass<R,S> d, R value1, S value2) {
		super(Union.make(value1, value2));
		desc = d;
	}
	
	/**
	 * Create a result with a reference value
	 * @param d description
	 * @param value result
	 * @return object result instance
	 */
	public static <T,U> ObjectResult<T,U> create(TestClass<T,U> d, T value) {
		return new ObjectResult<>(d,value);
	}
	
	/**
	 * Create a result with a SUT value
	 * @param T reference type
	 * @param U SUT type
	 * @param value value of SUT type
	 * @param d registration
	 * @return object result instance
	 */
	public static <T,U> ObjectResult<T,U> create(U value, TestClass<T,U> d) {
		return new ObjectResult<>(value,d);
	}
	
	@Override
	public boolean includes(Result<Union<R,S>> x) {
		if (super.getValue() == null) return super.includes(x);
		R ref = value.getR();
		int index = desc.indexOf(ref);
		if (index == -1) {
			newObjectResult = true; // do it before checking otherValue
		}
		if (!(x instanceof NormalResult<?>)) {
			sutHasException = true;
			return false;
		}
		Union<R,S> otherValue = x.getValue();
		if (otherValue == null) {
			sutIsNull = true;
			return false;
		}
		S sut = otherValue.getS();
		if (index == -1) {
			desc.register(ref, sut);
			sutHasValue = true;
			return true;
		} else {
			return sut == desc.getSUTObject(index);
		}
	}

	@Override
	public String genAssert(LiteralBuilder lb, String code) {
		if (value == null) return super.genAssert(lb, code);
		if (sutHasException) {
			return code + "; // should not crash";
		}
		if (sutIsNull) {
			return "assertNotNull(" + code + ");";
		}
		int index = desc.indexOf(value.getR());
		if (index == -1) {
			// This is a fatal error with the system; this shouldn't happen
			System.out.println("newObjectResult = " + newObjectResult);
			System.out.println("sutHasValue = " + sutHasValue);
			System.out.println("sutisNull = " + sutIsNull);
			System.out.println("sutHasException = " + sutHasException);
			System.out.println("value.getR = " + value.getR());
			System.out.println("value.getS = " + value.getS());
			throw new IllegalStateException("value not registered yet? " + value);
		}
		String name = desc.getIdentifier(index);
		if (newObjectResult) {
			if (sutHasValue) {
				return desc.getTypeName() + " " + name + " = " + code + ";";
			} else {
				return "assertNotNull(" + code + ");";
			}
		} else {
			return "assertSame(" + name + "," + code + ");";
		}
	}

}
