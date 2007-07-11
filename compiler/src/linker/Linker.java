package linker;

import java.io.RandomAccessFile;
import java.util.Hashtable;


public class Linker {

	
	private static Hashtable<String,Integer> moduleList;
	private static RandomAccessFile binaryFile;
	
	public static void main(String[] args) {
		
		String filename = new String();
		String binaryFilename = new String();
		
		
		if (args.length != 1) {
			System.out.println("Usage: java linker <objectfile>");
			System.exit(1);
		} else {
			filename = args[0];
		}
		
		ObjectFile mainModule = new ObjectFile(filename, "r");
		BinaryFile binary = new BinaryFile(mainModule);
		
		
		binary.fixAddressing();
		binary.export();
		
		
	}
	
	
}
