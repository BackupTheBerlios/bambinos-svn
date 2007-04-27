package compiler;

public class Scanner {
	
	
	static final int TOKEN_NULL = 0;
	static final int DIV = 3;
	//static final int MOD
	static final int AND = 5;
	static final int PLUS = 6;
	static final int MINUS = 7;
	static final int OR = 8;
	static final int EQL = 9;
	static final int NEQ = 10;
	static final int LSS = 11;
	static final int GEQ = 12;
	static final int LEQ = 13;
	static final int GTR = 14;
	
	static final int COMMA = 19;
	static final int COLON = 20;
	static final int SEMICOLON = 21;
	
	static final int RPAREN = 22;
	static final int RBRAK = 23;
	static final int LPAREN = 29;
	static final int LBRAK = 30;
	
	static final int NOT = 32;
	
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
	
	
	
	
	/** 
	 * Proposed Interface to Parser (see Niki Wirth):
	 * 
	 * Method get represents the actual scanner.
	 * write token in to global variable ident (integer wahrscheinlich)
	 * 
	 * Probleme:	-> Positiontracking fuer error handling
	 * 				-> ident muss type und auch eventuelle values ubergeben
	 * 				-> 
	 * 
	 * Vorschlag:
	 * 				-> entweder ident als objekt (klasse) anlegen mit den zugehoerigen Eigenschaften
	 * 				-> oder mehrere globale Integer oder was auch immer Variablen die aktuellen Typ, value, position, .. beeinhalten
	 * 
	 * 
	 * 
	 */
	public static void get(){
		
		//return null;
		int x=5;
		System.out.println("Hello");
		
	}
	
	/*
	 * what is the next one ?
	 * 
	 */
	public static void lookadhead(){
		
		
	}
	
	

}
