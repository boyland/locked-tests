package edu.uwm.cs.random;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import edu.uwm.cs.util.Function4;
import edu.uwm.cs.util.TriFunction;
import edu.uwm.cs.util.Union;

public interface Command<T> {
	
	public Result<T> execute(boolean asReference);
	
	/**
	 * Generate Java code for this command.
	 * @param lb literal builder for parameters etc.
	 * @return string of a Java expression
	 */
	public String code(LiteralBuilder lb); //XXX: lb not needed (too late) 

	public default String code(String template, String... args) {
		StringBuilder sb = new StringBuilder();
		if (args.length > 0 && template.indexOf('$') < 0) {
			sb.append(args[0]);
			sb.append('.');
			sb.append(template);
			sb.append('(');
			int argNum = -1;
			for (String arg : args) {
				++argNum;
				if (argNum == 0) continue;
				if (argNum > 1) sb.append(',');
				sb.append(arg);
			}
			sb.append(')');
		} else {
			int n = template.length();
			for (int i=0; i < n; ++i) {
				char ch = template.charAt(i);
				if (ch == '\\') {
					sb.append(template.charAt(++i));
				} else if (ch == '$') {
					int index = template.charAt(++i) - '0';
					sb.append(args[index]);
				} else {
					sb.append(ch);
				}
			}
		}
		return sb.toString();
	}
	
	/**
	 * Default implementation of a command.
	 * @param T result type
	 */
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
	
	public static class NewCommand<R,S> implements Command<Union<R,S>> {
		private final TestClass<R,S> testClass;
		private final Supplier<R> rConstr;
		private final Supplier<S> sConstr;

		public NewCommand(TestClass<R,S> tc) {
			this(tc, () -> makeInstance(tc.getRefClass()), () -> makeInstance(tc.getSUTClass()));
		}
		
		public NewCommand(TestClass<R,S> tc, Supplier<R> rc, Supplier<S> sc) {
			testClass = tc;
			rConstr = rc;
			sConstr = sc;
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
				if (asReference) {
					return new ObjectResult<>(testClass,rConstr.get());
				} else {
					return new ObjectResult<>(sConstr.get(),testClass);
				}
			} catch (Exception e) {
				return new ExceptionResult<>(e);
			}
		}

		@Override
		public String code(LiteralBuilder lb) {
			return "new " + testClass.getTypeName() + "()";
		}
	}

	public static class NewCommand1<R,S,U> implements Command<Union<R,S>> {
		private final TestClass<R,S> testClass;
		private final U arg;
		private final Function<U,R> rConstr;
		private final Function<U,S> sConstr;

		public NewCommand1(TestClass<R,S> tc, U a, Function<U,R> rc, Function<U,S> sc) {
			testClass = tc;
			arg = a;
			rConstr = rc;
			sConstr = sc;
		}
		
		@Override
		public Result<Union<R, S>> execute(boolean asReference) {
			try {
				if (asReference) {
					return new ObjectResult<>(testClass,rConstr.apply(arg));
				} else {
					return new ObjectResult<>(sConstr.apply(arg),testClass);
				}
			} catch (Exception e) {
				return new ExceptionResult<>(e);
			}
		}

		@Override
		public String code(LiteralBuilder lb) {
			return "new " + testClass.getTypeName() + "(" + lb.toString(arg) + ")";
		}
	}

	public static class NewCommand2<R,S,U,V> implements Command<Union<R,S>> {
		private final TestClass<R,S> testClass;
		private final U arg1;
		private final V arg2;
		private final BiFunction<U,V,R> rConstr;
		private final BiFunction<U,V,S> sConstr;

		public NewCommand2(TestClass<R,S> tc, U a, V b, BiFunction<U,V,R> rc, BiFunction<U,V,S> sc) {
			testClass = tc;
			arg1 = a;
			arg2 = b;
			rConstr = rc;
			sConstr = sc;
		}
		
		@Override
		public Result<Union<R, S>> execute(boolean asReference) {
			try {
				if (asReference) {
					return new ObjectResult<>(testClass,rConstr.apply(arg1,arg2));
				} else {
					return new ObjectResult<>(sConstr.apply(arg1,arg2),testClass);
				}
			} catch (Exception e) {
				return new ExceptionResult<>(e);
			}
		}

		@Override
		public String code(LiteralBuilder lb) {
			return "new " + testClass.getTypeName() + "(" + 
					lb.toString(arg1) + "," + lb.toString(arg2) + ")";
		}
	}

	public static class Command0<R,S,T> implements Command<T> {
		private final TestClass<R,S> testClass;
		private final int index;
		protected final Function<R,Result<T>> refFunc;
		protected final Function<S,Result<T>> sutFunc;
		protected final String methodName;
		
		public Command0(TestClass<R,S> tc, int i, Function<R,Result<T>> rf, Function<S,Result<T>> sf, String mn) {
			testClass = tc;
			index = i;
			refFunc = rf;
			sutFunc = sf;
			methodName = mn;
		}

		@Override
		public Result<T> execute(boolean asReference) {
			if (asReference) {
				return refFunc.apply(testClass.getRefObject(index));
			} else {
				return sutFunc.apply(testClass.getSUTObject(index));
			}
		}

		@Override
		public String code(LiteralBuilder lb) {
			return code(methodName, testClass.getIdentifier(index));
		}		
	}

	public static class Command1<R,S,T,U> implements Command<T> {
		private final TestClass<R,S> testClass;
		private final int index;
		private final U arg;
		protected final BiFunction<R,U,Result<T>> refFunc;
		protected final BiFunction<S,U,Result<T>> sutFunc;
		protected final String methodName;
		
		public Command1(TestClass<R,S> tc, int i, U a, BiFunction<R,U,Result<T>> rf, BiFunction<S,U,Result<T>> sf, String mn) {
			testClass = tc;
			index = i;
			arg = a;
			refFunc = rf;
			sutFunc = sf;
			methodName = mn;
		}

		@Override
		public Result<T> execute(boolean asReference) {
			if (asReference) {
				return refFunc.apply(testClass.getRefObject(index),arg);
			} else {
				return sutFunc.apply(testClass.getSUTObject(index),arg);
			}
		}

		@Override
		public String code(LiteralBuilder lb) {
			return code(methodName, testClass.getIdentifier(index), lb.toString(arg));
		}		
	}

	public static class Command2<R,S,T,U,V> implements Command<T> {
		private final TestClass<R,S> testClass;
		private final int index;
		private final U arg1;
		private final V arg2;
		protected final TriFunction<R,U,V,Result<T>> refFunc;
		protected final TriFunction<S,U,V,Result<T>> sutFunc;
		protected final String methodName;
		
		public Command2(TestClass<R,S> tc, int i, U a, V b, TriFunction<R,U,V,Result<T>> rf, TriFunction<S,U,V,Result<T>> sf, String mn) {
			testClass = tc;
			index = i;
			arg1 = a;
			arg2 = b;
			refFunc = rf;
			sutFunc = sf;
			methodName = mn;
		}

		@Override
		public Result<T> execute(boolean asReference) {
			if (asReference) {
				return refFunc.apply(testClass.getRefObject(index),arg1,arg2);
			} else {
				return sutFunc.apply(testClass.getSUTObject(index),arg1,arg2);
			}
		}

		@Override
		public String code(LiteralBuilder lb) {
			return code(methodName, testClass.getIdentifier(index), lb.toString(arg1), lb.toString(arg2));
		}		
	}

	public static class CommandM<R,S,T,U,V> implements Command<T> {
		protected final TestClass<R,S> recClass;
		protected final TestClass<U,V> argClass;
		protected final int recIndex;
		protected final int argIndex;
		protected final BiFunction<R,U,Result<T>> refFunc;
		protected final BiFunction<S,V,Result<T>> sutFunc;
		protected final String methodName;
		
		public CommandM(TestClass<R,S> rc, TestClass<U,V> ac, int i, int j, BiFunction<R,U,Result<T>> rf, BiFunction<S,V,Result<T>> sf, String mn) {
			recClass = rc;
			argClass = ac;
			recIndex = i;
			argIndex = j;
			refFunc = rf;
			sutFunc = sf;
			methodName = mn;
		}

		@Override
		public Result<T> execute(boolean asReference) {
			if (asReference) {
				return refFunc.apply(recClass.getRefObject(recIndex),argClass.getRefObject(argIndex));
			} else {
				return sutFunc.apply(recClass.getSUTObject(recIndex),argClass.getSUTObject(argIndex));
			}
		}

		@Override
		public String code(LiteralBuilder lb) {
			return code(methodName, recClass.getIdentifier(recIndex), argClass.getIdentifier(argIndex));
		}		
	}
	
	public static class CommandR<R,S,T> extends CommandM<R,S,T,R,S> {
		public CommandR(TestClass<R,S> tc, int i, int j, BiFunction<R,R,Result<T>> rf, BiFunction<S,S,Result<T>> sf, String mn) {
			super(tc,tc,i,j,rf,sf,mn);
		}
	}

	public static class Command3<R,S,T,U,V,W> implements Command<T> {
		private final TestClass<R,S> testClass;
		private final int index;
		private final U arg1;
		private final V arg2;
		private final W arg3;
		protected final Function4<R,U,V,W,Result<T>> refFunc;
		protected final Function4<S,U,V,W,Result<T>> sutFunc;
		protected final String methodName;
		
		public Command3(TestClass<R,S> tc, int i, U a, V b, W c, Function4<R,U,V,W,Result<T>> rf, Function4<S,U,V,W,Result<T>> sf, String mn) {
			testClass = tc;
			index = i;
			arg1 = a;
			arg2 = b;
			arg3 = c;
			refFunc = rf;
			sutFunc = sf;
			methodName = mn;
		}

		@Override
		public Result<T> execute(boolean asReference) {
			if (asReference) {
				return refFunc.apply(testClass.getRefObject(index),arg1,arg2,arg3);
			} else {
				return sutFunc.apply(testClass.getSUTObject(index),arg1,arg2,arg3);
			}
		}

		@Override
		public String code(LiteralBuilder lb) {
			return code(methodName, testClass.getIdentifier(index),
					lb.toString(arg1),
					lb.toString(arg2), 
					lb.toString(arg3));
		}		
	}

}
