package compiler;

import static compiler.Ident.TokenID.STRONG_SYM_BB;
import static compiler.Ident.TokenID.STRONG_SYM_CB;
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
import static compiler.Ident.TokenID.*;
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
	private static int countTLBRACES;

	static Ident currentToken = new Ident();

	//Liste von tokens, die bei jedem Zeilenende an den Code Generator weitergegeben werden
	static ArrayList<Ident> tokenList = new ArrayList<Ident>();

	public static void main(String[] args) {
		@SuppressWarnings("unused")
		Scanner scanny = new Scanner();
		Scanner
				.importSource("/folk/rgratz/share/docu/uni/compiler/ws/compiler/src/examples/SynErrors.java");
		program();
//		while (true){
//			nextToken();
//			if (currentToken.type == TEOF)
//				break;
//		}
	}

	/**
	 * Fetch next Token from Scanner.
	 * The static currentToken variable does always contain the current token identifier.
	 * 
	 */
	private static void nextToken() {
		currentToken = Scanner.getSym();
		debug("Current Token: " + currentToken.type + " line: " +
				currentToken.lineNumber);

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

			/* Error Handling week Errors (Missing any kind of Brackets, ...)
			 * missing token will be inserted
			 */
			if (l == ErrorLevel.WEEK) {
				System.out.println("Warning: missing symbol: " + expectedID +
						" in line " + currentToken.lineNumber);

				if (expectedID == TSEMICOLON)
					generateCode();
				else
					tokenList.add(new Ident(expectedID)); // insert missing Identifier into code Generation List

				nextToken = false; //dont fetch next token on Week Error !
			}
			// Strong Error Level
			else {
				syntaxError("Error: mismatch token: " +
						currentToken.type.toString() + " expected: " +
						expectedID.toString() + " at line: " +
						currentToken.lineNumber);
			}
		}

		if (currentToken.type == TSEMICOLON)
			generateCode(); // This can work here, because when semicolon is missing, it will be inserted !
		else {
			if (nextToken)
				tokenList.add(currentToken); // dont add Semicolon
		}
		// Hack for Body block, when only two } are missing - end body block
		if (currentToken.type == TLBRACES)
			countTLBRACES = +1;
		else if (currentToken.type == TRBRACES)
			countTLBRACES = -1;

		// fetch next token when no Token is missing
		if (nextToken) {
			nextToken();
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
		int i = 0;
		String print = "     SENTENCE: ";
		while (i < tokenList.size()) {
			print = print.concat((new Ident(tokenList.get(i).type).type
					.toString() + " "));
			i++;
		}
		debug(print);
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
				packageImport(); // optional 0,n
			} catch (IllegalTokenException e) {
				while (currentToken.type.ordinal() < STRONG_SYM_CB.ordinal())
					nextToken();
				e.printStackTrace();
			}

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

	/* Error Handling in classBlock:
	 * When something goes wrong in here, he always searches for the next "public" token !
	 */
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
			while (currentToken.type != TPUBLIC)
				nextToken();
			classBlock();
		}
	}

	private static void methodDeclaration() throws IllegalTokenException {
		debug("methodDeclaration");
		expect(TPUBLIC);
		expectWeek(TSTATIC);

		if (currentToken.type == TVOID)
			expect(TVOID);
		else if (currentToken.type.startSetDataType()) // TODO: improve-> Object is also Ident, but Object starts always with a upper case letter
			dataType(); // return type
		else
			syntaxError("Illegal Method Declaration, not return type. Token: " +
					currentToken.type.toString() + "not valid. in line:" +
					currentToken.lineNumber);

		expect(TSIDENT);
		expectWeek(TLPAREN);
		if (currentToken.type.startSetSimpleDeclaration()) { // if dataTypeDescriptor (startSet is dataType )
			dataTypeDescriptor();
			while (currentToken.type == TCOMMA ||
					currentToken.type.startSetSimpleDeclaration()) {
				expectWeek(TCOMMA);
				dataTypeDescriptor();
			}
		}
		expectWeek(TRPAREN);
		expectWeek(TLBRACES);
		bodyBlock();
		expectWeek(TRBRACES);
	}

	private static void objectDeclaration() throws IllegalTokenException {
		debug("objectDeclaration");
		debug("ObjectDeclaration");
		object();
		objectDeclarationSuffix();
		expectWeek(TSEMICOLON);
	}

	private static void simpleDeclaration() throws IllegalTokenException {
		debug("simpleDeclaration");
		if (currentToken.type.startSetPrimitive())
			primitiveDeclaration();
		else if (currentToken.type.startSetPrimitiveArray())
			primitiveArrayDeclaration();
		else if (currentToken.type == TSTRING)
			stringDeclaration();
		else if (currentToken.type == TSTRING_ARRAY)
			stringArrayDeclaration();
		expectWeek(TSEMICOLON);
	}

	private static void primitiveDeclaration() throws IllegalTokenException {
		debug("primitiveDeclaration");
		primitive();
		identifier();
		if (currentToken.type == TEQL)
			assignmentSuffix();
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
		debug("objectDeclarationAssignmentMethodCall");
		object();
		if (currentToken.type == TEQL || currentToken.type == TSIDENT ||
				currentToken.type == TLBRACK)
			arrayDeclarationSuffix();
		if (currentToken.type == TLPAREN)
			methodCallSuffix();
		expectWeek(TSEMICOLON);
	}

	private static void objectDeclarationSuffix() throws IllegalTokenException {
		debug("objectDeclarationSuffix");
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
					" in objectDeclaration" + currentToken.lineNumber);
		}
	}

	private static void assignmentSuffix() throws IllegalTokenException {
		debug("assignmentSuffix");
		expect(TEQL);
		expression();
	}

	private static void methodCallSuffix() throws IllegalTokenException {
		debug("methodCallSuffix");
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
		debug("arrayDeclarationSuffix");
		if (currentToken.type == TLBRACK)
			arraySelector();
		if (currentToken.type == TSIDENT)
			objectDeclarationSuffix();
		if (currentToken.type == TEQL)
			assignmentSuffix();
	}

	private static void bodyBlock() {
		debug("bodyBlock");
		while (true) {
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
				else if (currentToken.type == TRBRACES) {
					if (countTLBRACES <= 2)
						return;
				} else if (currentToken.type.ordinal() < STRONG_SYM_CB
						.ordinal())
					nextToken();
				else {
					debug("END BODY BLOCK");
					break;
				}
			} catch (IllegalTokenException e) {
				e.printStackTrace();
				// goto next Sync Token and retry statement
				while (currentToken.type.ordinal() < STRONG_SYM_BB.ordinal())
					nextToken();
				bodyBlock();
			}
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
		debug("ifStatement");
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
		debug("returnStatement");
		expect(TRETURN);
		expression();
		expectWeek(TSEMICOLON);
	}

	private static void dataTypeDescriptor() throws IllegalTokenException {
		debug("dataTypeDescriptor");
		dataType();
		identifier();
		if (currentToken.type == TLBRACK)
			arraySelector();
	}

	private static void expression() throws IllegalTokenException {
		debug("expression");
		term();
		while (currentToken.type == TAND || currentToken.type == TOR) {
			if (currentToken.type == TAND)
				expect(TAND);
			else if (currentToken.type == TOR)
				expect(TOR);
			term();
		}
	}

	private static void term() throws IllegalTokenException {
		debug("term");
		factor();
		while (currentToken.type == TPLUS || currentToken.type == TMINUS) {
			factor();
			while (currentToken.type == TPLUS || currentToken.type == TMINUS) {
				if (currentToken.type == TPLUS)
					expect(TPLUS);
				else if (currentToken.type == TMINUS)
					expect(TMINUS);
				factor();
			}
		}
	}

	private static void factor() throws IllegalTokenException {
		debug("factor");
		value();
		while (currentToken.type == TMULT || currentToken.type == TDIV ||
				currentToken.type == TMOD) {
			if (currentToken.type == TMULT)
				expect(TMULT);
			else if (currentToken.type == TDIV)
				expect(TDIV);
			else if (currentToken.type == TMOD)
				expect(TMOD);
			value();
		}
	}

	private static void value() throws IllegalTokenException {
		debug("value");
		if (currentToken.type == TSIDENT) {
			identifier();
			if (currentToken.type == TLBRACK)
				arraySelector();
			if (currentToken.type == TLPAREN) {
				methodCallSuffix();
			}
		} else if (currentToken.type == TMINUS || currentToken.type == TNUMBER)
			intValue();
		else if (currentToken.type == TCHAR_VALUE)
			expect(TCHAR_VALUE);
		else if (currentToken.type == TTRUE || currentToken.type == TFALSE)
			booleanValue();
		else if (currentToken.type == TSTRING_VALUE)
			expect(TSTRING_VALUE);
		else if (currentToken.type == TNULL)
			expect(TNULL);
		else if (currentToken.type == TNOT) {
			expect(TNOT);
			value();
		}
	}

	private static void condition() throws IllegalTokenException {
		debug("condition");
		expression();
		if (currentToken.type == TEQL) {
			expect(TEQL);
			expect(TEQL);
		} else if (currentToken.type == TNOT) {
			expect(TNOT);
			expect(TEQL);
		} else if (currentToken.type == TGTR) {
			expect(TGTR);
		} else if (currentToken.type == TLEQ) {
			expect(TLEQ);
		} else if (currentToken.type == TLSS) {
			expect(TLSS);
		} else if (currentToken.type == TGEQ) {
			expect(TGEQ);
		}
		expression();
	}

	private static void intValue() throws IllegalTokenException {
		debug("intValue");
		if (currentToken.type == TMINUS)
			expect(TMINUS);
		expect(TNUMBER);
	}

	private static void booleanValue() throws IllegalTokenException {
		debug("booleanValue");
		if (currentToken.type == TTRUE)
			expect(TTRUE);
		else if (currentToken.type == TFALSE)
			expect(TFALSE);
		else
			syntaxError("Wrong token " + currentToken.type.toString() +
					", boolean Value expected, at line: " +
					currentToken.lineNumber);
	}

	private static void primitive() throws IllegalTokenException {
		debug("primitive");
		if (currentToken.type == TINT)
			expect(TINT);
		else if (currentToken.type == TBOOL)
			expect(TBOOL);
		else if (currentToken.type == TCHAR)
			expect(TCHAR);
		else
			syntaxError("Wrong token " + currentToken.type.toString() +
					", primitive datatype expected, at line: " +
					currentToken.lineNumber);
	}

	private static void primitiveArray() throws IllegalTokenException {
		debug("primitiveArray");
		if (currentToken.type == TINT_ARRAY)
			expect(TINT_ARRAY);
		else if (currentToken.type == TBOOL_ARRAY)
			expect(TBOOL_ARRAY);
		else if (currentToken.type == TCHAR_ARRAY)
			expect(TCHAR_VALUE);
		else
			syntaxError("Wrong token " + currentToken.type.toString() +
					", primitive Array datatype expected, at line: " +
					currentToken.lineNumber);
	}

	private static void object() throws IllegalTokenException {
		debug("object");
		expect(TSIDENT);
	}

	private static void dataType() throws IllegalTokenException {
		debug("dataType");
		if (currentToken.type.startSetPrimitive())
			primitive();
		else if (currentToken.type.startSetPrimitiveArray())
			primitiveArray();
		else if (currentToken.type == TSTRING)
			expect(TSTRING);
		else if (currentToken.type == TSTRING_ARRAY)
			expect(TSTRING_ARRAY);
		else if (currentToken.type == TSIDENT)
			object();
		else
			syntaxError("Wrong token " + currentToken.type.toString() +
					", datatype expected at line: " + currentToken.lineNumber);
	}

	static private void identifier() throws IllegalTokenException {
		expect(TSIDENT);
		while (currentToken.type == TDOT) {
			expect(TSIDENT);
		}
	}

	private static void arraySelector() throws IllegalTokenException {
		debug("arraySelector");
		expect(TLBRACK);
		if (currentToken.type.startSetExpression())
			expression();
		expectWeek(TRBRACK);

	}
}
