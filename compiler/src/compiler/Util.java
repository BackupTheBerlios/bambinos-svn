package compiler;

public class Util {

	private static boolean debugging1 = true; // default
	//private static boolean debugging1 = true; // default

	// print debug messages Parser - Scanner tokens
	public static void debug1(String msg) {
		if (debugging1 == true)
			System.out.println(msg);
	}

	public static void setDebugging(boolean debugging) {
		Util.debugging1 = debugging;
	}

	
	
	/* 
	 * Exceptions
	 */
	
	public static class IllegalTokenException extends Exception {

		public IllegalTokenException() {
			// TODO Auto-generated constructor stub
		}

		public IllegalTokenException(String message) {
			super(message);
			// TODO Auto-generated constructor stub
		}

		public IllegalTokenException(Throwable cause) {
			super(cause);
			// TODO Auto-generated constructor stub
		}

		public IllegalTokenException(String message, Throwable cause) {
			super(message, cause);
			// TODO Auto-generated constructor stub
		}

	}
}
