package compiler;
/**
 * Representing a sym
 * 
 */
public class SymbolTableCell{

	private String name;
	private ClassType classType; // constante, variable, type(glaube array), procedure
	private DataType type; // object: int, bool, char, String
	private int intValue; // wert
	private String stringValue;
	SymbolTableList methodSymbols;
	
	public SymbolTableCell(String name,ClassType classType, DataType type, int intValue, String stringValue){
		this.name=name;
		this.classType=classType;
		this.type=type;
		this.intValue=intValue;
		this.stringValue=stringValue;
		// create new symbol list for local variables
		if (classType == ClassType.method){
			methodSymbols= new SymbolTableList();
		}
			
		
	}
	
	public SymbolTableCell(){
		
	}
	
	public static enum ClassType{
		var, // variable
		array, // type bei wirt wenn richtig verstanden
		method, // gibt keine procedures
	}
	
	public static enum DataType{
		object, intT, boolT, charT, String;
	}
	
	public String getName() {
		return name;
	}

	public ClassType getClassType() {
		return classType;
	}


	public  DataType getType() {
		return type;
	}


	public int getIntValue() {
		return intValue;
	}
	
	public String getStringValue(){
		return stringValue;
	}


}
