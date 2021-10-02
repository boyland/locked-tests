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
	 * Create a result where the two types are the same.
	 * @param d description
	 * @param value result
	 * @return
	 */
	public static <T> ObjectResult<T,T> create(TestClass<T,T> d, T value) {
		return new ObjectResult<>(d,value,value);
	}
	
	@Override
	public boolean includes(Result<Union<R,S>> x) {
		if (super.getValue() == null) return super.includes(x);
		if (!(x instanceof NormalResult<?>)) return false;
		Union<R,S> otherValue = x.getValue();
		if (otherValue == null) return false;
		R ref = value.getR();
		S sut = otherValue.getS();
		int index = desc.indexOf(ref);
		if (index == -1) {
			newObjectResult = true;
			desc.register(ref, sut);
			return true;
		} else {
			return sut == desc.getSUTObject(index);
		}
	}

	@Override
	public String genAssert(LiteralBuilder lb, String code) {
		if (value == null) return super.genAssert(lb, code);
		String name = lb.toString(value.getR());
		if (newObjectResult) {
			return desc.getTypeName() + " " + name + " = " + code + ";";
		} else {
			return "assertSame(" + name + "," + code + ");";
		}
	}

}
