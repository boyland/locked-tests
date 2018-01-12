# locked-tests
Locked JUnit test cases for student projects.  Inspired by DeNero and others: Problems Before Solutions (2015)

This project provides a JUnit test case wrapper (abstract class) that enables an instructor to create a 
test suite that does not divulge the values expected by the routines.  Students must figure out what the
expected value is and "unlock" the test.  The student's input is checked against a hash of the expected
value.  The test suite includes the hashes, but not the values.  Once a test is unlocked, the expected
value is stored in a text file stored in the Java project so that it will be available the next time that
the tests are used.

The hashes use 15 bits of random salt added to the value to get 16 bits of a hash.  The 31 bits are then 
rendered as a positive integer value.  There is no attempt to have strong encryption; the intended use is
for instruction, not security or privacy.

The unlocking interface is run on demand.  If a GUI is available, a dialog is put up to request the
student enter a value, otherwise, it is done on the command line.  In either case, the test method being
run is shown to the user with unlocked tests revealed to their actual value.  The GUI interface provides
more help on input; the command-line interface uses a single way to read any run-time value.

The following test case results can be encoded:
- Primitive types: int, double, float, char and boolean
- Predefined types: String
- User-defined types: any class with a public static "fromString" method taking a string.

The following classes in this project are intended to be used by clients (instructors):
- edu.uwm.cs351.junit.LockedTestCase
   (replacement for junit.framework.TestCase from Junit 3)
   The test suite may use methods T(...), Ts(...), Ti(...), Tb(...), Tc(...) Td(...)
   to access locked test values given their hashes.
- edu.uwm.cs351.junit.Util
   (main program that runs on the console/command-line for computing hashes)
   
See edu.uwm.cs351.TestRational for a use of LockedTestCase.
