package compiler;

public class TypeDesc {

	
	private int form; // array, record (class) 0...record 1...array  2...primitive
	private int len; // arrays, number of lenght
	DataType base; // for arrays which type, int,char,String,boolean
	SymbolTableList fields;
	private int size;
	
	// constructor for arrays
	public TypeDesc(int form,int len,DataType base,SymbolTableList fields, int size){
		this.form=form;
		this.len=len;
		this.base=base;
		this.fields=fields;
		this.size=size;
	}
	
	// constructor for classes
	public TypeDesc(int form,SymbolTableList fields,int size){
		this.form=form;
		this.fields=fields;
		this.size=size;
	}
	
	// constructor for primitives
	public TypeDesc(int form, DataType base, int size){
		this.form=form;
		this.size=size;
		this.base=base;
	}
	
	public TypeDesc(){
		
	}
	
	public static enum DataType{
		TypeDesc, intT, boolT, charT, String;
	}
	
	public int getSize(){
		return this.size;
	}

	public DataType getBase() {
		return base;
	}

	
}
