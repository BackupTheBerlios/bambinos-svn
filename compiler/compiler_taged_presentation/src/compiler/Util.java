package compiler;

public class Util {

	// Print parser debug messages
	private static boolean debug1 = false; // default
	//private static boolean debugging1 = true; // default

	//print opCode and Code Generation Stuff
	private static boolean debug2 = false; // default
	//private static boolean debugging1 = true; // default
	
	
	//Supress important messages should be true !!
	private static boolean debug3 = false; // default
	//private static boolean debugging1 = true; // default

	
	
	// print debug messages Parser - Scanner tokens
	public static void debug1(String msg) {
		if (debug1 == true)
			System.out.println(msg);
	}
	
	public static void debug2(String msg) {
		if (debug2 == true)
			System.out.println(msg);
	}
	
	public static void debug3(String msg) {
		if (debug3 == true)
			System.out.println(msg);
	}



	public static void setDebugging(boolean debugging) {
		Util.debug1 = debugging;
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
	
	public static class TypeErrorException extends Exception {

		public TypeErrorException() {
			// TODO Auto-generated constructor stub
		}

		public TypeErrorException(String message) {
			super(message);
			// TODO Auto-generated constructor stub
		}

		public TypeErrorException(Throwable cause) {
			super(cause);
			// TODO Auto-generated constructor stub
		}

		public TypeErrorException(String message, Throwable cause) {
			super(message, cause);
			// TODO Auto-generated constructor stub
		}

	}
}
