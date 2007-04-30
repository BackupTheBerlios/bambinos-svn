package compiler;

public class Ident {
	
	TokenID ident_number; // type
	String ident_value; // value
			
	int line_number; 
	
	public void setIdentValue(String id_value){
		ident_value = id_value;
	}
	
	public String getIdentValue(){
		return new String(ident_value);
	}
	
	
	
	public enum TokenID{
	
	TNULL ,
	TDIV, // "/"
	// MOD,
	TAND,   // "+"
	TPLUS,
	TMINUS,
	TOR,
	TEQL, // "=="
	TNEQ, // "!="
	TLSS, // "<"
	TGEQ,
	TLEQ,
	TGTR,
	
	TCOMMA, //","
	TCOLON, //":"
	TSEMICOLON, //";"
	
	TRPAREN, // "}"
	TRBRAK, // ")"
	TLPAREN, // "{"
	TLBRAK, // "("
	
	TNOT, // "!"
	
	/*
	 * laut Wirth:
	 * 
	 * Hier muss man zwischen week und strong symbols unterscheiden. Denn der
	 * Parser wird bei einem leichtem Fehler Klammer zu, Semikolon vergessen,
	 * weiterparsen
	 * 
	 * Bei einem Schwerem Fehler hoert der parser auf, bis das naechste Strong
	 * symbol kommt und faengt dann an wieder zu parsen.
	 * 
	 * Alle Strong Symbols fangen einfach bei einem hoeherem Integer Wert an.
	 * Man nennt das naechste strong symbol einen Synchronization point
	 * 
	 * strong symbols bei uns: class package import methods declaration (public
	 * methods call (in EBNF nachtragen) if while declarations
	 * (int,char,string,boolean) datatype_declaration (object) ??? datatype
	 * assignments ?
	 * 
	 * 
	 * 
	 */
	
	TIDENT
	
	
	}

}
