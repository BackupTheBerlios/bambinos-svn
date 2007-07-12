package linker;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ObjectFile {

	public String moduleName;
	public Integer magicWord;
	public Integer mainInstruction;
	public int offsetTableSize;
	public int fixupTableSize;

	private final int SUCCESS = 0;
	private final int INVALID_MAGIC_WORD = 1;
	private final int INVALID_FILESTRUCTURE = 2;

	private long offsetTableOffset;
	private long fixupTableOffset;
	private long opCodeOffset;
	
	private OffsetTableElement[] offsetTable;
	private FixupTableElement[] fixupTable;

	private int offsetInBinaryFile = -1;
	
	private boolean writeable = false;

	public RandomAccessFile file;

	/**
	 * @param filename
	 *            The filename of the objectFile (without ending)
	 * @param mode
	 *            The mode of the file (can be "r" for reading or "rw" for
	 *            writing)
	 * @author Lacki
	 */
	public ObjectFile(String filename, String mode) {

		if ((mode.equals("r")) || (mode.equals("rw"))) {

			this.moduleName = getModuleName(filename);
			this.file = openFile(filename, mode);

		} else {
			return;
		}

		// if the objectFile-object is created in ReadOnly mode, the existing
		// file gets scanned
		// otherwise another operation is executed TODO
		if (writeable == false) {
			if (scanFile() != SUCCESS) {
				System.out.println("An error occured");
			}
		}

	}
	
	public void setBinaryOffset(int value) {
		this.offsetInBinaryFile = value;
	}
	
	
	public int getBinaryOffset() {
		return this.offsetInBinaryFile;
	}

	/**
	 * Extracts the modulename from a filename. That means the path and the file-ending are cut
	 * @param filename
	 * @return modulename
	 * @author lacki
	 */
	private String getModuleName(String filename) {

		String moduleName = new String();

		filename = filename.split("\\.")[0];
		String[] pathElements = filename.split(File.separator);

		moduleName = pathElements[pathElements.length - 1];

		return moduleName;

	}

	/**
	 * writes the Symbol and the fixup Table into the object file
	 * 
	 * @param map
	 * @throws IOException
	 */
	public void writeTable(HashMap<String, Integer> map) throws IOException {
		for (Map.Entry<String, Integer> e : map.entrySet()) {
			int number = 0;
			String elem = e.getKey();
			int offset = e.getValue();
			elem = elem.concat("=");
			elem = makeCorrString(elem);
			for (int i = 0; i < elem.length(); i += 4) {
				number = ((int) elem.charAt(i) << 24) + ((int) elem.charAt(i + 1) << 16) +
						((int) elem.charAt(i + 2) << 8) + ((int) elem.charAt(i + 3));
				file.writeInt(number);
			}
			file.writeInt(offset);
		}
	}

	public void writeTable(Vector<FixupTableElement> map) throws IOException {
		for (int i = 0; i < map.size(); i++) {
			int number = 0;
			String elem = map.get(i).module;
			int offset = map.get(i).offset;
			elem = elem.concat("=");
			elem = makeCorrString(elem);
			for (int j = 0; j < elem.length(); j += 4) {
				number = ((int) elem.charAt(j) << 24) + ((int) elem.charAt(j + 1) << 16) +
						((int) elem.charAt(j + 2) << 8) + ((int) elem.charAt(j + 3));
				file.writeInt(number);
			}
			file.writeInt(offset);
		}
	}

	// creates Strings which are divisible by 4
	public String makeCorrString(String x) {
		int mod = x.length() % 4;
		if (mod != 0) {
			mod = 4 - mod;
			while (mod > 0) {
				x = x.concat("0");
				mod--;
			}
		}
		return x;
	}

	/**
	 * Scans the structure of the objectFile and writes the values into the
	 * class-properties
	 */
	private int scanFile() {

		// reads the magicWord of the file
		Integer magicWord = readNextWord();

		// Writes the class property magicWord
		if (magicWord != null) {

			if (magicWord != 0) {
				return INVALID_MAGIC_WORD;
			}

			this.magicWord = magicWord;
		} else {
			return INVALID_MAGIC_WORD;
		}

		// Skips the jump-to-main-method command (BSR xxx) (4 bytes)
		this.mainInstruction = readNextWord();

		// reads the length of the offsetTable (length is in words)
		Integer offsetTableSize = readNextWord();

		// writes the class property offsetTableSize
		if (offsetTableSize != null) {
			this.offsetTableSize = offsetTableSize;
		} else {
			return INVALID_FILESTRUCTURE;
		}

		this.offsetTableOffset = getCurrentFileOffset();

		
		this.offsetTable = this.readOffsetTable();
		
		/*
		// skips the offsetTable TODO
		for (int i = 0; i < this.offsetTableSize; i++) {
			readNextWord();
		}
		*/
		
		
		// reads the length of the fixupTable (length is in words)
		Integer fixupTableSize = readNextWord();

		this.fixupTableOffset = getCurrentFileOffset();
	
		// writes the class property fixupTableSize
		if (fixupTableSize != null) {
			this.fixupTableSize = fixupTableSize;
		} else {
			return INVALID_FILESTRUCTURE;
		}

		this.fixupTable = this.readFixupTable();
		
		/*
		// skips the fixupTable TODO
		for (int i = 0; i < this.fixupTableSize; i++) {
			readNextWord();
		}
		*/
		
		
		this.opCodeOffset = getCurrentFileOffset();

		return SUCCESS;
	}
	
	public FixupTableElement[] getFixupTable() {
		return this.fixupTable;
	}
	
	public OffsetTableElement[] getOffsetTable() {
		return this.offsetTable;
	}
	
	public void setFixupTable(FixupTableElement[] fixupTable) {
		this.fixupTable = fixupTable;
	}

	public void setOffsetTable(OffsetTableElement[] offsetTable) {
		this.offsetTable = offsetTable;
	}
	
	/**
	 * reads the fixup-table from the object file and returns its elements in an
	 * array
	 * 
	 * @return Array of FixupTableElements
	 * @author Lacki
	 */
	public FixupTableElement[] readFixupTable() {

		// sets the filepointer to the beginning of the fixuptable
		this.setFileOffset(this.fixupTableOffset);

		Vector<FixupTableElement> tmpFixupTableElements = new Vector<FixupTableElement>();

		// this verifies, that we don't read more than the data from the
		// fixup-table
		// the fixupTableOffset + fixupTableSize tells us how much we need to
		// read to get the whole table
		while (this.getCurrentFileOffset() < (this.fixupTableOffset + this.fixupTableSize)) {

			FixupTableElement currentElement = new FixupTableElement();

			String path = new String();

			String[] pathElements;

			String module = new String();
			String name = new String();
			Integer offset;

			path = readStringFromFile('=');

			pathElements = path.split("\\.");

			if (pathElements.length != 2) {
				System.out.println("Error while reading fixupTable. Invalid file syntax.");
				return null;
			}

			module = pathElements[0];
			name = pathElements[1];

			offset = readNextWord();

			currentElement.module = module;
			currentElement.name = name;
			currentElement.offset = offset;

			tmpFixupTableElements.addElement(currentElement);

		}

		// the Vector that contains the FixupTableElements is traversed into an
		// array
		FixupTableElement[] fixupTable = new FixupTableElement[tmpFixupTableElements.size()];

		for (int i = 0; i < tmpFixupTableElements.size(); i++) {
			fixupTable[i] = tmpFixupTableElements.elementAt(i);
		}

		return fixupTable;
	}

	/**
	 * reads the offset-table from the object file and returns its elements in
	 * an array
	 * 
	 * @return Array of OffsetTableElements
	 * @author Lacki
	 */
	public OffsetTableElement[] readOffsetTable() {

		// sets the filepointer to the beginning of the offsetTable
		this.setFileOffset(this.offsetTableOffset);

		Vector<OffsetTableElement> tmpOffsetTableElements = new Vector<OffsetTableElement>();

		// this verifies, that we don't read more than the data from the
		// offset-table
		// the offsetTableOffset + offsetTableSize tells us how much we need to
		// read to get the whole table
		while (this.getCurrentFileOffset() < (this.offsetTableOffset + this.offsetTableSize)) {

			OffsetTableElement currentElement = new OffsetTableElement();

			String name = new String();
			Integer offset;

			name = readStringFromFile('=');
			offset = readNextWord();

			currentElement.name = name;
			currentElement.offset = offset;

			tmpOffsetTableElements.addElement(currentElement);

		}

		// the Vector that contains the OffsetTableElements is traversed into an
		// array
		OffsetTableElement[] offsetTable = new OffsetTableElement[tmpOffsetTableElements.size()];

		for (int i = 0; i < tmpOffsetTableElements.size(); i++) {
			offsetTable[i] = tmpOffsetTableElements.elementAt(i);
		}

		return offsetTable;

	}

	/**
	 * reads the opCode and returns it as Integer-Array (Integer[]).
	 * 
	 * @return Integer[]
	 */
	public Integer[] getOpCode() {

		// sets the filepointer to the opCode offset
		this.setFileOffset(this.opCodeOffset);

		Vector<Integer> tmpOpCode = new Vector<Integer>();

		Integer currentCommand = readNextWord();

		while (currentCommand != null) {

			tmpOpCode.addElement(currentCommand);
			currentCommand = readNextWord();

		}

		Integer[] opCode = new Integer[tmpOpCode.size()];

		for (int i = 0; i < tmpOpCode.size(); i++) {
			opCode[i] = tmpOpCode.elementAt(i);
		}

		//opCode = (Integer[]) tmpOpCode.toArray();

		return opCode;
	}

	/**
	 * reads a string from the objectFile until a given delimiter is reached.
	 * this reads full words from the file even if the string doesn't need a
	 * full word-space in that case the additional bytes to a full word are
	 * thrown away
	 */
	private String readStringFromFile(char stringDelimiter) {

		String value = new String();
		byte delimiter = (byte) stringDelimiter;
		int bytesRead = 0;

		byte currentByte = readNextByte();

		// read bytes from the file until the delimiter is read
		while (currentByte != delimiter) {
			bytesRead++;
			value = value + (char) currentByte;
			currentByte = readNextByte();
		}

		// the equals-sign needs to be added to the read-bytes
		bytesRead++;

		// bytesRead indicates how many bytes were read.
		// since we are wordaligned we need to read some additional bytes to
		// assure that we have read full words
		int bytesToRead = bytesRead % 4;
		bytesToRead = 4 - bytesToRead;

		for (int i = 0; i < bytesToRead; i++) {
			readNextByte();
		}

		return value;
	}

	/**
	 * reads the next 32bit word from the current fileposition
	 * 
	 * @return the word (as Integer)
	 */
	private Integer readNextWord() {

		Integer currentWord = null;

		try {
			currentWord = this.file.readInt();
			System.out.println(currentWord);
			return currentWord;
		} catch (EOFException eof) {
			return null;
		} catch (IOException io) {
			System.out.println("An error occurred while reading objectFile");
			return null;
		}

	}

	/**
	 * read the next 4bit byte from the current fileposition
	 * 
	 * @return the byte (as byte)
	 */
	private byte readNextByte() {

		byte currentByte = -1;

		try {
			currentByte = this.file.readByte();
			return currentByte;
		} catch (EOFException eof) {
			return -2;
		} catch (IOException io) {
			System.out.println("An error occurred while reading objectFile");
			return -1;
		}

	}

	/**
	 * sets the filepointer of the objectFile to the given offset
	 * 
	 * @param the
	 *            offset where to put to filepointer
	 */
	private void setFileOffset(long offset) {

		try {

			this.file.seek(offset * 4);

		} catch (IOException io) {
			System.out.println("Cannot set filepointer to specified position in objectFile");
		}

	}

	/**
	 * Reads the current position in the File and returns it as an offset (as
	 * long value)
	 * 
	 * @return the offset (as long)
	 */
	private long getCurrentFileOffset() {
		long currentOffset = 0;

		try {
			currentOffset = this.file.getFilePointer();

			if ((currentOffset % 4) == 0) {
				return currentOffset / 4;
			} else {
				System.out.println("Invalid objectFile structure");
				return -1;
			}

		} catch (IOException io) {
			System.out.println("An error occurred while reading objectFile");
			return -1;
		}

	}

	/**
	 * Opens a file in a given mode and returns a filedescriptor
	 * 
	 * @param filename
	 *            The filename of the file to open
	 * @param mode
	 *            The mode how to open that file (readable (r), writeable (rw))
	 * @return The filedescriptor
	 * @author Lacki
	 */
	private RandomAccessFile openFile(String filename, String mode) {

		try {
			
			if (mode.equals("rw")) {
				File tmpFile = new File(filename);
				tmpFile.delete();
			}
			
			RandomAccessFile objectFile = new RandomAccessFile(filename, mode);

			// sets the class property writeable to false or true respectively
			if (mode.equals("r")) {
				this.writeable = false;
			} else {
				this.writeable = true;
			}

			return objectFile;

		} catch (IOException io) {
			System.out.println("Cannot open objectFile");
			return null;
		}
	}

}
