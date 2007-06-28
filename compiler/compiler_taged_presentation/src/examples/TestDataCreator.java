package examples;

import java.io.RandomAccessFile;
import java.io.IOException;

public class TestDataCreator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			RandomAccessFile output = new RandomAccessFile("/media/shared/Uni/compilerbau/comPiler/dev/compiler/src/examples/integer_outputfile1.txt", "rw");
			output.seek(0);
			output.writeInt(2097153);
			output.writeInt(8388614);
			output.writeInt(8388615);
			
			//00000100100000000000000000000110
			//00000000100000000000000000000110
			//00000000010000000000000000000110
			
			//001000
			//11001100000000000000000000000010
			//11010000000000000000000000000010
			output.close();
		} catch(IOException io) {
			
		}
		
		
	}

}
