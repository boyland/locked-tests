package edu.uwm.cs.junit;

import java.awt.HeadlessException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class LockedTestCase extends TestCase {
	
	private static class Info {
		final File testFile;
		final Map<Integer,Object> keys;
		final Map<String,String> replacements;
	
		Info(File f) {
			testFile = f;
			keys = new HashMap<Integer,Object>();
			replacements = new HashMap<String,String>();
			read();
		}
		
		public void read() {
			try {
				if (testFile.canRead()) {
					BufferedReader br = new BufferedReader(new FileReader(testFile));
					String in;
					while ((in = br.readLine()) != null) {
						int eqi = in.indexOf('=');
						if (eqi == -1) {
							System.err.println("test corrupted (1): " + in);
						} else {
							try {
								int key = Integer.parseInt(in.substring(0,eqi));
								Object val = Util.parseObject(in.substring(eqi+1));
								if (Util.checkHash(key, val)) {
									put("T", key, val);
								} else {
									System.err.println("test corrupted (2): " + in);
								}
							} catch (NumberFormatException e) {
								System.err.println("test corrupted (3): " + in);
							} catch (ParseException e) {
								System.err.println("test corrupted (4): " + in);
							}
						}
					}
					br.close();
				} else {
					write();
				}
			} catch (FileNotFoundException e) {
				System.err.println("Cannot locate/write test key file " + e);
				System.err.println("Testing aborted.");
				System.exit(100);
			} catch (IOException e) {
				System.err.println("Error while reading/writing test key file: " + e);
				System.err.println("Testing aborted.");
				System.exit(100);
			}
		}
		
		/**
		 * @param key
		 * @param val
		 */
		void put(String target, int key, Object val) {
			putReplace(target,key,val);
			keys.put(key, val);
		}

		void putReplace(String target, int key, Object val) {
			replacements.put(target + "(" + key + ")", Util.toString(val));
		}
		
		public void write() throws IOException {
			PrintWriter pw = new PrintWriter(new FileWriter(testFile));
			for (Map.Entry<Integer, Object> e : keys.entrySet()) {
				int key = e.getKey();
				Object value = e.getValue();
				if (Util.checkHash(key, value)) {
					pw.println(key + "=" + Util.toString(value));
				} else {
					pw.close();
					throw new IOException("internal test cases corrupted.");
				}
			}
			pw.close();
		}
    
    @Override
    public String toString() {
      return "Info(" + testFile + ") with " + keys.size() + " keys";
    }
  }

	private static Map<String,Info> allLockedTestInfo = new HashMap<String,Info>();
	
	private static Info getLockedTestInfo(String filename) {
		Info result = allLockedTestInfo.get(filename);
		if (result == null) {
			result = new Info(new File(filename));
			allLockedTestInfo.put(filename, result);
		}
		return result;
	}
	private Info lockedTestInfo;
	
	protected LockedTestCase() {
		String className = this.getClass().getCanonicalName();
		lockedTestInfo = getLockedTestInfo(className + ".tst");
	}

	/**
	 * @param key
	 * @param val
	 */
	private void addKey(String target, int key, Object val) {
		lockedTestInfo.put(target, key, val);
	}

	private void writeTestFile() throws IOException {
		lockedTestInfo.write();
	}

	protected Object T(int key, String type, String target) {
		Object result = Util.ERROR_OBJECT;
		Integer okey = new Integer(key);
		if (lockedTestInfo.keys.containsKey(okey)) {
			result = lockedTestInfo.keys.get(okey);
			lockedTestInfo.putReplace(target, key, result);
		} else {
			result = askUser(key,type,target);
			if (result != Util.ERROR_OBJECT && Util.checkHash(key, result)) {
				addKey(target, key, result);
				try {
					writeTestFile();
				} catch (IOException e) {
					System.err.println("Warning: test key file writing crashed; Test cases may be locked again.");
				}
			}
		}
		if (result == Util.ERROR_OBJECT) {
			assertFalse("test locked", true);
		}
		if (type != null && !type.equals(result.getClass().getName()) && !type.equals(result.getClass().getSimpleName())) {
			assertFalse("test unlocked incorrectly with wrong type",true);
		}
		return result;
	}
	
	protected Object T(int key) {
		return T(key,null,"T");
	}
	
	protected boolean Tb(int key) {
		Boolean b = (Boolean)T(key,"Boolean","Tb");
		return b.booleanValue();
	}
	
	protected int Ti(int key) {
		Integer i = (Integer)T(key,"Integer","Ti");
		return i.intValue();
	}
	
	protected char Tc(int key) {
		Character c = (Character)T(key,"Character","Tc");
		return c.charValue();
	}
	
	protected String Ts(int key) {
		return (String)T(key,"String","Ts");
	}
	
	protected float Tf(int key) {
	  Float f = (Float)T(key,"Float","Tf");
	  return f.floatValue();
	}
	
	private static BufferedReader input = null;
	
	private Object askUser(int key, String type, String target) {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		int n = stack.length;
		int i = 1;
		for (; i < n; ++i) {
			// System.out.println(stack[i]);
			if (stack[i].getClassName().equals(LockedTestCase.class.getCanonicalName())) continue;
			if (stack[i].getMethodName().equals(target)) continue;
			break;
		}
		int lno = stack[i].getLineNumber();
		// System.out.println(stack[i].getFileName()+":" + lno);
		String className = stack[i].getClassName();
    return askUser(lockedTestInfo, className, lno, key, type, target);
	}

  /**
   * Ask the user to resolve the test at a particular
   * line of the particular test case.
   * The line should include the text: target + "(" + key + ")".
   * @param info TODO
   * @param className
   * @param lno
   * @param key
   * @param type Java type of the 
   * @param target lock text (e.g. "Ti")
   * @return
   */
  private static Object askUser(Info info, String className, int lno, int key,
      String type, String target) {
    String[] contents = Util.readSourceFile(className);
		if (contents.length <= lno) {
			System.err.println("Can't find test case asking for unlocking.");
			return Util.ERROR_OBJECT;
		}
		int l;
		for (l=lno-1; l >= 1 && contents[l].indexOf("void test") < 0; --l) {
			String line = contents[l];
			for (Map.Entry<String,String> e : info.replacements.entrySet()) {
				line = line.replace(e.getKey(),e.getValue());
			}
			contents[l] = line;
		}
		contents[lno] = contents[lno].replace(target+"("+key+")", "???");
		String[] snippet = new String[lno-l+1];
		for (int k=l; k <= lno; ++k) {
			snippet[k-l] = contents[k];
		}
		try {
			return TestCaseUnlockDialog.show(snippet, type, key);
		} catch (HeadlessException ex) {
			// fall though
		}
		try {
			System.out.println("In the following locked testcase:\n");
			for (int j=l; j <= lno; ++j) {
				System.out.println(contents[j]);
			}
			System.out.println("\nWhat should go in place of ??? on the line? (Type a value and press return)");
			if (input == null) {
				input = new BufferedReader(new InputStreamReader(System.in));
			}
			for (;;) {
				if (type != null) System.out.println("We are expecting a value of type " + type);
				try {
					String response = input.readLine();
					if (response.equals("") || response.equals("quit")) return Util.ERROR_OBJECT;
					Object result = Util.parseObject(response);
					if (Util.checkHash(key,result)) {
						System.out.println("Yes, that's right.  The test is now unlocked.");
						return result;
					}
					System.out.println("No.  That value can't be right.  Try again.");
				} catch (RuntimeException e) {
					System.out.println("I don't understand your response.  Make sure to use quotes for strings.  Try again.");
				}
			}
		} catch (IOException ex) {
			System.err.println("A serious error occurred.");
		}
		return Util.ERROR_OBJECT;
  }
  
  /**
   * Find all locked tests in the given class name (a locked JUnit test).
   * @param classname name of the class including locked tests.
   */
  public static void unlockAll(String className) {
	unlockAll(className,className);
  }
  public static void unlockAll(String infoName, String className) {
	Info info = getLockedTestInfo(infoName+".tst");
    String[] contents = Util.readSourceFile(className);
    // System.out.println("contents = " + Arrays.toString(contents));
    if (contents.length <= 1) return;
    /* System.out.println("Info = " + info);
    for (Map.Entry<String,String> e : info.replacements.entrySet()) {
      System.out.println(e.getKey() + " = " + e.getValue());
    }*/
    Pattern pattern = Pattern.compile("(T[a-zA-z]*)\\(([0-9]+)\\)");
    try {
      for (int i=1; i < contents.length; ++i) {
        Matcher m = pattern.matcher(contents[i]);
        while (m.find()) {
          String target = m.group(1);
          String keystring = m.group(2);
          if (target.length() > 2) continue;
          int key = Integer.parseInt(keystring);
          String type = null;
          switch (target) {
          default: break;
          case "Ti": type = "Integer"; break;
          case "Ts": type = "String"; break;
          case "Tb": type = "Boolean"; break;
          case "Tc": type = "Character"; break;
          case "Tf": type = "Float"; break;
          }
          if (info.keys.containsKey(key)) {
            info.putReplace(target, key, info.keys.get(key));
          } else if (!info.keys.containsKey(key)) {
            Object result = askUser(info,className,i,key,type,target);
            if (result != Util.ERROR_OBJECT && Util.checkHash(key, result)) {
              info.put(target, key, result);
            } else {
              return;
            }
          }
        }
      }
    } finally {
      try {
        info.write();
      } catch (IOException e) {
        System.err.println("Warning: test key file writing crashed; Test cases may be locked again.");
      }
    }
  }
}
