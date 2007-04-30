package compiler;

public class Ident {
	
	int ident_number; // type
	String ident_value; // value
			
	int line_number; 
	
	public void setIdentValue(String id_value){
		ident_value = id_value;
	}
	
	public String getIdentValue(){
		return new String(ident_value);
	}
	
	
	
	
	
	public static final int TOKEN_NULL = 0;
	public static final int DIV = 3;
	//static final int MOD
	public static final int AND = 5;
	public static final int PLUS = 6;
	public static final int MINUS = 7;
	public static final int OR = 8;
	public static final int EQL = 9;
	public static final int NEQ = 10;
	public static final int LSS = 11;
	public static final int GEQ = 12;
	public static final int LEQ = 13;
	public static final int GTR = 14;
	
	public static final int COMMA = 19;
	public static final int COLON = 20;
	public static final int SEMICOLON = 21;
	
	public static final int RPAREN = 22;
	public static final int RBRAK = 23;
	public static final int LPAREN = 29;
	public static final int LBRAK = 30;
	
	public static final int NOT = 32;
	
	/* laut Wirth:
	 * 
	 * Hier muss man zwischen week und strong symbols unterscheiden.
	 * Denn der Parser wird bei einem leichtem Fehler Klammer zu, Semikolon vergessen, weiterparsen
	 * 
	 * Bei einem Schwerem Fehler hoert der parser auf, bis das naechste Strong symbol kommt und faengt dann an wieder zu parsen.
	 *  
	 * Alle Strong Symbols fangen einfach bei einem hoeherem Integer Wert an.
	 * Man nennt das naechste strong symbol einen Synchronization point
	 * 
	 * strong symbols bei uns:
	 * 					class
	 * 					package import
	 * 					methods declaration (public
	 * 	 				methods call (in EBNF nachtragen)
	 * 					if
	 * 					while
	 * 					declarations (int,char,string,boolean) datatype_declaration (object) ???
	 * 					datatype assignments ?
	 *
	 * 						
	 *  
	 */
	
	static final int IDENT = 100;
	
	
	

}
