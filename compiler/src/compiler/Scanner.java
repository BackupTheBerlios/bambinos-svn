package compiler;

import static compiler.Ident.TokenID.*;

import compiler.Ident.TokenID;

public class Scanner {
	
	
	
	
	
	
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
	
	// Ident und lookadhead als Objekt
	
	static Ident ident = new Ident();
	static Ident identLook = new Ident();
	
	void testSetID(TokenID tid){
		
		ident.ident_number=TAND;
	}
	
	
	public static Ident get(){
		
		return ident;
		
	}

	
	
	
	public static Ident lookadhead(){
		
		return identLook;
		
	}
	
	

}
