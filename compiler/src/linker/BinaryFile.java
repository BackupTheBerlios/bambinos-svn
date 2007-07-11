package linker;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Hashtable;
import java.util.Vector;

public class BinaryFile {

	private RandomAccessFile binaryFile;
	private Vector<Integer> instructions;
	private Hashtable<String,Integer> moduleList;
	
	private ObjectFile mainModule;
	private String filename = new String();
	
	private final Integer header = 1;
	
	public BinaryFile(ObjectFile mainModule) {
		
		this.moduleList = new Hashtable<String, Integer>();
		this.instructions = new Vector();
		
		// writes the header to the instructions (so that the first instruction starts with 1)
		this.instructions.add(this.header);
		
		this.mainModule = mainModule;
		
		this.filename = mainModule.moduleName.concat(".bin");
		
		//this.binaryFile = openFile(filename);
		this.addCode(mainModule);
	}
	
	
	private void fixAddressing(ObjectFile mainModule) {
		
		if (mainModule == null) {
			mainModule = this.mainModule;
		}
		
		Integer mainModuleOffset = moduleList.get(mainModule.moduleName); 
		
		
		FixupTableElement[] fixupTable = mainModule.getFixupTable();
		
		for (int i = 0; i < fixupTable.length; i++) {
			FixupTableElement currentFixupTableElement = fixupTable[i];
			
			
			ObjectFile importedModule = new ObjectFile(currentFixupTableElement.module.concat(".obj"), "r");
			OffsetTableElement[] importedOffsetTable = importedModule.getOffsetTable();
			
			this.addCode(importedModule);
			
			for (int j = 0; j < importedOffsetTable.length; j++) {
				
				OffsetTableElement currentOffsetTableElement = importedOffsetTable[j];
				
				if (currentOffsetTableElement.name.equals(currentFixupTableElement.name)) {
					
					// offset of the imported module
					Integer importedModuleOffset = moduleList.get(importedModule.moduleName);
					
					// we substract 1 because the offset is already the first value of the opCode
					Integer fixupOffsetInOffsetTable = importedModuleOffset + currentOffsetTableElement.offset - 1;
					
					// offset of the main module
					Integer fixupOffsetInFixupTable = mainModuleOffset + currentFixupTableElement.offset - 1;
					
					// fix the command in the main-module with the address of the imported module
					Integer currentInstruction = this.instructions.elementAt(fixupOffsetInFixupTable);
					currentInstruction = fixCommand(currentInstruction, fixupOffsetInOffsetTable);
					instructions.set(fixupOffsetInFixupTable, currentInstruction);
					
				}
				
			}
			
		
		}
		
	}
	
	public void fixAddressing() {
		
		fixAddressing(null);
		
	}

	/**
	 * changes the value of currentInstruction to instructionValue
	 * @param currentInstruction
	 * @param instructionValue
	 * @return currentInstruction
	 * @author lacki
	 */
	private Integer fixCommand(Integer currentInstruction, Integer instructionValue) {
				
		currentInstruction = currentInstruction >> 26;
		currentInstruction = currentInstruction << 26;
		
		currentInstruction = currentInstruction + instructionValue;
		
		return currentInstruction;
		
	}
	
	/**
	 * Adds code from objectFile to the instructions-vector and marks the offset of this code in moduleList
	 * @param objectFile
	 * @author lacki
	 */
	public void addCode(ObjectFile objectFile) {
		
		// if the objectFile was already added to the binaryFile it won't be added again.
		if ((objectFile == null) || (moduleList.containsKey(objectFile.moduleName))) {
			return;
		}
		
		Integer[] opCode = objectFile.getOpCode();
		// writes the modulename and the offset of the module code in the instructions-vector into the instructions-vector
		this.moduleList.put(objectFile.moduleName, this.instructions.size());
		
		// writes the opCode in the instructions-vector
		for (int i=0; i < opCode.length; i++) {
			this.instructions.addElement(opCode[i]);
		}
		
	}
	
	
	/**
	 * Writes the instructions to the binary file
	 */
	public void export() {
		
		System.out.println("exporting binary file");
		
		this.prepareBinaryFile();
		
		for (int i = 0; i < this.instructions.size(); i++) {
			
			this.writeWord(this.instructions.elementAt(i));
			
		}
		
		System.out.println("data written into file " + this.filename );
		
	}
	
	/**
	 * deletes an already existing binary file, and writes the binary-header to a new one
	 * @author lacki
	 */
	private void prepareBinaryFile() {
		
		File tmpBinary = new File(this.filename);
		tmpBinary.delete();
		
		
		this.binaryFile = this.openFile(filename);		
		
	}
	
	private void writeWord(Integer currentInstruction) {
		
		try {
			this.binaryFile.writeInt(currentInstruction);
		} catch (IOException io) {
			System.out.println("Error while exporting binary");
		}
		
	}
	
	/**
	 * Opens a file in a given mode and returns a filedescriptor
	 * 
	 * @param filename The filename of the file to open
	 * @return The filedescriptor
	 * @author Lacki
	 */
	private RandomAccessFile openFile(String filename) {

		try {
			RandomAccessFile binaryFile = new RandomAccessFile(filename, "rw");
			return binaryFile;
			
		} catch (IOException io) {
			System.out.println("Cannot open binaryFile");
			return null;
		}
	}
	
	
}
