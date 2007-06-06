package compiler;

import static compiler.Ident.TokenID.TAND;
import static compiler.Ident.TokenID.TBOOL;
import static compiler.Ident.TokenID.TBOOL_ARRAY;
import static compiler.Ident.TokenID.TCHAR;
import static compiler.Ident.TokenID.TCHAR_ARRAY;
import static compiler.Ident.TokenID.TCHAR_VALUE;
import static compiler.Ident.TokenID.TCLASS;
import static compiler.Ident.TokenID.TCOMMA;
import static compiler.Ident.TokenID.TDIV;
import static compiler.Ident.TokenID.TDOT;
import static compiler.Ident.TokenID.TELSE;
import static compiler.Ident.TokenID.TEOF;
import static compiler.Ident.TokenID.TEQL;
import static compiler.Ident.TokenID.TERROR;
import static compiler.Ident.TokenID.TFALSE;
import static compiler.Ident.TokenID.TGTR;
import static compiler.Ident.TokenID.TIF;
import static compiler.Ident.TokenID.TIMPORT;
import static compiler.Ident.TokenID.TINT;
import static compiler.Ident.TokenID.TINT_ARRAY;
import static compiler.Ident.TokenID.TLBRACES;
import static compiler.Ident.TokenID.TLBRACK;
import static compiler.Ident.TokenID.TLPAREN;
import static compiler.Ident.TokenID.TLSS;
import static compiler.Ident.TokenID.TMINUS;
import static compiler.Ident.TokenID.TMOD;
import static compiler.Ident.TokenID.TMULT;
import static compiler.Ident.TokenID.TNEW;
import static compiler.Ident.TokenID.TNOT;
import static compiler.Ident.TokenID.TNULL;
import static compiler.Ident.TokenID.TNUMBER;
import static compiler.Ident.TokenID.TOR;
import static compiler.Ident.TokenID.TPACKAGE;
import static compiler.Ident.TokenID.TPLUS;
import static compiler.Ident.TokenID.TPUBLIC;
import static compiler.Ident.TokenID.TRBRACES;
import static compiler.Ident.TokenID.TRBRACK;
import static compiler.Ident.TokenID.TRETURN;
import static compiler.Ident.TokenID.TRPAREN;
import static compiler.Ident.TokenID.TSEMICOLON;
import static compiler.Ident.TokenID.TSIDENT;
import static compiler.Ident.TokenID.TSTATIC;
import static compiler.Ident.TokenID.TSTRING;
import static compiler.Ident.TokenID.TSTRING_ARRAY;
import static compiler.Ident.TokenID.TSTRING_VALUE;
import static compiler.Ident.TokenID.TTRUE;
import static compiler.Ident.TokenID.TVOID;
import static compiler.Ident.TokenID.TWHILE;
import static compiler.Util.debug;

import java.util.ArrayList;

import compiler.Ident.TokenID;

/**
 * Start Compiling 
 * 		-> fetch token from Scanner 
 * 		-> initiazile Code Generation
 * 
 * @author wondn ruap
 */
public class Parser {

	/* When set true Code Generation will be performed.
	* Code Generation will stop on any parsing error!
	*/
	static boolean setCodeGeneration = true;
	static Ident currentToken = new Ident();

	String test = new String("hallo");

	//Liste von tokens, die bei jedem Zeilenende an den Code Generator weitergegeben werden
	static ArrayList<Ident> tokenList = new ArrayList<Ident>();

	public static void main(String[] args) {
		@SuppressWarnings("unused")
		Scanner scanny = new Scanner();
		Scanner
				.importSource("/folk/rgratz/share/docu/uni/compiler/ws/compiler/src/compiler/Quicksort.java");
		program();
	}

//	@SuppressWarnings("serial")
//	public class IllegalTokenException extends RuntimeException {
//		public IllegalTokenException(String s) {
//			super(s);
//		}
//	}

	/**
	 * Fetch next Token from Scanner.
	 * The static currentToken variable does always contain the current token identifier.
	 * 
	 */
	private static void nextToken() {
		currentToken = Scanner.getSym();
		debug("Current Token: " + currentToken.type);

	}

	/**
	 * To ensure the correctness of the next token.
	 * 
	 * Week error handling:
	 * Mssing tokens which are expected will be inserted, and a warning will be printed. 
	 * 
	 * Strong Error detection:
	 * wrong token comes 
	 * 		=> Syntax Error. This means:
	 * 			=> goto next Strong Symbol and continue parsing
	 * 			=> Code generation will stop ! 
	 * 
	 * @param expectedID
	 * @throws IllegalTokenException 
	 */
	private static void expect(ErrorLevel l, TokenID expectedID)
			throws IllegalTokenException {
		boolean nextToken = true;
		if (currentToken.type != expectedID) {
			if (l == ErrorLevel.WEEK) { // Error Handling week Errors (Missing any kind of Brackets, ...)
				System.out.println("Warning: missing symbol: " + expectedID);
				tokenList.add(new Ident(expectedID)); // insert missing Identifier
				nextToken = false; //dont fetch next token when "missing Brackets Error" is true !
			} else {
				syntaxError("Error: mismatch token: " +
						currentToken.type.toString() + " expected: " +
						expectedID.toString() + " at line: " +
						currentToken.line_number);
			}
		} else {
			tokenList.add(currentToken);
		}
		if (nextToken) {
			nextToken();
			if (currentToken.type == TSEMICOLON) {
				generateCode();
				nextToken();
			}
		}
		if (currentToken.type == TERROR) {
			syntaxError("Error: Token: \"Error\" received, thats not good !");
		}
	}

	private static void expect(TokenID id) throws IllegalTokenException {
		expect(ErrorLevel.STRONG, id);
	}

	private static void expectWeek(TokenID id) throws IllegalTokenException {
		expect(ErrorLevel.WEEK, id);
	}

	private static enum ErrorLevel {
		WEEK, STRONG;
	}

	/**
	 * Start Code generation for listed tokens in the global Variable: tokenList.
	 * 
	 * @return null
	 */
	private static void generateCode() {

		tokenList.clear();
	}

	/**
	 * Continue parsing on Syntax Errors, but stop Code Generation !
	 * 
	 */
	private static void syntaxError(String string) throws IllegalTokenException {
		setCodeGeneration = false;
		throw new IllegalTokenException(string);
	}

	/*********** START OF PRODUCTION RULES: *********************/

	static private void program() {
		debug("Method: program");
		nextToken(); // initialization

		if (currentToken.type.equals(TPACKAGE))
			try {
				packageDeclaration();
			} catch (IllegalTokenException e1) {
				e1.printStackTrace();
			} // optional 1x

		while (currentToken.type.equals(TIMPORT))
			try {
				packageImport();
			} catch (IllegalTokenException e) {
				e.printStackTrace();
			} // optional 0,n

		classDeclaration(); // continue parsing even on error
	}

	static private void packageDeclaration() throws IllegalTokenException {
		expect(TPACKAGE);
		identifier();
		expectWeek(TSEMICOLON);
	}

	static private void packageImport() throws IllegalTokenException {
		expect(TIMPORT);
		identifier();
		expectWeek(TSEMICOLON);
	}

	static private void classDeclaration() {
		debug("Class declaration");
		try {
			expectWeek(TPUBLIC);
			expectWeek(TCLASS);
			expect(TSIDENT);
			expectWeek(TLBRACES);
			classBlock();
			expectWeek(TRBRACES);
			expectWeek(TEOF);
		} catch (IllegalTokenException e) {
			e.printStackTrace();
		}

	}

	// hm... method must not have a token
	private static void classBlock() {
		debug("ClassBlock");
		try {
			while (currentToken.type == TSIDENT ||
					currentToken.type.startSetSimpleDeclaration()) {
				if (currentToken.type == TSIDENT)
					objectDeclaration();
				if (currentToken.type.startSetSimpleDeclaration())
					simpleDeclaration();
			}
			while (currentToken.type == TPUBLIC)
				methodDeclaration();

		} catch (IllegalTokenException e) {
			e.printStackTrace();
			classBlock();
		}
	}

	private static void methodDeclaration() throws IllegalTokenException {
		expect(TPUBLIC);
		expectWeek(TSTATIC);

		if (currentToken.type == TVOID)
			expect(TVOID);
		else if (currentToken.type.startSetDataType())
			dataType(); // return type
		else
			syntaxError("Illegal Method Declaration, not return type. Token: " +
					currentToken.type.toString() + "not valid. in line:" +
					currentToken.line_number);

		expect(TSIDENT);
		expectWeek(TLPAREN);
		if (currentToken.type.startSetDataType()) { // if dataTypeDescriptor (startSet is dataType )
			dataTypeDescriptor();
			while (currentToken.type == TCOMMA ||
					currentToken.type.startSetDataType()) {
				expectWeek(TCOMMA);
				dataTypeDescriptor();
			}
		}
		expectWeek(TRPAREN);
		expectWeek(TLBRACES);
		bodyBlock();
		expectWeek(TLBRACES);
	}

	private static void objectDeclaration() throws IllegalTokenException {
		debug("ObjectDeclaration");
		object();
		objectDeclarationSuffix();
	}

	private static void simpleDeclaration() throws IllegalTokenException {
		debug("SimpleDeclaration");
		if (currentToken.type.startSetPrimitive())
			primitiveDeclaration();
		if (currentToken.type.startSetPrimitiveArray())
			primitiveArrayDeclaration();
		if (currentToken.type == TSTRING)
			stringDeclaration();
		if (currentToken.type == TSTRING_ARRAY)
			stringArrayDeclaration();
	}

	private static void primitiveDeclaration() throws IllegalTokenException {
		primitive();
		identifier();
		if (currentToken.type == TEQL)
			assignmentSuffix();
		expectWeek(TSEMICOLON);
	}

	private static void primitiveArrayDeclaration()
			throws IllegalTokenException {
		debug("primitiveArrayDeclaration");
		primitiveArray();
		identifier();
		expectWeek(TEQL);
		expectWeek(TNEW);
		primitive();
		expectWeek(TLBRACK);
		expect(TNUMBER);
		expectWeek(TRBRACK);
		expectWeek(TSEMICOLON);
	}

	private static void stringDeclaration() throws IllegalTokenException {
		debug("stringDeclaration");
		expect(TSTRING);
		identifier();
		expectWeek(TEQL);
		expectWeek(TNEW);
		expectWeek(TSTRING);
		expectWeek(TLPAREN);
		if (currentToken.type == TSTRING_VALUE)
			expect(TSTRING_VALUE);
		expectWeek(TRPAREN);
	}

	private static void stringArrayDeclaration() throws IllegalTokenException {
		debug("stringArrayDeclaration");
		expect(TSTRING_ARRAY);
		identifier();
		expectWeek(TEQL);
		expectWeek(TNEW);
		expectWeek(TSTRING);
		expectWeek(TLBRACK);
		expect(TNUMBER);
		expectWeek(TRBRACK);
	}

	private static void objectDeclarationAssignmentMethodCall()
			throws IllegalTokenException {
		object();
		if (currentToken.type == TEQL || currentToken.type == TSIDENT ||
				currentToken.type == TLBRACK)
			arrayDeclarationSuffix();
		if (currentToken.type == TLPAREN)
			methodCallSuffix();
	}

	private static void objectDeclarationSuffix() throws IllegalTokenException {
		identifier();
		expectWeek(TEQL);
		expectWeek(TNEW);
		if (currentToken.type == TSIDENT) {
			object();
			if (currentToken.type == TLPAREN) {
				expect(TLPAREN);
				if (currentToken.type.startSetExpression()) {
					expression();
				}
				expect(TRPAREN);
			} else {
				expect(TLBRACK);
				expect(TNUMBER);
				expectWeek(TRBRACK);
			}
		} else if (currentToken.type.startSetPrimitive()) {
			primitive();
			expectWeek(TLBRACK);
			expectWeek(TNUMBER);
			expectWeek(TRBRACK);
		} else {
			syntaxError("Illelag token: " + currentToken.type.toString() +
					" in objectDeclaration" + currentToken.line_number);
		}
	}

	private static void assignmentSuffix() throws IllegalTokenException {
		expect(TEQL);
		expression();
	}

	private static void methodCallSuffix() throws IllegalTokenException {
		expect(TLPAREN);
		if (currentToken.type.startSetExpression())
			expression();
		while (currentToken.type == TCOMMA ||
				currentToken.type.startSetExpression()) {
			expectWeek(TCOMMA);
			expression();
		}
		expect(TRPAREN);
	}

	private static void arrayDeclarationSuffix() throws IllegalTokenException {
		if (currentToken.type == TLBRACK)
			arraySelector();
		if (currentToken.type == TSIDENT)
			objectDeclarationSuffix();
		if (currentToken.type == TEQL)
			assignmentSuffix();
	}

	private static void bodyBlock() {
		try {
			if (currentToken.type == TWHILE)
				whileStatement();
			else if (currentToken.type == TIF)
				ifStatement();
			else if (currentToken.type == TRETURN)
				returnStatement();
			else if (currentToken.type.startSetSimpleDeclaration())
				simpleDeclaration();
			else if (currentToken.type == TSIDENT)
				objectDeclarationAssignmentMethodCall();
			else
				syntaxError("Unknown Error in Body Block Statement. Token: " +
						currentToken.type.toString() + " in line: " +
						currentToken.line_number);
		} catch (IllegalTokenException e) {
			e.printStackTrace();
			bodyBlock();
		}
	}

	private static void whileStatement() throws IllegalTokenException {
		debug("whileStatement");
		expect(TWHILE);
		expectWeek(TLPAREN);
		condition();
		expectWeek(TRPAREN);
		expectWeek(TLBRACES);
		bodyBlock();
		expectWeek(TRBRACES);
	}

	private static void ifStatement() throws IllegalTokenException {
		expect(TIF);
		expectWeek(TLPAREN);
		condition();
		expectWeek(TRPAREN);
		expectWeek(TLBRACES);
		bodyBlock();
		expectWeek(TRBRACES);
		if (currentToken.type == TELSE) {
			expect(TELSE);
			expectWeek(TLBRACES);
			bodyBlock();
			expectWeek(TRBRACES);
		}
	}

	private static void returnStatement() throws IllegalTokenException {
		expect(TRETURN);
		expression();
	}

	private static void dataTypeDescriptor() throws IllegalTokenException {
		dataType();
		identifier();
		if (currentToken.type == TLBRACK)
			arraySelector();
	}

	private static void expression() throws IllegalTokenException {
		term();
		while (currentToken.type == TAND || currentToken.type == TOR) {
			if (currentToken.type == TAND)
				expect(TAND);
			if (currentToken.type == TOR)
				expect(TOR);
			term();
		}
	}

	private static void term() throws IllegalTokenException {
		factor();
		while (currentToken.type == TPLUS || currentToken.type == TMINUS) {
			factor();
			while (currentToken.type == TPLUS || currentToken.type == TMINUS) {
				if (currentToken.type == TPLUS)
					expect(TPLUS);
				if (currentToken.type == TMINUS)
					expect(TMINUS);
				factor();
			}
		}
	}

	private static void factor() throws IllegalTokenException {
		value();
		while (currentToken.type == TMULT || currentToken.type == TDIV ||
				currentToken.type == TMOD) {
			if (currentToken.type == TMULT)
				expect(TMULT);
			if (currentToken.type == TDIV)
				expect(TDIV);
			if (currentToken.type == TMOD)
				expect(TMOD);
			value();
		}
	}

	private static void value() throws IllegalTokenException {
		if (currentToken.type == TSIDENT) {
			identifier();
			if (currentToken.type == TLBRACK)
				arraySelector();
			if (currentToken.type == TLPAREN) {
				methodCallSuffix();
			}
		}
		if (currentToken.type == TMINUS || currentToken.type == TNUMBER)
			intValue();
		if (currentToken.type == TCHAR_VALUE)
			expect(TCHAR_VALUE);
		if (currentToken.type == TTRUE || currentToken.type == TFALSE)
			booleanValue();
		if (currentToken.type == TSTRING_VALUE)
			expect(TSTRING_VALUE);
		if (currentToken.type == TNULL)
			expect(TNULL);
		if (currentToken.type == TNOT) {
			expect(TNOT);
			value();
		}
	}

	private static void condition() throws IllegalTokenException {
		expression();
		if (currentToken.type == TEQL) {
			expect(TEQL);
			expect(TEQL);
			expression();
		}
		if (currentToken.type == TNOT) {
			expect(TNOT);
			expect(TEQL);
			expression();
		}

		if (currentToken.type == TGTR) {
			expect(TGTR);
			if (currentToken.type == TEQL) {
				expect(TEQL);
			}
			expression();
		}
		if (currentToken.type == TLSS) {
			expect(TLSS);
			if (currentToken.type == TEQL) {
				expect(TEQL);
			}
			expression();
		}
	}

	private static void intValue() throws IllegalTokenException {
		if (currentToken.type == TMINUS)
			expect(TMINUS);
		expect(TNUMBER);
	}

	private static void booleanValue() throws IllegalTokenException {
		if (currentToken.type == TTRUE)
			expect(TTRUE);
		else if (currentToken.type == TFALSE)
			expect(TFALSE);
		else
			syntaxError("Wrong token " + currentToken.type.toString() +
					", boolean Value expected, at line: " +
					currentToken.line_number);
	}

	private static void primitive() throws IllegalTokenException {
		if (currentToken.type == TINT)
			expect(TINT);
		else if (currentToken.type == TBOOL)
			expect(TBOOL);
		else if (currentToken.type == TCHAR)
			expect(TCHAR);
		else
			syntaxError("Wrong token " + currentToken.type.toString() +
					", primitive datatype expected, at line: " +
					currentToken.line_number);
	}

	private static void primitiveArray() throws IllegalTokenException {
		if (currentToken.type == TINT_ARRAY)
			expect(TINT_ARRAY);
		else if (currentToken.type == TBOOL_ARRAY)
			expect(TBOOL_ARRAY);
		else if (currentToken.type == TCHAR_ARRAY)
			expect(TCHAR_VALUE);
		else
			syntaxError("Wro token " + currentToken.type.toString() +
					", primitive Array datatype expected, at line: " +
					currentToken.line_number);
	}

	private static void object() throws IllegalTokenException {
		expect(TSIDENT);
	}

	private static void dataType() throws IllegalTokenException {
		if (currentToken.type.startSetPrimitive())
			primitive();
		else if (currentToken.type == TSTRING)
			expect(TSTRING);
		else if (currentToken.type == TSIDENT)
			object();
		else
			syntaxError("Wrong token " + currentToken.type.toString() +
					", datatype expected at line: " + currentToken.line_number);
	}

	static private void identifier() throws IllegalTokenException {
		expect(TSIDENT);
		while (currentToken.type == TDOT) {
			expect(TSIDENT);
		}
	}

	private static void arraySelector() throws IllegalTokenException {
		expect(TLBRACK);
		if (currentToken.type.startSetExpression())
			expression();
		expect(TRBRACK);

	}
}
