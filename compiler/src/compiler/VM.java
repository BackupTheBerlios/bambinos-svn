package compiler;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.IOException;
import javax.swing.JTextArea;
import java.awt.GridLayout;
import javax.swing.JFrame;

public class VM {

	private static JTextArea registerDisplay;
	
	private static int[] registers = new int[32];
	private int stackSize = 1024;
	private int heapSize = 4096;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		// initialize register-array
		registers[0] = 0;
		
		if (args[0] == null) {
			System.out.println("Usage: please add a filename");
			System.exit(1);
		}
		
		initGUI();
		updateRegisterDisplay();
		
		String filename = new String(args[0]);
		String currentLine = new String();
		
		RandomAccessFile inputFile = openFile(filename);
		
		currentLine = readLine(inputFile);
		while (currentLine != null) {
			execute(currentLine);
			currentLine = readLine(inputFile);	

		}
				
	}
	
	private static RandomAccessFile openFile(String filename) {
		RandomAccessFile file = null;
		
		try {
			file = new RandomAccessFile(filename, "r");	
		} catch(FileNotFoundException fileNotFound) {
			System.out.println("File " + filename.toString() + " does not exist: " + fileNotFound.toString());
			System.exit(1);
		}
		
		return file;
		
	}
	
	private static String readLine(RandomAccessFile file) {
		
		String currentLine = new String();
		
		try {
			currentLine = file.readLine();
		} catch(IOException io) {
			System.out.println("Error reading line from file");
		}
		
		return currentLine;
	}
	
	
	private static void execute(String currentLine) {
		
		if (currentLine == "")  {
			return;
		}
		
		int parameterCount = 0;
		
		int targetRegister;
		int firstSourceRegister;
		int secondSourceRegister;
		int sourceValue;
		
		String command = new String();
		String[] currentLineElements = new String[2];
		String[] parameters = new String[3];
		currentLineElements = currentLine.split(" ");
		
		parameters = currentLineElements[1].split(",");
		command = currentLineElements[0];
		
		command = command.toUpperCase();
		
		Integer targetRegisterIntegerValue = new Integer(parameters[0]);
		Integer firstSourceRegisterIntegerValue = new Integer(parameters[1]);
		Integer secondSourceRegisterIntegerValue;
		Integer sourceValueIntegerValue;
		
		if (parameters.length > 2) {
			secondSourceRegisterIntegerValue = new Integer(parameters[2]);
			sourceValueIntegerValue = new Integer(parameters[2]);	
		} else {
			secondSourceRegisterIntegerValue = new Integer(0);
			sourceValueIntegerValue = new Integer(0);	
		}
		
		
		targetRegister = targetRegisterIntegerValue.intValue();
		firstSourceRegister = firstSourceRegisterIntegerValue.intValue();
		secondSourceRegister = secondSourceRegisterIntegerValue.intValue();
		sourceValue = sourceValueIntegerValue.intValue(); 
		
		
		if (command.matches("ADD")) {
			executeADD(targetRegister, firstSourceRegister, secondSourceRegister);
		}
		else if(command.matches("ADDI")) {
			executeADDI(targetRegister, firstSourceRegister, sourceValue);
		}
		if (command.matches("SUB")) {
			executeSUB(targetRegister, firstSourceRegister, secondSourceRegister);
		}
		else if(command.matches("SUBI")) {
			executeSUBI(targetRegister, firstSourceRegister, sourceValue);
		}
		else if(command.matches("MUL")) {
			executeMUL(targetRegister, firstSourceRegister, secondSourceRegister);
		}
		else if(command.matches("MULI")) {
			executeMULI(targetRegister, firstSourceRegister, sourceValue);
		}
		if (command.matches("DIV")) {
			executeDIV(targetRegister, firstSourceRegister, secondSourceRegister);
		}
		else if(command.matches("DIVI")) {
			executeDIVI(targetRegister, firstSourceRegister, sourceValue);
		}
		if (command.matches("MOD")) {
			executeMOD(targetRegister, firstSourceRegister, secondSourceRegister);
		}
		else if(command.matches("MODI")) {
			executeMODI(targetRegister, firstSourceRegister, sourceValue);
		}
		if (command.matches("CMP")) {
			executeCMP(targetRegister, firstSourceRegister, secondSourceRegister);
		}
		else if(command.matches("CMPI")) {
			executeCMPI(targetRegister, firstSourceRegister, sourceValue);
		}
		if (command.matches("CHK")) {
			executeCHK(targetRegister, firstSourceRegister);
		}
		else if(command.matches("CHKI")) {
			executeCHKI(targetRegister, firstSourceRegister);
		}
		else if(command.matches("AND")) {
			executeAND(targetRegister, firstSourceRegister, secondSourceRegister);
		}
		else if(command.matches("ANDI")) {
			executeANDI(targetRegister, firstSourceRegister, sourceValue);
		}
		else if(command.matches("BIC")) {
			executeBIC(targetRegister, firstSourceRegister, secondSourceRegister);
		}
		else if(command.matches("BICI")) {
			executeBICI(targetRegister, firstSourceRegister, sourceValue);
		}
		else if(command.matches("OR")) {
			executeOR(targetRegister, firstSourceRegister, secondSourceRegister);
		}
		else if(command.matches("ORI")) {
			executeORI(targetRegister, firstSourceRegister, sourceValue);
		}
		else if(command.matches("XOR")) {
			executeXOR(targetRegister, firstSourceRegister, secondSourceRegister);
		}
		else if(command.matches("XORI")) {
			executeXORI(targetRegister, firstSourceRegister, sourceValue);
		}
		else if(command.matches("LSH")) {
			executeLSH(targetRegister, firstSourceRegister, secondSourceRegister);
		}
		else if(command.matches("LSHI")) {
			executeLSHI(targetRegister, firstSourceRegister, sourceValue);
		}
		else if(command.matches("ASH")) {
			executeASH(targetRegister, firstSourceRegister, secondSourceRegister);
		}
		else if(command.matches("ASHI")) {
			executeASHI(targetRegister, firstSourceRegister, sourceValue);
		}
		
		updateRegisterDisplay();
	}

	
	private static void executeADD(int r0, int r1, int r2) {
		registers[r0] = registers[r1] + registers[r2];
		
		System.out.println("ADD: " + registers[r0]);
	}
	
	private static void executeADDI(int r0, int r1, int r2) {
		registers[r0] = registers[r1] + r2;
		
		System.out.println("ADDI: " + registers[r0]);
	}
	
	private static void executeSUB(int r0, int r1, int r2) {
		registers[r0] = registers[r1] - registers[r2];
		
		System.out.println("SUB: " + registers[r0]);
	}
	
	private static void executeSUBI(int r0, int r1, int r2) {
		registers[r0] = registers[r1] - r2;
		
		System.out.println("SUBI: " + registers[r0]);
	}
	
	private static void executeMUL(int r0, int r1, int r2) {
		registers[r0] = registers[r1] * registers[r2];
		
		System.out.println("MUL: " + registers[r0]);
	}
	
	private static void executeMULI(int r0, int r1, int r2) {
		registers[r0] = registers[r1] * r2;
		
		System.out.println("MULI: " + registers[r0]);
	}
	
	private static void executeDIV(int r0, int r1, int r2) {
		if (registers[r2] == 0) {
			System.out.println("Division by 0");
			return;
		}
		
		registers[r0] = registers[r1] / registers[r2];
		
		System.out.println("DIV: " + registers[r0]);
	}
	
	private static void executeDIVI(int r0, int r1, int r2) {
		if (r2 == 0) {
			System.out.println("Division by 0");
			return;
		}
		
		
		registers[r0] = registers[r1] / r2;
		
		System.out.println("DIVI: " + registers[r0]);
	}

	private static void executeMOD(int r0, int r1, int r2) {
		
		registers[r0] = registers[r1] % registers[r2];
		
		System.out.println("MOD: " + registers[r0]);
	}
	
	private static void executeMODI(int r0, int r1, int r2) {
		
		registers[r0] = registers[r1] % r2;
		
		System.out.println("MODI: " + registers[r0]);
	}
	
	private static void executeCMP(int r0, int r1, int r2) {
		
		registers[r0] = registers[r1] - registers[r2];
		
		System.out.println("CMP: " + registers[r0]);
	}
	
	private static void executeCMPI(int r0, int r1, int r2) {
		
		registers[r0] = registers[r1] - r2;
		
		System.out.println("CMPI: " + registers[r0]);
	}
	
	private static void executeCHK(int r0, int r1) {
		
		if ((registers[r0] < 0) || (registers[r0] > registers[r1])) {
			System.out.println("ERROR: CHK: out of range");
		}
		
		System.out.println("CHK: ");
	}
	
	private static void executeCHKI(int r0, int r1) {
		
		if ((registers[r0] < 0) || (registers[r0] > r1)) {
			System.out.println("ERROR: CHKI: out of range");
		}
		
		System.out.println("CHKI: ");
	}
	
	private static void executeAND(int r0, int r1, int r2) {
		
		registers[r0] = registers[r1] & registers[r2];
		
		System.out.println("AND: " + registers[r0]);
	}
	
	private static void executeANDI(int r0, int r1, int r2) {
		
		registers[r0] = registers[r1] & r2;
		
		System.out.println("ANDI: " + registers[r0]);
		
	}
	private static void executeOR(int r0, int r1, int r2) {
		
		registers[r0] = registers[r1] | registers[r2];
		
		System.out.println("OR: " + registers[r0]);
	}
	
	private static void executeORI(int r0, int r1, int r2) {
		
		registers[r0] = registers[r1] | r2;
		
		System.out.println("ORI: " + registers[r0]);
	}
	private static void executeXOR(int r0, int r1, int r2) {
		
		registers[r0] = registers[r1] ^ registers[r2];
		
		System.out.println("XOR: " + registers[r0]);
	}
	
	private static void executeXORI(int r0, int r1, int r2) {
		
		registers[r0] = registers[r1] ^ r2;
		
		System.out.println("XORI: " + registers[r0]);
	}
	private static void executeBIC(int r0, int r1, int r2) {
		
		registers[r0] = registers[r1] & (~registers[r2]);
		
		System.out.println("BIC: " + registers[r0]);
	}
	
	private static void executeBICI(int r0, int r1, int r2) {
		
		registers[r0] = registers[r1] & (~r2);
		
		System.out.println("BICI: " + registers[r0]);
	}	
	private static void executeLSH(int r0, int r1, int r2) {

		if (r2 > 0) {
			registers[r0] = registers[r1] >>> (registers[r2]);	
		} else {
			registers[r0] = registers[r1] >> (registers[r2]);
		}
		
		System.out.println("LSH: " + registers[r0]);
	}
	
	private static void executeLSHI(int r0, int r1, int r2) {
		
		if (r2 > 0) {
			registers[r0] = registers[r1] >>> (r2);	
		} else {
			registers[r0] = registers[r1] >> (r2);
		}
		
		System.out.println("LSHI: " + registers[r0]);
	}	
	
	private static void executeASH(int r0, int r1, int r2) {

		if (r2 > 0) {
			registers[r0] = registers[r1] >> (registers[r2]);	
		}
		
		System.out.println("ASH: " + registers[r0]);
	}
	
	private static void executeASHI(int r0, int r1, int r2) {
		
		if (r2 > 0) {
			registers[r0] = registers[r1] >> (r2);	
		}
		
		System.out.println("ASHI: " + registers[r0]);
	}
	
	
	private static void initGUI() {
	
		JFrame frame = new JFrame("overview memory");
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	    
	    registerDisplay = new JTextArea();
	    registerDisplay.setEditable(false);
	    
        GridLayout layout = new GridLayout(0,2);
        
        frame.setLayout(layout);
        
        frame.add(registerDisplay);
        
        frame.setSize(300, 550);
        frame.setVisible(true);
        
 	}
	
	private static void updateRegisterDisplay() {
		
		registerDisplay.setText("");
		String registerLabel = new String();
		
		for (int i=0; i<registers.length; i++) {
			
			if (i < 10) {
				registerLabel = "0" + i;
			} else {
				registerLabel = "" + i;
			}
			
			registerDisplay.append("R" + registerLabel + ": " + registers[i] + "\n");
		}
	}
	
}
