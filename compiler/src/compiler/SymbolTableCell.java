package compiler;

/**
 * Representing a sym
 * 
 */
public class SymbolTableCell {

	private String name;
	private ClassType classType; // constante, variable, object(array,class) , procedure
	private TypeDesc type; // int, bool, char, String
	private String value;
	SymbolTableList methodSymbols;
	private int offset; //offset is negative , (wirth names it val)
	private int size; // in 4 bytes 

	public SymbolTableCell(String name, ClassType classType, TypeDesc type,
			String value, int offset, int size) {
		this.name = name;
		this.classType = classType;
		this.type = type;
		this.value=value;
		// create new symbol list for local variables
		if (classType == ClassType.method || classType == ClassType.array) {
			methodSymbols = new SymbolTableList();
		}
		this.offset = offset;
		this.size = size;

	}

	public SymbolTableCell() {

	}
	
	/**
	 * Needed for method declarations, because size has to be fixed up later
	 * 
	 * @param size
	 * @param offset
	 */
	public void fixSizeAndOffset(int size, int offset){
		this.size=size;
		this.offset=offset;
	}

	public static enum ClassType {
		var, // variable
		array, // type bei wirth wenn richtig verstanden
		method, // gibt keine procedures, 
	}

	public String getName() {
		return name;
	}

	public ClassType getClassType() {
		return classType;
	}

	public TypeDesc getType() {
		return type;
	}

	public int getSize() {
		return size;
	}

	public int getOffset() {
		return offset;
	}

	public String getValue() {
		return value;
	}

}
