package compiler;


/**
 * Class Ident serves as interface for the Parser and Scanner communication.
 *  
 * TokenIDs are ordered. Week Symbols first, then Strong symbols. 
 * Placeholder STRONG_SYM is the boarder.
 * 
 * @author rgratz
 * 
 */
public class Ident {


	TokenID type; // type
	String value; // value

	int line_number;

	public void Ident() {
		setIdentValue("");
	}
	
	public void setIdentValue(String id_value) {
		value = id_value;
	}

	public String getIdentValue() {
		return new String(value);
	}


	public static enum TokenID {
		TNULL,
		TDIV, // "/"
		// MOD,
		TAND, // "&&"
		TOR, // "||"
		TPLUS, // "+" 
		TMINUS, // "-" 
		TEQL, // "="
		TNEQ, // "!="
		TLSS, // "<"
		TGEQ, // ">=" 
		TLEQ, // "<="
		TGTR, // ">"
		TCOMMA, // ","
		TCOLON, // ":"
		TSEMICOLON, // ";"

		TRPAREN, // right parenthesis ")"
		TLPAREN, // left parenthesis "("
		
		TRBRACK, // right square brackets "]"
		TLBRACK, // left square brackets "["
		
		TRBRACES, // right curly braces "}"
		TLBRACES, // left curly braces "{"
				
		TDQUOTE, // """
		TSQUOTE, // "'"
		TNOT, // "!"

		/*
		 * laut Wirth:
		 * 
		 * Hier muss man zwischen week und strong symbols unterscheiden. Denn
		 * der Parser wird bei einem leichtem Fehler e.g. (Klammer zu, Semikolon
		 * vergessen) weiterparsen
		 * 
		 * Bei einem Schwerem Fehler hoert der parser auf, bis das naechste
		 * Strong symbol kommt und faengt dann an wieder zu parsen.
		 * 
		 * Alle Strong Symbols fangen einfach bei einem hoeherem Integer Wert
		 * an. Bei Enum ordinal Zahl. Man nennt das naechste strong symbol einen
		 * Synchronization point
		 * 
		 * strong symbols bei uns: class package import methods declaration
		 * (public methods call (in EBNF nachtragen) if while declarations
		 * (int,char,string,boolean) datatype_declaration (object) ??? datatype
		 * assignments ?
		 * 
		 * 
		 * 
		 */

		STRONG_SYM, // Grenze
		TPACKAGE, // "package"
		TIMPORT, 
		TPUBLIC,
		TSTATIC,
		TFINAL,
		TCLASS,
		TIDENT, // identifier
		TPRIM, // primitive
		TRETURN, // return
		TNEW, // "new"
		TIF, 
		TELSE, 
		TWHILE,
		
		TEOF; // end of file

	}


}

