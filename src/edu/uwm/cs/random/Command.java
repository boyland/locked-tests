package edu.uwm.cs.random;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import edu.uwm.cs.util.TriFunction;
import edu.uwm.cs.util.Union;

public interface Command<T> {
	
	//XXX: How do we handle a command that creates a new SUT ?
	public Result<T> execute(boolean asReference);
	
	/**
	 * Generate Java code for this command.
	 * @param lb literal builder for parameters etc.
	 * @return string of a Java expression
	 */
	public String code(LiteralBuilder lb); //XXX: lb not needed (too late) 

	public static class Default<T> implements Command<T> {
		private final Supplier<Result<T>> refSupp;
		private final Supplier<Result<T>> sutSupp;
		private final String code;
		
		public Default(Supplier<Result<T>> refs, Supplier<Result<T>> suts, String c) {
			refSupp  = refs;
			sutSupp = suts;
			code = c;
		}
		
		public Result<T> execute(boolean asReference) {
			return asReference? refSupp.get() : sutSupp.get();
		}
		
		public String code(LiteralBuilder lb) {
			return code;
		}
	}
	
	static abstract class Builder<R,S,T> {
		protected final Class<R> refClass;
		protected final Class<S> sutClass;
		protected final LiteralBuilder literals;
				
		protected S getSUT(R ref) {
			if (ref == null) return null;
			S sut = sutClass.cast(literals.getTestObject(literals.toString(ref)));
			if (sut == null) throw new NullPointerException("couldn't find SUT for " + ref + " of type " + ref.getClass() + " named " + literals.toString(ref));
			return sut;
		}
		
		@SuppressWarnings("unchecked")
		protected <U> U getTest(U ref) {
			if (literals.isMutableObject(ref)) {
				return (U) literals.getTestObject(literals.toString(ref));
			}
			return ref;
		}

		protected Builder(LiteralBuilder lb, Class<R> rClass, Class<S> sClass) {
			literals = lb;
			refClass = rClass;
			sutClass = sClass;
		}
		
		protected Command<T> build(Supplier<Result<T>> refs, Supplier<Result<T>> suts, String c) {
			return new Default<>(refs,suts,c);
		}
	}
	
	public static class NewInstance<R,S> extends Builder<R,S,Union<R,S>> implements Command<Union<R,S>> {

		private final String sutName;
		
		protected NewInstance(LiteralBuilder lb, Class<R> rClass, Class<S> sClass, String sutName) {
			super(lb, rClass, sClass);
			this.sutName = sutName;
		}
		
		private static <T> T makeInstance(Class<T> clazz) {
			try {
				return clazz.newInstance();
			} catch (InstantiationException|IllegalAccessException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		@Override
		public Result<Union<R, S>> execute(boolean asReference) {
			try {
				Union<R,S> result;
				if (asReference) {
					result = Union.makeR(makeInstance(refClass));
				} else {
					result = Union.makeS(makeInstance(sutClass));
				}
				return new NormalResult<>(result);
			} catch (Exception e) {
				return new ExceptionResult<>(e);
			}
		}

		@Override
		public String code(LiteralBuilder lb) {
			return "new " + sutName + "()";
		}	
	}
	
	public static class NewInstance1<R,S,T,U> extends Builder<R,S,T> implements Function<U,Command<T>> {
		protected final Function<U,Result<T>> constructor;
		protected final String typeName;
		
		public NewInstance1(LiteralBuilder lb, Class<R> rClass, Class<S> sClass, Function<U,Result<T>> cons, String typeName) {
			super(lb,rClass,sClass);
			constructor = cons;
			this.typeName = typeName;
		}

		@Override
		public Command<T> apply(U rarg) {
			U sarg = getTest(rarg);
			return build(
					() -> constructor.apply(rarg),
					() -> constructor.apply(sarg),
					"new " + typeName + "(" + literals.toString(rarg) + ")");
		}		
	}
	
	public static class NewInstance2<R,S,T,U,V> extends Builder<R,S,T> implements BiFunction<U,V,Command<T>> {
		protected final BiFunction<U,V,Result<T>> constructor;
		protected final String typeName;
		
		public NewInstance2(LiteralBuilder lb, Class<R> rClass, Class<S> sClass, BiFunction<U,V,Result<T>> cons, String typeName) {
			super(lb,rClass,sClass);
			constructor = cons;
			this.typeName = typeName;
		}

		@Override
		public Command<T> apply(U rarg1, V rarg2) {
			U sarg1 = getTest(rarg1);
			V sarg2 = getTest(rarg2);
			return build(
					() -> constructor.apply(rarg1, rarg2),
					() -> constructor.apply(sarg1, sarg2),
					"new " + typeName + "(" + literals.toString(rarg1) + "," + literals.toString(rarg2) + ")");
		}		
	}
	
	public static class Builder0<R,S,T> extends Builder<R,S,T> implements Function<R,Command<T>> {
		protected final Function<R,Result<T>> refFunc;
		protected final Function<S,Result<T>> sutFunc;
		protected final String methodName;
		
		protected Builder0(LiteralBuilder lb, Class<R> rClass, Class<S> sClass,
				Function<R,Result<T>> rfunc, Function<S,Result<T>> sfunc, String mname) {
			super(lb,rClass,sClass);
			refFunc = rfunc;
			sutFunc = sfunc;
			methodName = mname;
		}

		@Override
		public Command<T> apply(R ref) {
			S sut = getSUT(ref);
			return build(() -> refFunc.apply(ref),
					     () -> sutFunc.apply(sut), 
					     literals.toString(ref) + "." + methodName + "()");
		}
	}
	
	public static class Builder1<R,S,T,U> extends Builder<R,S,T> implements BiFunction<R,U,Command<T>> {
		protected final BiFunction<R,U,Result<T>> refFunc;
		protected final BiFunction<S,U,Result<T>> sutFunc;
		protected final String methodName;

		protected Builder1(LiteralBuilder lb, Class<R> rClass, Class<S> sClass,
			BiFunction<R,U,Result<T>> rfunc, BiFunction<S,U,Result<T>> sfunc, String mname) {
			super(lb, rClass, sClass);
			refFunc = rfunc;
			sutFunc = sfunc;
			methodName = mname;
		}

		@Override
		public Command<T> apply(R ref, U rarg) {
			S sut = getSUT(ref);
			U sarg = getTest(rarg);
			return build(() -> refFunc.apply(ref,rarg),
					     () -> sutFunc.apply(sut,sarg), 
					     literals.toString(ref) + "." + methodName + "(" + literals.toString(rarg) + ")");
		}
		
	}
	
	public static class BuilderR<R,S,T> extends Builder<R,S,T> implements BiFunction<R,R,Command<T>> {
		protected final BiFunction<R,R,Result<T>> refFunc;
		protected final BiFunction<S,S,Result<T>> sutFunc;
		protected final String methodName;

		protected BuilderR(LiteralBuilder lb, Class<R> rClass, Class<S> sClass,
			BiFunction<R,R,Result<T>> rfunc, BiFunction<S,S,Result<T>> sfunc, String mname) {
			super(lb, rClass, sClass);
			refFunc = rfunc;
			sutFunc = sfunc;
			methodName = mname;
		}

		@Override
		public Command<T> apply(R ref, R ref1) {
			S sut = getSUT(ref);
			S sut1 = getSUT(ref1);
			return build(() -> refFunc.apply(ref,ref1),
					     () -> sutFunc.apply(sut,sut1), 
					     literals.toString(ref) + "." + methodName + "(" + literals.toString(ref1) + ")");
		}
		
	}

	public static class Builder2<R,S,T,U,V> extends Builder<R,S,T> implements TriFunction<R,U,V,Command<T>> {
		protected final TriFunction<R,U,V,Result<T>> refFunc;
		protected final TriFunction<S,U,V,Result<T>> sutFunc;
		protected final String methodName;

		protected Builder2(LiteralBuilder lb, Class<R> rClass, Class<S> sClass,
			TriFunction<R,U,V,Result<T>> rfunc, TriFunction<S,U,V,Result<T>> sfunc, String mname) {
			super(lb, rClass, sClass);
			refFunc = rfunc;
			sutFunc = sfunc;
			methodName = mname;
		}

		@Override
		public Command<T> apply(R ref, U rarg1, V rarg2) {
			S sut = getSUT(ref);
			U sarg1 = getTest(rarg1);
			V sarg2 = getTest(rarg2);
			return build(() -> refFunc.apply(ref,rarg1,rarg2),
					     () -> sutFunc.apply(sut,sarg1,sarg2), 
					     literals.toString(ref) + "." + methodName + "(" + literals.toString(rarg1) + "," + literals.toString(rarg2) + ")");
		}
		
	}
	
	public static class BuilderR1<R,S,T,V> extends Builder<R,S,T> implements TriFunction<R,R,V,Command<T>> {
		protected final TriFunction<R,R,V,Result<T>> refFunc;
		protected final TriFunction<S,S,V,Result<T>> sutFunc;
		protected final String methodName;

		protected BuilderR1(LiteralBuilder lb, Class<R> rClass, Class<S> sClass,
			TriFunction<R,R,V,Result<T>> rfunc, TriFunction<S,S,V,Result<T>> sfunc, String mname) {
			super(lb, rClass, sClass);
			refFunc = rfunc;
			sutFunc = sfunc;
			methodName = mname;
		}

		@Override
		public Command<T> apply(R ref, R ref1, V rarg2) {
			S sut = getSUT(ref);
			S sut1 = getSUT(ref1);
			V sarg2 = getTest(rarg2);
			return build(() -> refFunc.apply(ref,ref1,rarg2),
					     () -> sutFunc.apply(sut,sut1,sarg2), 
					     literals.toString(ref) + "." + methodName + "(" + literals.toString(ref1) + "," + literals.toString(rarg2) + ")");
		}
		
	}

	public static class Builder1R<R,S,T,U> extends Builder<R,S,T> implements TriFunction<R,U,R,Command<T>> {
		protected final TriFunction<R,U,R,Result<T>> refFunc;
		protected final TriFunction<S,U,S,Result<T>> sutFunc;
		protected final String methodName;

		protected Builder1R(LiteralBuilder lb, Class<R> rClass, Class<S> sClass,
			TriFunction<R,U,R,Result<T>> rfunc, TriFunction<S,U,S,Result<T>> sfunc, String mname) {
			super(lb, rClass, sClass);
			refFunc = rfunc;
			sutFunc = sfunc;
			methodName = mname;
		}

		@Override
		public Command<T> apply(R ref, U rarg1, R ref2) {
			S sut = getSUT(ref);
			U sarg1 = getTest(rarg1);
			S sut2 = getSUT(ref2);
			return build(() -> refFunc.apply(ref,rarg1,ref2),
					     () -> sutFunc.apply(sut,sarg1,sut2), 
					     literals.toString(ref) + "." + methodName + "(" + literals.toString(rarg1) + "," + literals.toString(ref2) + ")");
		}
		
	}

	public static class BuilderRR<R,S,T> extends Builder<R,S,T> implements TriFunction<R,R,R,Command<T>> {
		protected final TriFunction<R,R,R,Result<T>> refFunc;
		protected final TriFunction<S,S,S,Result<T>> sutFunc;
		protected final String methodName;

		protected BuilderRR(LiteralBuilder lb, Class<R> rClass, Class<S> sClass,
			TriFunction<R,R,R,Result<T>> rfunc, TriFunction<S,S,S,Result<T>> sfunc, String mname) {
			super(lb, rClass, sClass);
			refFunc = rfunc;
			sutFunc = sfunc;
			methodName = mname;
		}

		@Override
		public Command<T> apply(R ref, R ref1, R ref2) {
			S sut = getSUT(ref);
			S sut1 = getSUT(ref1);
			S sut2 = getSUT(ref2);
			return build(() -> refFunc.apply(ref,ref1,ref2),
					     () -> sutFunc.apply(sut,sut1,sut2), 
					     literals.toString(ref) + "." + methodName + "(" + literals.toString(ref1) + "," + literals.toString(ref2) + ")");
		}
		
	}
}
