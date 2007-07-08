package linker;


public class Linker {

	
	public static void main(String[] args) {
		
		String filename = new String();
		
		if (args.length != 1) {
			System.out.println("Usage: java linker <objectfile>");
			System.exit(1);
		} else {
			filename = args[0];
		}
		
		ObjectFile objectFile = new ObjectFile(filename, "r");
		
		
	}
	
	
}
