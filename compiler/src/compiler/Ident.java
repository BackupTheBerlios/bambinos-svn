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

	int lineNumber;

	public Ident(TokenID type, String value, int lineNumber) {
		super();
		this.type = type;
		this.value = value;
		this.lineNumber = lineNumber;
	}

	public Ident() {
		setIdentValue("");
	}

	public Ident(TokenID type) {
		this.type = type;
	}

	public void setIdentValue(String id_value) {
		value = id_value;
	}

	public String getIdentValue() {
		return new String(value);
	}

	public static enum TokenID {
		TNULL, // "null" 
		TERROR, // invalid Token 

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

		TSTRING_VALUE, TCHAR_VALUE,

		TNOT, // "!"
		TTRUE, // "true"
		TFALSE, // "false"

		TNUMBER, // number

		TSTATIC, TCLASS, TVOID, // are no Strong symbols, because we always start with public !

		TELSE, // "else"

		TNEW, // "new"

		// strong symbols

		TPACKAGE, // "package"
		STRONG_SYM_BB, // Symbols serving as Synchronisation points: BB...Body Block

		/** bodyblock SYNC points **/
		TWHILE, // "while"
		TIF, // "if"
		TRETURN, // return

		TINT, // "int"
		TBOOL, // "boolean"
		TCHAR, // "char"
		TSTRING, // "String"

		TINT_ARRAY, // "int[]"
		TBOOL_ARRAY, // "boolean[]"
		TCHAR_ARRAY, // "char[]"
		TSTRING_ARRAY, // "String[]"

		TSIDENT, // simple identifier

		TPRINT, // printStatement

		/** bodyblock SYNC points end**/

		STRONG_SYM_CB, // syn point CB.. Class Block
		TIMPORT, TPUBLIC,

		TEOF; // end of file

		public boolean startSetRelation() {
			switch (this) {
			case TEQL:
			case TNOT:
			case TGTR:
			case TLEQ:
			case TLSS:
			case TGEQ:
				return true;
			default:
				return false;
			}
		}

		// some startsets implemented as enum methods
		public boolean startSetDataType() {
			if (this.startSetSimpleDeclaration())
				return true;
			switch (this) {
			case TSIDENT:
				return true;
			default:
				return false;
			}
		}

		public boolean allBracketTypes() {
			switch (this) {
			case TRBRACES:
			case TLBRACES:
			case TRBRACK:
			case TLBRACK:
			case TRPAREN:
			case TLPAREN:
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
