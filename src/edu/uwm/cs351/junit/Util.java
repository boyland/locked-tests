package edu.uwm.cs351.junit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Util {
	public static final Object ERROR_OBJECT = new Object();
	
	public static String escape(char ch) {
		switch (ch) {
		case '\\': return "\\\\";
		case '\n': return "\\n";
		case '\r': return "\\r";
		case '\f': return "\\f";
		case '\t': return "\\t";
		case '\b': return "\\b";
		case '"': return "\\\"";
		case '\'': return "\\'";
		default: break;
		}
		if (ch < 16) return "\\u000" + Integer.toHexString(ch);
		if (ch < ' ') return "\\u00" + Integer.toHexString(ch);
		if (ch < 127) return ""+ch;
		if (ch < 256) return "\\u00" + Integer.toHexString(ch);
		if (ch < 4096) return "\\u0" + Integer.toHexString(ch);
		return "\\u" + Integer.toHexString(ch);
	}
	
	public static String unescape(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < s.length(); ++i) {
			switch (s.charAt(i)) {
			case '\\':
				if (++i == s.length()) throw new ParseException("bad string: '" + s + "'");
				switch (s.charAt(i)) {
				case 'n': sb.append('\n'); break;
				case 'r': sb.append('\r'); break;
				case 't': sb.append('\t'); break;
				case 'b': sb.append('\b'); break;
				case 'f': sb.append('\f'); break;
				case 'u':
					if (i+4 >= s.length()) throw new ParseException("bad string: '" + s + "'");
					char ch = (char)Integer.parseInt(s.substring(i+1, i+5),16);
					i += 4;
					sb.append(ch);
					break;
				default:
					sb.append(s.charAt(i));
					break;
				}
				break;
			default:
				sb.append(s.charAt(i));
				break;
			}
		}
		return sb.toString();
	}
	
	private static Set<String> fromStringClasses = new HashSet<String>();
	
	private static Object fromString(Class<?> clazz, String s) {
		try {
			Method m = clazz.getMethod("fromString", String.class);
			if (m == null) {
				throw new ParseException("Class " + clazz + " has no static String method called 'fromString'");
			}
			Object result = m.invoke(null, s);
			if (result != null) {
				fromStringClasses.add(clazz.getCanonicalName());
			}
			if (result == null) {
				throw new ParseException(clazz + ".fromString(String) returned null!");
			}
			return result;
		} catch (ParseException e) {
			throw e;
		} catch (NumberFormatException e) {
			throw e;
		} catch (InvocationTargetException e) {
			Throwable t = e.getCause();
			if (t instanceof RuntimeException) throw (RuntimeException)t;
			throw new ParseException("Error while trying to run " + clazz + ".fromString("+s+")");
		} catch (Exception e) {
			e.printStackTrace();
			throw new ParseException("Error while trying to run " + clazz + ".fromString(String): " + e.getMessage());
		}
	}
	
	public static Object fromString(String className, String s) {
		try {
			return fromString(Class.forName(className),s);
		} catch (ClassNotFoundException e) {
			throw new ParseException("Unknown class: " + className);
		}
	}
	
	public static List<String> getFromStringClasses() {
		List<String> result = new ArrayList<String>(fromStringClasses);
		Collections.sort(result);
		return result;
	}
	
	public static String toString(Object o) {
		if (o == null) return "null";
		if (o instanceof Integer || o instanceof Boolean) return o.toString();
		if (o instanceof Character) {
			return "'" + escape((Character)o) + "'";
		}
		if (o instanceof String) {
			String s = (String)o;
			StringBuilder sb = new StringBuilder();
			sb.append('"');
			for (int i=0; i < s.length(); ++i) {
				sb.append(escape(s.charAt(i)));
			}
			sb.append('"');
			return sb.toString();
		}
		Class<?> clazz = o.getClass();
		String result = o.toString();
		Object check = fromString(clazz,result);
		if (check != null && o.equals(check)) {
			return "?" + clazz.getCanonicalName()+" "+result;
		}
		throw new IllegalArgumentException("can't stringify " + o);
	}
	
	public static Object parseObject(String s) {
		if (s == null || s.length() < 1) throw new ParseException("cannot parse '" + s + "'");
		switch (s.charAt(0)) {
		case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
			return Integer.parseInt(s);
		case '-':
			return -Integer.parseInt(s.substring(1));
		case '"':
			if (s.length() < 2 || s.charAt(s.length()-1) != '"') throw new ParseException("cannot parse '" + s + "'");
			return unescape(s.substring(1, s.length()-1));
		case '\'':
			if (s.length() < 2 || s.charAt(s.length()-1) != '\'') throw new ParseException("cannot parse '" + s + "'");
			String u = unescape(s.substring(1, s.length()-1));
			if (u.length() != 1) throw new ParseException("cannot parse '" + s + "'");
			return new Character(u.charAt(0));
		case '?':
			int ind = s.indexOf(' ');
			if (ind < 0) throw new ParseException("cannot parse '" + s + "'");
			Object result = fromString(s.substring(1, ind),s.substring(ind+1));
			if (result != null) return result;
		case 'n':
			if (s.equals("null")) return null;
		case 't':
			if (s.equals("true")) return Boolean.TRUE;
		case 'f':
			if (s.equals("false")) return Boolean.FALSE;
		default:
			throw new ParseException("cannot parse '" + s + "'");
		}
	}

	private static Random random = new Random();
	
	public static int randomSalt() {
		return random.nextInt(32768);
	}
	
	public static int getSalt(int key) {
		return key>>16;
	}
	
	public static int hash(Object o) {
		return hash(o,randomSalt());
	}
	
	public static int hash(Object o, int salt) {
		int h = (""+o+salt).hashCode();
		return (salt << 16) | (65535&((h >> 16)^h));
	}
	
	public static boolean checkHash(int key, Object value) {
		return key == hash(value,getSalt(key));
	}
	
	public static String[] readSourceFile(String className) {
		String filename = className.replace('.', File.separatorChar);
		File source = new File("src" + File.separator + filename + ".java");
		List<String> contents = new ArrayList<String>();
		contents.add("Contents of " + filename); // line 0!
		try {
			BufferedReader br = new BufferedReader(new FileReader(source));
			String in;
			while ((in = br.readLine()) != null) {
				contents.add(in);
			}
			br.close();
		} catch (FileNotFoundException e) {
			System.err.println("Error: " + e);
			// muffle
		} catch (IOException e) {
			System.err.println("Error: " + e);
			// muffle
		}
		return contents.toArray(new String[contents.size()]);
	}
	
	public static void main(String[] args) throws IOException {
		for (String s : args) {
			Object o = parseObject(s);
			System.out.println("hash for " + s + " = " + hash(o));
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String s;
		while ((s = br.readLine()) != null) {
			if (s.equals("quit") || s.equals("q")) return;
			if (s.equals("exit")) return;
			if (s.equals("?") || s.startsWith("h")) {
				System.out.println("Enter a test result value to get a hashcode to use for it.");
				System.out.println("Examples: true \"hello\\n\" -777 'x'");
				System.out.println("But only one value per line.");
				System.out.println("If a class in the classpath has a static method 'fromString' that accepts a String,");
				System.out.println("then one can write ?fully.qualified.class.name literalstring");
				System.out.println("For example:");
				System.out.println("?edu.uwm.cs351.Rational 3/5");
				continue;
			}
			Object o;
			try {
				o = parseObject(s);
				System.out.println("hash for " + s + " = " + hash(o));
			} catch (ParseException e) {
				System.err.println("Parse error: " + e.getMessage());
			} catch (NumberFormatException e) {
				System.err.println("Format error: " + e.getMessage());
			} catch (RuntimeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		br.close();
	}
}
