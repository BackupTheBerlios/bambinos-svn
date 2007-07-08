package compiler;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.EOFException;
import javax.swing.JTextArea;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


class ContinueButtonListener implements ActionListener
{
	private static boolean eventFired = false;
	
	public void waitForEvent() {
		
		while (eventFired == false) {

		}
		
		eventFired = false;
		
	}
	
    public void actionPerformed( ActionEvent e )
    {
        eventFired = true;
    }
}

public class VM {


	private static JTextArea registerDisplay;
	private static JTextArea instructionDisplay;
	private static JTextArea debugDisplay;
	private static JTextArea outputDisplay;
	
	private static ActionEvent actionEvent;
	private static ContinueButtonListener continueListener = new ContinueButtonListener();
	
	private static Integer[] registers = new Integer[32];
	public static Integer PC = new Integer(0);
	public static Integer IR = new Integer(0);
	// current Instruction Position
	public static Integer CIP = new Integer(0);
	private static Integer[] memory = new Integer[4096];
	private static Integer[] instructions;
	
	
	private static int highestInstructionAddress = 0;
	
	
	private static boolean debug = false;
	
	// format 1 instructions
	private static final int ADDI=0, SUBI=1, MULI=2, DIVI=3,MODI=4, CMPI=5, 
	CHKI=6, ANDI=7, BICI=8, ORI=9, XORI=10, LSHI=11, ASHI=12, LDW=13, 
	LDB=14, POP=15, STW=16, STB=17, PSH=18, BEQ=19, BNE=20, BLT=21, 
	BGE=22, BGT=23, BLE=24, HIGHEST_FORMAT_1=29;
	
	// format 2 instructions
	private static final int ADD=30,  SUB=31,  MUL=32,  DIV=33, MOD=34,  CMP = 35,  CHK=36,  
	AND=37,  BIC=38, OR=39,  XOR=40,  LSH=41,  ASH=42, PRNI=43, PRNC=44, PRNB=45, HIGHEST_FORMAT_2=49;
	
	
	// format 3 instructions
	private static final int BSR=50, RET=51, HIGHEST_FORMAT_3=63;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
			
		//execute();
		
		// initialize register-array
		registers[0] = 0;
		
		String filename = new String();
		
		if (args.length > 0) {
			filename = new String(args[0]);
			
			if (args.length > 1) {
						
				if (args[1].equals("--debug")) {
					System.out.println(args[1]);
					debug = true;
				}
				
			}
			
		} else {
			System.out.println("Usage: please add a filename");
			System.exit(1);
		}
		
		if (args[0] == null) {
			
		}
		
		
		
		initGui();
		
		if (debug) {
			initDebugGUI();
			updateRegisterDisplay();	
		}
		
		
		
		RandomAccessFile inputFile = openFile(filename);
		initCodeMemory(inputFile);
		loadCodeIntoMemory(inputFile);
		
		execute();
					
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
	
	/**
	 * Determines the filesize and initializes a memory-array accordingly
	 * The memory-size is filesize in bytes / 4. 
	 * @param file
	 */
	private static void initCodeMemory(RandomAccessFile file) {
		
		try {
			long fileSizeInBytes = file.length();
			
			if (debug) {
				debugDisplay.append("Filesize in Bytes: " + fileSizeInBytes + "\n");
			}
			
			int fileSizeInIntegers = (int)(fileSizeInBytes / 4);
			if ((fileSizeInBytes % 4) != 0) {
				fileSizeInIntegers++;
			}
			
			// we increment the memory size because we store the first instruction at position 1 and not 0.
			fileSizeInIntegers++;
			
			instructions =  new Integer[fileSizeInIntegers];
			
			if (debug) {
				debugDisplay.append("Memorysize in words: " + fileSizeInIntegers + "\n");
			}
			
		} catch(IOException io) {
			System.out.println("Cannot read sourcefile");
			System.exit(1);
		}
		
	}
	
	/**
	 * Loads all instructions from the sourcefile into the instruction-memory. The IR is set to the first instruction and the PC to
	 * the address of the next instruction
	 * @param file
	 */
	private static void loadCodeIntoMemory(RandomAccessFile file) {
		
		String currentInstruction = new String();
		Integer integerCurrentInstruction = new Integer(0);
		
		int currentMemoryPosition = 1;
		String binaryString = new String();
		
		try {
			
			Integer magicWord = new Integer(0);
			magicWord = file.readInt();
			
			// if the binary file starts with our magic word, the file is read into memory. 
			// otherwise the memory remains empty
			if (magicWord == 0) {
				
				while (currentInstruction != null) {
					integerCurrentInstruction = file.readInt();
					instructions[currentMemoryPosition] = integerCurrentInstruction;
					binaryString = Integer.toBinaryString(integerCurrentInstruction);
					currentMemoryPosition++;
				}

			} else {
				System.out.println("Unknown binary file format");
			}
						
			
		} catch(EOFException eof) {
			highestInstructionAddress = currentMemoryPosition - 1;
			if (debug) {
				System.out.println("End of file reached");
			}
			
		} catch(IOException io) {
			System.out.println("Error reading line from file");
		}
		
		/**
		 * If the first address of the instructionMemory is in use, then the first instruction is copied into IR
		 */
		if (instructions[1] != null) {
			CIP = 1;
			IR = instructions[CIP];
		}
		
		/**
		 * PC points at the next instruction after the current (which is 2)
		 * remember: our memory starts at position 1
		 * if the first instruction is a branch-command, the PC changes after command execution
		 */ 
		PC = CIP + 1;
		
		
	}
	
	
	private static void execute() {
		
		Integer currentInstruction = new Integer(0);
		
		int opCode;
		int targetValue;
		int firstSourceValue;
		int secondSourceValue; 
				
		int signBit; 
		
		while (IR != null) {
			
			if (debug) {
				continueListener.waitForEvent();
			}
			
			
			opCode = -1;
			currentInstruction = IR;
			
			opCode = currentInstruction >>> 26;
			
			// format 1 instructions
			if ((opCode >= 0) && (opCode <= HIGHEST_FORMAT_1)) {
				targetValue = currentInstruction & 65011712;
				targetValue = targetValue >>> 21;
				firstSourceValue = currentInstruction & 2031616;	
				firstSourceValue = firstSourceValue >>> 16;
				
				secondSourceValue = currentInstruction & 65535;
				
				
				signBit = secondSourceValue >>> 15;
				
				secondSourceValue = currentInstruction & 32767;
				//System.out.println(Integer.toBinaryString(IR) + ": " + Integer.toBinaryString(secondSourceValue));
				if (signBit == 1) {
					secondSourceValue = secondSourceValue * (-1);
				}
				
				if (debug) {
					debugDisplay.append("sign: " + (int)signBit + "\n");
					debugDisplay.append("opCode format 1 \n");
				}
				
				
			// format 2 instructions
			} else if ((opCode > HIGHEST_FORMAT_1) && (opCode <= HIGHEST_FORMAT_2)) {
				targetValue = currentInstruction & 65011712;
				targetValue = targetValue >>> 21;
				firstSourceValue = currentInstruction & 2031616;
				firstSourceValue = firstSourceValue >>> 16;
				
				secondSourceValue = currentInstruction & 31;
				
				if (debug) {
					debugDisplay.append("opCode format 2 \n");
				}
				
			// format 3 instructions
			} else if ((opCode > HIGHEST_FORMAT_2) && (opCode <= HIGHEST_FORMAT_3)) {
				targetValue = currentInstruction & 67108863; 
				firstSourceValue = 0;
				firstSourceValue = 0;
				secondSourceValue = 0;
				
				if (debug) {
					debugDisplay.append("opCode format 3 \n");
				}
				
			} else {
				targetValue = 0;
				firstSourceValue = 0;
				firstSourceValue = 0;
				secondSourceValue = 0;
				
				if (debug) {
					debugDisplay.append("unknown opCode format \n");
				}
			}
			
			
			if (debug) {
				debugDisplay.append("opcode: " + opCode + "\n");
				debugDisplay.append("target: " + targetValue  + "\n");
				debugDisplay.append("first : " + firstSourceValue  + "\n");
				debugDisplay.append("second: " + secondSourceValue  + "\n");
				
			}
			
			if (debug) {
				updateRegisterDisplay();
				
				String binaryString = new String();
				binaryString = Integer.toBinaryString(IR);
				
				while (binaryString.length() < 32) {
					binaryString = "0" + binaryString;
				}

				debugDisplay.append("CIP: " + CIP + "\n");
				debugDisplay.append("IR: " + binaryString + "\n");
			}
			
			
			switch(opCode) {
			case ADD: executeADD(targetValue, firstSourceValue, secondSourceValue); break;
			case ADDI: executeADDI(targetValue, firstSourceValue, secondSourceValue); break;
			case SUB: executeSUB(targetValue, firstSourceValue, secondSourceValue); break;
			case SUBI: executeSUBI(targetValue, firstSourceValue, secondSourceValue); break;
			case MUL: executeMUL(targetValue, firstSourceValue, secondSourceValue); break;
			case MULI: executeMULI(targetValue, firstSourceValue, secondSourceValue); break;
			case DIV: executeDIV(targetValue, firstSourceValue, secondSourceValue); break;
			case DIVI: executeDIVI(targetValue, firstSourceValue, secondSourceValue); break;
			case MOD: executeMOD(targetValue, firstSourceValue, secondSourceValue); break;
			case MODI: executeMODI(targetValue, firstSourceValue, secondSourceValue); break;
			case CMP: executeCMP(targetValue, firstSourceValue, secondSourceValue); break;
			case CMPI: executeCMPI(targetValue, firstSourceValue, secondSourceValue); break;
			case CHK: executeCHK(targetValue, firstSourceValue); break;
			case CHKI: executeCHKI(targetValue, firstSourceValue); break;
			case AND: executeAND(targetValue, firstSourceValue, secondSourceValue); break;
			case ANDI: executeANDI(targetValue, firstSourceValue, secondSourceValue); break;
			case BIC: executeBIC(targetValue, firstSourceValue, secondSourceValue); break;
			case BICI: executeBICI(targetValue, firstSourceValue, secondSourceValue); break;
			case OR: executeOR(targetValue, firstSourceValue, secondSourceValue); break;
			case ORI: executeORI(targetValue, firstSourceValue, secondSourceValue); break;
			case XOR: executeXOR(targetValue, firstSourceValue, secondSourceValue); break;
			case XORI: executeXORI(targetValue, firstSourceValue, secondSourceValue); break;
			case LSH: executeLSH(targetValue, firstSourceValue, secondSourceValue); break;
			case LSHI: executeLSHI(targetValue, firstSourceValue, secondSourceValue); break;
			case ASH: executeASH(targetValue, firstSourceValue, secondSourceValue); break;
			case ASHI: executeASHI(targetValue, firstSourceValue, secondSourceValue); break;
			case LDW: executeLDW(targetValue, firstSourceValue, secondSourceValue); break;
			case POP: executePOP(targetValue, firstSourceValue, secondSourceValue); break;
			case STW: executeSTW(targetValue, firstSourceValue, secondSourceValue); break;
			case PSH: executePSH(targetValue, firstSourceValue, secondSourceValue); break;
			case BEQ: executeBEQ(targetValue, secondSourceValue); break;
			case BNE: executeBNE(targetValue, secondSourceValue); break;
			case BLT: executeBLT(targetValue, secondSourceValue); break;
			case BGE: executeBGE(targetValue, secondSourceValue); break;
			case BGT: executeBGT(targetValue, secondSourceValue); break;
			case BLE: executeBLE(targetValue, secondSourceValue); break;
			case BSR: executeBSR(targetValue); break;
			case RET: executeRET(targetValue); break;
			case PRNI: executePRNI(targetValue, firstSourceValue, secondSourceValue); break;
			case PRNC: executePRNC(targetValue, firstSourceValue, secondSourceValue); break;
			case PRNB: executePRNB(targetValue, firstSourceValue, secondSourceValue); break;
			default: System.out.println("invalid opCode... \n");
			}
			
			if (debug) {
				debugDisplay.append("PC: " + PC + "\n");
			}
			
			
			// the next instruction to be executes lies at instruction-memory-address PC
			if ((PC != null) && ((instructions.length) >= PC)) {
				IR = instructions[PC];
				CIP = PC;
				
				// increment the PC to the next instruction-memory-address
				increasePC();
				
			} else {
				IR = null;
			}
			
			
			if (debug) {
				updateRegisterDisplay();
				debugDisplay.append("--------------------------- \n");
			}
			
		}
		
	}
	
	private static void initGui() {
		
		JFrame outputFrame = new JFrame("output");
		outputFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JScrollPane outputScrollPane = new JScrollPane();
		
		outputDisplay = new JTextArea();
		outputDisplay.setEditable(false);
		
		outputScrollPane.setViewportView(outputDisplay);
		
		GridLayout outputLayout = new GridLayout(2,1);
		
		outputFrame.add(outputScrollPane);
		
		outputFrame.setBounds(450,450,400,300);
		
        outputFrame.setVisible(true);
	}
	
	private static void initDebugGUI() {
		
		
		JFrame memoryFrame = new JFrame("overview memory");
	    memoryFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	    JFrame debugFrame = new JFrame("debug window");
	    debugFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    
	    JScrollPane scrollPane1 = new JScrollPane();
	    JScrollPane scrollPane2 = new JScrollPane();
	    JScrollPane scrollPane3 = new JScrollPane();
	    
	    JButton nextInstructionButton = new JButton("next Step");
	    nextInstructionButton.addActionListener(continueListener);
	    
	    registerDisplay = new JTextArea();
	    registerDisplay.setEditable(false);
	    
	    instructionDisplay = new JTextArea();
	    instructionDisplay.setEditable(false);
	    
	    debugDisplay = new JTextArea();
        debugDisplay.setEditable(false);
        
        
	    scrollPane1.setViewportView(registerDisplay);
	    scrollPane2.setViewportView(instructionDisplay);
	    scrollPane3.setViewportView(debugDisplay);
	    
        GridLayout layout = new GridLayout(1,4);
        GridLayout debugLayout = new GridLayout(2,1);
        
        GridBagLayout layout1 = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();

        
        constraints.gridwidth = 3;
        constraints.fill = constraints.BOTH;
        
        
        
        memoryFrame.setLayout(layout);
        debugFrame.setLayout(debugLayout);
        
        memoryFrame.add(scrollPane1,constraints);
        memoryFrame.add(scrollPane2, constraints);
        debugFrame.add(scrollPane3, constraints);
        debugFrame.add(nextInstructionButton);
        
        memoryFrame.setSize(400, 550);
        memoryFrame.setVisible(true);
        
        debugFrame.setBounds(450, 0, 400, 400);
        debugFrame.setVisible(true);
        
 	}
	
	private static void updateRegisterDisplay() {
		
		registerDisplay.setText("");
		String registerLabel = new String();
		
		for (int i=0; i<registers.length; i++) {
			
			if (i < 10) {
				registerLabel = "R0" + i;
			} else if (i == 29) {
				registerLabel = "FP (29)";
			} else if (i == 30) {
				registerLabel = "SP (30)";
			} else if (i == 31) {
				registerLabel = "LNK (31)";
			} else {
				registerLabel = "R" + i;
			}
			
			registerDisplay.append(registerLabel + ": " + registers[i] + "\n");
		}
			
	}
	
	private static void executeStringCommand(String currentLine) {
		
		if (currentLine == "")  {
			return;
		}
		
		int parameterCount = 0;
		
		int targetValue;
		int firstSourceValue;
		int secondSourceValue;
		
		String command = new String();
		String[] currentLineElements = new String[2];
		String[] parameters = new String[3];
		currentLineElements = currentLine.split(" ");
		
		parameters = currentLineElements[1].split(",");
		command = currentLineElements[0];
		
		command = command.toUpperCase();
		
		Integer integerTargetValue = new Integer(parameters[0]);
		Integer integerFirstSourceValue = new Integer(parameters[1]);
		Integer integerSecondSourceValue;
		
		if (parameters.length > 2) {
			integerSecondSourceValue = new Integer(parameters[2]);
		} else {
			integerSecondSourceValue = new Integer(0);
		}
		
		int instructionFormat = 0;
		
		targetValue = integerTargetValue.intValue();
		firstSourceValue = integerFirstSourceValue.intValue();
		secondSourceValue = integerSecondSourceValue.intValue();
		
		System.out.println("binary of " + targetValue + ": " + Integer.toBinaryString(targetValue));
		
		String targetRegisterBinaryRepresentation = new String();
		targetRegisterBinaryRepresentation = Integer.toBinaryString(targetValue);
		
		while (targetRegisterBinaryRepresentation.length() < 6) {
			targetRegisterBinaryRepresentation = "0" + targetRegisterBinaryRepresentation;
		}
		
		String firstSourceRegisterBinaryRepresentation = new String();
		firstSourceRegisterBinaryRepresentation = Integer.toBinaryString(firstSourceValue);
		
		while (firstSourceRegisterBinaryRepresentation.length() < 4) {
			firstSourceRegisterBinaryRepresentation = "0" + firstSourceRegisterBinaryRepresentation;
		}
		
		String secondSourceRegisterBinaryRepresentation = new String();
		secondSourceRegisterBinaryRepresentation = Integer.toBinaryString(secondSourceValue);
		
		while (secondSourceRegisterBinaryRepresentation.length() < 4) {
			secondSourceRegisterBinaryRepresentation = "0" + secondSourceRegisterBinaryRepresentation;
		}
		
		
		
		
		if (command.matches("ADD")) {
			executeADD(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("ADDI")) {
			executeADDI(targetValue, firstSourceValue, secondSourceValue);
		}
		if (command.matches("SUB")) {
			executeSUB(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("SUBI")) {
			executeSUBI(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("MUL")) {
			executeMUL(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("MULI")) {
			executeMULI(targetValue, firstSourceValue, secondSourceValue);
		}
		if (command.matches("DIV")) {
			executeDIV(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("DIVI")) {
			executeDIVI(targetValue, firstSourceValue, secondSourceValue);
		}
		if (command.matches("MOD")) {
			executeMOD(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("MODI")) {
			executeMODI(targetValue, firstSourceValue, secondSourceValue);
		}
		if (command.matches("CMP")) {
			executeCMP(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("CMPI")) {
			executeCMPI(targetValue, firstSourceValue, secondSourceValue);
		}
		if (command.matches("CHK")) {
			executeCHK(targetValue, firstSourceValue);
		}
		else if(command.matches("CHKI")) {
			executeCHKI(targetValue, firstSourceValue);
		}
		else if(command.matches("AND")) {
			executeAND(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("ANDI")) {
			executeANDI(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("BIC")) {
			executeBIC(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("BICI")) {
			executeBICI(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("OR")) {
			executeOR(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("ORI")) {
			executeORI(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("XOR")) {
			executeXOR(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("XORI")) {
			executeXORI(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("LSH")) {
			executeLSH(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("LSHI")) {
			executeLSHI(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("ASH")) {
			executeASH(targetValue, firstSourceValue, secondSourceValue);
		}
		else if(command.matches("ASHI")) {
			executeASHI(targetValue, firstSourceValue, secondSourceValue);
		}
		
		updateRegisterDisplay();
	}

	
	private static void increasePC() {
			
		if (PC < (instructions.length-1)) {
			PC++;
		} else {
			PC = null;
		}
		
	}
	
	private static void executeADD(int r0, int r1, int r2) {
		if (debug) {
			instructionDisplay.append("ADD " + r0  + ", " + registers[r1] + ", " + registers[r2]);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = registers[r1] + registers[r2];
		
	}
	
	private static void executeADDI(int r0, int r1, int r2) {
		if (debug) {
			instructionDisplay.append("ADDI " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = registers[r1] + r2;
		
		
	}
	
	private static void executeSUB(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("SUB " + r0  + ", " + registers[r1] + ", " + registers[r2]);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = registers[r1] - registers[r2];
	
		
	}
	
	private static void executeSUBI(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("SUBI " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = registers[r1] - r2;
		
		
		
	}
	
	private static void executeMUL(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("MUL " + r0  + ", " + registers[r1] + ", " + registers[r2]);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = registers[r1] * registers[r2];
		
		
		
	}
	
	private static void executeMULI(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("MULI " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = registers[r1] * r2;

		
		
	}
	
	private static void executeDIV(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("DIV " + r0  + ", " + registers[r1] + ", " + registers[r2]);
			instructionDisplay.append("\n");
		}
		
		
		if (registers[r2] == 0) {
			System.out.println("Division by 0");
			return;
		}
		
		registers[r0] = registers[r1] / registers[r2];
		
		
	}
	
	private static void executeDIVI(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("DIVI " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		if (r2 == 0) {
			System.out.println("Division by 0");
			return;
		}
			
		registers[r0] = registers[r1] / r2;
		
		
	}

	private static void executeMOD(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("MOD " + r0  + ", " + registers[r1] + ", " + registers[r2]);
			instructionDisplay.append("\n");
		}
		
		if (registers[r2] == 0) {
			System.out.println("Division by 0");
			return;
		}
		
		registers[r0] = registers[r1] % registers[r2];
	
		
	}
	
	private static void executeMODI(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("MODI " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		
		if (r2 == 0) {
			System.out.println("Division by 0");
			return;
		}
		
		registers[r0] = registers[r1] % r2;
		
		
	}
	
	private static void executeCMP(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("CMP " + r0  + ", " + registers[r1] + ", " + registers[r2]);
			instructionDisplay.append("\n");
		}
		
		
		registers[r0] = registers[r1] - registers[r2];
		
		
	}
	
	private static void executeCMPI(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("CMPI " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = registers[r1] - r2;
		
		

	}
	
	private static void executeCHK(int r0, int r1) {


		if (debug) {
			instructionDisplay.append("CHK " + r0  + ", " + registers[r1]);
			instructionDisplay.append("\n");
		}
		
		if ((registers[r0] < 0) || (registers[r0] > registers[r1])) {
			System.out.println("ERROR: CHK: out of range");
		}
		

	}
	
	private static void executeCHKI(int r0, int r1) {
		
		if (debug) {
			instructionDisplay.append("CHKI " + r0  + ", " + registers[r1]);
			instructionDisplay.append("\n");
		}
		
		if ((registers[r0] < 0) || (registers[r0] > r1)) {
			System.out.println("ERROR: CHKI: out of range");
		}
		
	}
	
	private static void executeAND(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("AND " + r0  + ", " + registers[r1] + ", " + registers[r2]);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = registers[r1] & registers[r2];
		
		
	}
	
	private static void executeANDI(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("ANDI " + r0  + ", " + registers[r1] + ", " + registers[r2]);
			instructionDisplay.append("\n");
		}
		
		
		registers[r0] = registers[r1] & r2;
		
	}

	private static void executeOR(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("OR " + r0  + ", " + registers[r1] + ", " + registers[r2]);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = registers[r1] | registers[r2];
		
	}
	
	private static void executeORI(int r0, int r1, int r2) {

		if (debug) {
			instructionDisplay.append("ORI " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		registers[r0] = registers[r1] | r2;
		
	}
	
	private static void executeXOR(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("XOR " + r0  + ", " + registers[r1] + ", " + registers[r2]);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = registers[r1] ^ registers[r2];
		
	}
	
	private static void executeXORI(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("XORI " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = registers[r1] ^ r2;
		
	}
	private static void executeBIC(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("BIC " + r0 + ", " + registers[r1] + ", " + registers[r2]);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = registers[r1] & (~registers[r2]);
		
	}
	
	private static void executeBICI(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("BICI " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = registers[r1] & (~r2);
		
	}	
	private static void executeLSH(int r0, int r1, int r2) {

		if (debug) {
			instructionDisplay.append("LSH " + r0  + ", " + registers[r1] + ", " + registers[r2]);
			instructionDisplay.append("\n");
		}
		
		if (r2 > 0) {
			registers[r0] = registers[r1] >>> (registers[r2]);	
		} else {
			registers[r0] = registers[r1] >> (registers[r2]);
		}
		
	}
	
	private static void executeLSHI(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("LSHI " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		if (r2 > 0) {
			registers[r0] = registers[r1] >>> (r2);	
		} else {
			registers[r0] = registers[r1] >> (r2);
		}
		
	}	
	
	private static void executeASH(int r0, int r1, int r2) {

		if (debug) {
			instructionDisplay.append("ASH " + r0  + ", " + registers[r1] + ", " + registers[r2]);
			instructionDisplay.append("\n");
		}
		
		if (r2 > 0) {
			registers[r0] = registers[r1] >> (registers[r2]);	
		}
		
	}
	
	private static void executeASHI(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("ASHI " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		if (r2 > 0) {
			registers[r0] = registers[r1] >> (r2);	
		}
		
	}
	
	
	
	private static void executeLDW(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("LDW " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = memory[registers[r1] + r2];
		
	}
	
	private static void executePOP(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("POP " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		registers[r0] = memory[registers[r1]];
		registers[r1] = registers[r1] + r2;
		
		//registers[30] = registers[30] - r2;
		
	}
	
	
	private static void executeSTW(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("STW " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		memory[registers[r1] + r2] = registers[r0];
		
	}
	
	
	private static void executePSH(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("PSH " + r0  + ", " + registers[r1] + ", " + r2);
			instructionDisplay.append("\n");
		}
		
		registers[r1] = registers[r1] - r2;
		memory[registers[r1]] = registers[r0];
		
		// increment SP
		//registers[30] = registers[30] + r2;
		
		
	}
	
	private static void executePRNI(int r0, int r1, int r2) {
		
		if (debug) {
			instructionDisplay.append("PRNI " + registers[r0] + ", " + registers[r1] + ", " +  registers[r2]);
		}
		
		if (registers[r2] != null) {
			int intValue = registers[r2];
			
			outputDisplay.append(intValue + " ");
			System.out.println(intValue);
		}	
		
	}
	
	private static void executePRNC(int r0, int r1,  int r2) {

		if (debug) {
			instructionDisplay.append("PRNC " + registers[r0] + ", " + registers[r1] + ", " +  registers[r2]);
		}
		
		if (registers[r2] != null) {
			int intValue = registers[r2];
			char charValue = (char)intValue;
			
			outputDisplay.append(charValue + " ");
			System.out.println(charValue);
					
		}	
	}
	
	
	private static void executePRNB(int r0, int r1,  int r2) {

		if (debug) {
			instructionDisplay.append("PRNB " + registers[r0] + ", " + registers[r1] + ", " +  registers[r2]);
		}
		
		String booleanValueRepresentation = new String();
		
		if (registers[r2] != null) {
			int intValue = registers[r2];
			
			if (intValue == 0) {
				booleanValueRepresentation = "false";
			} else if (intValue == 1) {
				booleanValueRepresentation = "true";
			}
			
			if (booleanValueRepresentation.equals("") == false) {
				outputDisplay.append(booleanValueRepresentation);
				System.out.println(booleanValueRepresentation);
			}
			
		}	
	}
	
	
	private static void executeBSR(int jumpAddress) {
		
		if (debug) {
			instructionDisplay.append("BSR " + jumpAddress);
			instructionDisplay.append("\n");
		}
		
		registers[31] = PC;
		PC = jumpAddress; 
		
		
	}
	
	private static void executeRET(int r0) {
		
		if (debug) {
			instructionDisplay.append("RET " + registers[r0]);
			instructionDisplay.append("\n");
		}
		
		PC = registers[r0];
		
		
	}
	
	
	private static void executeBEQ(int r0, int jumpAddress) {
		
		if (debug) {
			instructionDisplay.append("BEQ " + registers[r0] + ", " + jumpAddress);
			instructionDisplay.append("\n");
		}
		
		if (registers[r0] == 0) {
			//System.out.println("BEQ: value of register " + r0 + " is 0");
			executeBSR(jumpAddress);
		}
		
		
		
	}
	
	
	private static void executeBNE(int r0, int jumpAddress) {
		
		if (debug) {
			instructionDisplay.append("BNE " + registers[r0] + ", " + jumpAddress);
			instructionDisplay.append("\n");
		}
		
		if (registers[r0] != 0) {
			//System.out.println("BNE: value of register " + r0 + " is not 0");
			executeBSR(jumpAddress);
		}		
		
		
	}
	
	private static void executeBLT(int r0, int jumpAddress) {
		
		if (debug) {
			instructionDisplay.append("BLT " + registers[r0] + ", " + jumpAddress);
			instructionDisplay.append("\n");
		}
		
		if (registers[r0] < 0) {
			//System.out.println("BLT: value of register " + r0 + " is < 0");
			executeBSR(jumpAddress);
		}		
		
		
	}
	
	private static void executeBGE(int r0, int jumpAddress) {
		
		if (debug) {
			instructionDisplay.append("BGE " + registers[r0] + ", " + jumpAddress);
			instructionDisplay.append("\n");
		}
		
		if (registers[r0] >= 0) {
			//System.out.println("BGE: value of register " + r0 + " is >= 0");
			executeBSR(jumpAddress);
		}		
		
		
	}
	
	private static void executeBGT(int r0, int jumpAddress) {
		
		if (debug) {
			instructionDisplay.append("BGT " + registers[r0] + ", " + jumpAddress);
			instructionDisplay.append("\n");
		}
		
		if (registers[r0] > 0) {
			//System.out.println("BGT: value of register " + r0 + " is > 0");
			executeBSR(jumpAddress);
		}		
		
	}
	
	private static void executeBLE(int r0, int jumpAddress) {
		
		if (debug) {
			instructionDisplay.append("BLE " + registers[r0] + ", " + jumpAddress);
			instructionDisplay.append("\n");
		}
		
		if (registers[r0] <= 0) {
			//System.out.println("BLE: value of register " + r0 + " is <= 0");
			executeBSR(jumpAddress);
		}		
		
		
	}
	
	
}
