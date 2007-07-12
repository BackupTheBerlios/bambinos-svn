package linker;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Hashtable;
import java.util.Vector;

public class BinaryFile {

	private RandomAccessFile binaryFile;
	private Vector<Integer> instructions;
	private Hashtable<String,ObjectFile> moduleList;
	
	private ObjectFile mainModule;
	private String filename = new String();
	
	private final Integer header = 1;
	
	public BinaryFile(ObjectFile mainModule) {
		
		this.moduleList = new Hashtable<String, ObjectFile>();
		this.instructions = new Vector();
		
		// writes the header to the instructions (so that the first instruction starts with 1)
		this.instructions.add(this.header);
		
		this.mainModule = mainModule;
		
		this.filename = mainModule.moduleName.concat(".bin");
		
		//this.binaryFile = openFile(filename);
		this.mainModule = this.addCode(mainModule);
		this.addExitSymbol();
	}
	
	
	private OffsetTableElement[] fixOffsetTable(OffsetTableElement[] offsetTable, Integer moduleOffset) {
						
		for (int i = 0; i < offsetTable.length; i++) {
			
			OffsetTableElement currentElement = offsetTable[i];
			
			currentElement.offset = currentElement.offset + moduleOffset - 1;
			
			offsetTable[i] = currentElement;
		}
		
		return offsetTable;
		
	}
	
	private FixupTableElement[] fixFixupTable(FixupTableElement[] fixupTable, Integer moduleOffset) {
		
		
		for (int i = 0; i < fixupTable.length; i++) {
			FixupTableElement currentFixupTableElement = fixupTable[i];
			
			
			String importedModuleName = currentFixupTableElement.module.concat(".obj");
			
			ObjectFile importedModule;
			
			// if the module was already loaded into the binaryfile it doesn't have to be loaded again
			if (this.moduleList.containsKey(currentFixupTableElement.module)) {
				importedModule = moduleList.get(currentFixupTableElement.module);
			} else {
				importedModule = new ObjectFile(importedModuleName, "r");
				importedModule = this.addCode(importedModule);
				
				importedModule = this.fixAddressing(importedModule);
				
			}
			
			
			// offset of the imported module
			//Integer importedModuleOffset = this.moduleList.get(importedModule);
			Integer importedModuleOffset = importedModule.getBinaryOffset();
			
			
			//importedModule = this.fixAddressing(importedModule);
			
			OffsetTableElement[] importedOffsetTable = importedModule.getOffsetTable();
			
			//importedOffsetTable = this.fixOffsetTable(importedOffsetTable, importedModuleOffset);
			
			for (int j = 0; j < importedOffsetTable.length; j++) {
				
				OffsetTableElement currentOffsetTableElement = importedOffsetTable[j];
				
				if (currentOffsetTableElement.name.equals(currentFixupTableElement.name)) {
					
					
					// we substract 1 because the offset is already the first value of the opCode
					//Integer fixupOffsetInOffsetTable = importedModuleOffset + currentOffsetTableElement.offset - 1;
					Integer fixupOffsetInOffsetTable = currentOffsetTableElement.offset;
					
					Integer currentInstruction;
					
					//currentInstruction = this.instructions.elementAt(fixupOffsetInOffsetTable);
					//currentInstruction = fixCommand(currentInstruction, fixupOffsetInOffsetTable);
					
					// offset of the main module
					Integer fixupOffsetInFixupTable = moduleOffset + currentFixupTableElement.offset - 1;
					
					// fix the command in the main-module with the address of the imported module
					currentInstruction = this.instructions.elementAt(fixupOffsetInFixupTable);
					currentInstruction = fixCommand(currentInstruction, fixupOffsetInOffsetTable);
					instructions.set(fixupOffsetInFixupTable, currentInstruction);
					
				}
				
			}
			
			moduleList.put(importedModuleName, importedModule);
			
		}
		
		return fixupTable;
	}
	
	private ObjectFile fixAddressing(ObjectFile module) {
		
		//Integer moduleOffset = moduleList.get(module);
		Integer moduleOffset = module.getBinaryOffset();
		
		//OffsetTableElement[] offsetTable = module.getOffsetTable();
		//offsetTable = this.fixOffsetTable(offsetTable, moduleOffset);
		
		//module.setOffsetTable(offsetTable);
		
		FixupTableElement[] fixupTable = module.getFixupTable();
		fixupTable = this.fixFixupTable(fixupTable, moduleOffset);
		
		module.setFixupTable(fixupTable);
		
		return module;		
	}
	
	public ObjectFile fixAddressing() {
		
		ObjectFile mainModule = fixAddressing(this.mainModule);
		return mainModule;
		
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
	public ObjectFile addCode(ObjectFile objectFile) {
		
		// if the objectFile was already added to the binaryFile it won't be added again.
		// that means it must have a valid offsetInBinary-value (!= -1)
		if ((objectFile == null) || (objectFile.getBinaryOffset() != (-1))) {
			return objectFile;
		}
		
		
		moduleList.put(objectFile.moduleName, objectFile);
		
		Integer[] opCode = objectFile.getOpCode();
		
		
		// writes the modulename and the offset of the module code in the instructions-vector into the instructions-vector
		Integer moduleOffset = this.instructions.size();
		objectFile.setBinaryOffset(moduleOffset);
		//this.moduleList.put(objectFile, moduleOffset);
		
		
		// TODO fix Addressing of offsettable
		OffsetTableElement[] offsetTable = objectFile.getOffsetTable();
		
		for (int i = 0; i < offsetTable.length; i++) {
			
			OffsetTableElement currentElement = offsetTable[i];
			currentElement.offset = currentElement.offset + moduleOffset - 1;
			
			offsetTable[i] = currentElement;
			
		}
		
		// writes the opCode in the instructions-vector
		for (int i=0; i < opCode.length; i++) {
			this.instructions.addElement(opCode[i]);
		}
		
		return objectFile;
		
	}
	
	/**
	 * Adds an exit instruction to the instructions-vector. If this symbol is executed by the VM, the program execution stops.
	 * This symbol is intended as the last executed command in the binary.
	 * The instruction means "BSR -1" 
	 * @author lacki
	 */
	private void addExitSymbol() {
		
		Integer exitInstruction = new Integer(0);
		
		Integer opCode = new Integer(50);
		Integer value = new Integer(1);
		
		exitInstruction = opCode;
		exitInstruction = exitInstruction << 1;
		
		exitInstruction = exitInstruction | 1;
		
		
		exitInstruction = exitInstruction << 25;
		
		exitInstruction = exitInstruction | value;
		
		this.instructions.addElement(exitInstruction);
		
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
