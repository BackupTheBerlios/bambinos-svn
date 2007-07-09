package compiler;

/**
 * Representing a sym
 * 
 */
public class SymbolTableCell {

	private String name;
	private ClassType classType; // constante, variable, object(array,class) , procedure
	private TypeDesc type; // int, bool, char, String
	// TODO remove private String value;
	SymbolTableList methodSymbols;
	private int proc; // program counter of methods (needed in opcode)
	private int offset; //offset is negative , (wirth names it val)
	private int size; // in 4 bytes
	private boolean globalScope;
	private int objectDesc; // a unique number that describes the type of the table-entry. 
	

	public SymbolTableCell(String name, ClassType classType, TypeDesc type,
			int offset, int size, boolean globalScope) {
		this.name = name;
		this.classType = classType;
		this.type = type;
		// create new symbol list for local variables
		if (classType == ClassType.method || classType == ClassType.array) {
			methodSymbols = new SymbolTableList();
		}
		this.offset = offset;
		this.size = size;
		this.globalScope=globalScope;
		this.objectDesc = 0;

	}

	public SymbolTableCell() {

	}
	public SymbolTableCell(ClassType classType,boolean scope,boolean createSublist) {
		this.classType=classType;
		this.globalScope=scope;
		if (createSublist){
			methodSymbols = new SymbolTableList();
		}
	}

	/**
	 * Needed for method declarations, because size has to be fixed up later
	 * 
	 * @param size
	 * @param offset
	 */
	public void fixSizeAndOffset(int size, int offset) {
		this.size = size;
		this.offset = offset;
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

	public int getProc() {
		return proc;
	}

	public void setProc(int proc) {
		this.proc = proc;
	}
	public boolean isGlobalScope() {
		return globalScope;
	}

	public void setGlobalScope(boolean globalScope) {
		this.globalScope = globalScope;
	}

	public void setClassType(ClassType classType) {
		this.classType = classType;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setType(String type) {
		if (type.equals("intT"))
			this.type=CodeGenerator.INTTYPE;
		if (type.equals("charT"))
			this.type=CodeGenerator.CHARTYPE;
		if (type.equals("boolT"))
			this.type=CodeGenerator.BOOLTYPE;
	}

	public void setSize(int size) {
		this.size = size;
	}
}
