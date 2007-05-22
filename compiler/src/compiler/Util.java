package compiler;

public class Util {

	private static boolean debugging;

	public static void debug(String msg) {
		if (debugging == true)
			System.out.println(msg);
	}

	public static void setDebugging(boolean debugging) {
		Util.debugging = debugging;
	}

}
