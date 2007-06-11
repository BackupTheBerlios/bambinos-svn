package compiler;

public class Util {

	private static boolean debugging = true; // default

	// print debug messages
	public static void debug(String msg) {
		if (debugging == true)
			System.out.println(msg);
	}

	public static void setDebugging(boolean debugging) {
		Util.debugging = debugging;
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
