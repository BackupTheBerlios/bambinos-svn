package compiler;

public class Util {

	private static boolean debugging=true; // default

	// print debug messages
	public static void debug(String msg) {
		if (debugging == true)
			System.out.println(msg);
	}

	public static void setDebugging(boolean debugging) {
		Util.debugging = debugging;
	}

}
