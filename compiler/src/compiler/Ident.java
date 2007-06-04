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
		TNULL, // "null" 
		TERROR,// invalid Token 
		
		TDIV, // "/"
		TMULT, // *
		TMOD, // modulo %
		TAND, // "&&"
		TOR, // "||"
		TPLUS, // "+" 
		TMINUS, // "-" 
		TEQL, // "="
		TLSS, // "<"
		TGEQ, // ">=" 
		TLEQ, // "<="
		TGTR, // ">"
		TCOMMA, // ","
		TDOT, TCOLON, // ":"
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

		TINT, // "int"
		TBOOL, // "boolean"
		TCHAR, // "character"
		TSTRING, // "String"

		TINT_ARRAY, // "int[]"
		TBOOL_ARRAY, // "boolean[]"
		TCHAR_ARRAY, // "char[]"
		TSTRING_ARRAY, // "String[]"

		TSTRING_VALUE, TCHAR_VALUE, 

		TTRUE, // "true"
		TFALSE, // "false"

		TNUMBER, // number

		STRONG_SYM, // Grenze

		TPACKAGE, // "package"
		TIMPORT, TPUBLIC, TSTATIC, TFINAL, TCLASS, TVOID, TSIDENT, // simple identifier
		TRETURN, // return
		TNEW, // "new"
		TIF, // "if"
		TELSE, // "else"
		TWHILE, // "while"

		TEOF; // end of file

		// some startsets implemented as enum methods
		public boolean startSetDataType() {
			switch (this) {
			case TSIDENT:
			case TINT:
			case TBOOL:
			case TCHAR:
				return true;
			default:
				return false;
			}
		}

		public boolean startSetExpression() {
			switch (this) {
			case TNOT:
			case TNUMBER:
			case TSIDENT:
			case TSTRING_VALUE:
			case TCHAR_VALUE:
			case TNULL:
			case TMINUS:
			case TTRUE:
			case TFALSE:
				return true;
			default:
				return false;
			}
		}

		public boolean startSetPrimitive() {
			switch (this) {
			case TINT:
			case TBOOL:
			case TCHAR:
				return true;
			default:
				return false;
			}
		}

		public boolean startSetPrimitiveArray() {
			switch (this) {
			case TINT_ARRAY:
			case TBOOL_ARRAY:
			case TCHAR_ARRAY:
				return true;
			default:
				return false;
			}

		}

		public boolean startSetSimpleDeclaration() {
			if (this.startSetPrimitive())
				return true;
			if (this.startSetPrimitiveArray())
				return true;
			switch (this) {
			case TSTRING:
			case TSTRING_ARRAY:
				return true;
			default:
				return false;
			}
		}

		public boolean startSetBodyBlock() {
			if (this.startSetSimpleDeclaration())
				return true;
			switch (this) {
			case TSIDENT:
			case TWHILE:
			case TIF:
			case TRETURN:
				return true;
			default:
				return false;
			}
		}
	}

}
