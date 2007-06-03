package compiler;

import static compiler.Ident.TokenID.*;
import static compiler.Util.debug;

import java.util.ArrayList;

import compiler.Ident;
import compiler.Scanner;
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
		Scanner scanny = new Scanner();
		Scanner
				.importSource("/folk/rgratz/share/docu/uni/compiler/ws/compiler/src/compiler/Scanner.java");
		program();
	}

	//Start method in EBNF
	static private void program() {
		debug("Method: program");
		nextToken();

		if (currentToken.type.equals(TPACKAGE))
			packageDeclaration(); // optional 1x

		while (currentToken.type.equals(TIMPORT))
			packageImport(); // optional 0,n

		classDeclaration();
	}

	/*
	 * EBNF: packageDeclaration="package" identifier ;
	 * 
	 * optional 1x
	 */
	static private void packageDeclaration() {
		expect(TPACKAGE);
		identifier();
		expect(TSEMICOLON);
	}

	// optional 0,n 
	static private void packageImport() {
		expect(TIMPORT);
		identifier();
		expect(TSEMICOLON);
	}

	//obligat
	static private void classDeclaration() {
		expect(TPUBLIC);
		expect(TCLASS);
		expect(TSIDENT);
		expect(TLBRACES);
		classBlock();
		expect(TRBRACES);
		// no semicolon expected at class end
	}

	// obligat hm... method must not have a token
	private static void classBlock() {
		debug("ClassBlock");
		while (currentToken.type == TSIDENT ||
				currentToken.type.startSetSimpleDeclaration()) {
			if (currentToken.type == TSIDENT)
				objectDeclaration();
			if (currentToken.type.startSetSimpleDeclaration())
				simpleDeclaration();
		}
		while (currentToken.type == TPUBLIC)
			methodDeclaration();
	}

	private static void methodDeclaration() {
		expect(TPUBLIC);
		expect(TSTATIC);

		if (currentToken.type == TVOID)
			expect(TVOID);
		else if (currentToken.type.startSetDataType())
			dataType();
		else
			syntaxError("Illegal Method Declaration. Token: " +
					currentToken.type.toString() + "not valid. in line:" +
					currentToken.line_number);

		expect(TSIDENT);
		expect(TLPAREN);
		if (currentToken.type.startSetDataType()) { // if dataTypeDescriptor (startSet is dataType )
			dataTypeDescriptor();
			while (currentToken.type == TCOMMA) {
				dataTypeDescriptor();
			}
		}
		expect(TRPAREN);
		expect(TLBRACES);
		bodyBlock();
		expect(TLBRACES);
	}

	private static void objectDeclaration() {
		debug("ObjectDeclaration");
		object();
		objectDeclarationSuffix();
	}

	private static void simpleDeclaration() {
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

	private static void primitiveDeclaration() {
		primitive();
		identifier();
		if (currentToken.type == TEQL)
			assignmentSuffix();
	}

	private static void primitiveArrayDeclaration() {
		debug("primitiveArrayDeclaration");
		primitiveArray();
		identifier();
		expect(TEQL);
		expect(TNEW);
		primitive();
		expect(TLBRACK);
		expect(TNUMBER);
		expect(TRBRACK);
	}

	private static void stringDeclaration() {
		debug("stringDeclaration");
		expect(TSTRING);
		identifier();
		expect(TEQL);
		expect(TNEW);
		expect(TSTRING);
		expect(TLPAREN);
		if (currentToken.type == TSTRING_VALUE)
			expect(TSTRING_VALUE);
		expect(TRPAREN);
	}

	private static void stringArrayDeclaration() {
		debug("stringArrayDeclaration");
		expect(TSTRING_ARRAY);
		identifier();
		expect(TEQL);
		expect(TNEW);
		expect(TSTRING);
		expect(TLBRACK);
		expect(TNUMBER);
		expect(TRBRACK);
	}

	private static void objectDeclarationAssignmentMethodCall() {
		object();
		if (currentToken.type == TEQL || currentToken.type == TSIDENT ||
				currentToken.type == TLBRACK)
			arrayDeclarationSuffix();
		if (currentToken.type == TLPAREN)
			methodCallSuffix();
	}

	private static objectDeclarationSuffix() {
		identifier();
		expect(TEQL);
		expect(TNEW);
		if (currentToken.type == TSIDENT) {
			object();
			if (currentToken.type == TLPAREN) {
				expect(TLPAREN);
				if (currentToken.type.startSetExpression()) { // expression ist optional
					expression();
				}
				expect(TRPAREN);
			} else {
				expect(TLBRACK);
				expect(TNUMBER);
				expect(TRBRACK);
			}
		} else if (currentToken.type.startSetPrimitive()) {
			primitive();
			expect(TLBRACK);
			expect(TNUMBER);
			expect(TRBRACK);
		} else {
			syntaxError("Illeag token: " + currentToken.type.toString() +
					" in objectDeclaration", currentToken.line_number);
		}
	}

	private static void assignmentSuffix() {
		expect(TEQL);
		expression();
	}

	private static void methodCallSuffix() {
		expect(TLPAREN);
		if (currentToken.type.startSetExpression())
			expression();
		while (currentToken.type.startSetExpression())
			expect(TCOMMA);
		expression();
	}

	private static void arrayDeclarationSuffix() {
		if (currentToken.type == TLBRACK)
			arraySelector();
		if (currentToken.type == TSIDENT)
			objectDeclarationSuffix();
		if (currentToken.type == TEQL)
			assignmentSuffix();
	}

	private static void bodyBlock() {
		if (currentToken.type == TWHILE)
			whileStatement();
		if (currentToken.type == TIF)
			ifStatement();
		if (currentToken.type == TRETURN)
			returnStatement();
		if (currentToken.type.startSetSimpleDeclaration())
			simpleDeclaration();
		if (currentToken.type == TSIDENT)
			objectDeclarationAssignmentMethodCall();
	}

	private static void dataTypeDescriptor() {

	}

	private static void dataType() {

	}

	static private void identifier() {
		expect(TSIDENT);
		while (currentToken.type == TDOT) {
			expect(TSIDENT);
		}
	}

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
	 * To ensure the right, next token !
	 * If the wrong token comes => Syntax Error. This means:
	 * 		=> goto next Strong Symbol and continue parsing
	 * 		=> Code generation will stop ! 
	 * 
	 * @param expectedID
	 */
	private static void expect(TokenID expectedID) {
		if (currentToken.type != expectedID) {
			syntaxError("Expected: " + currentToken.type.toString(),
					currentToken.line_number);

		} else {
			tokenList.add(currentToken);
		}
		nextToken();
	}

	/**
	 * Start Code generation for the given sentence.
	 */
	private static void generateCode() {

		tokenList.clear();
	}

	/**
	 * Continue parsing on Syntax Errors, but stop Code Generation !
	 * 
	 */
	static private void syntaxError(String string, int line_number) {
		System.out.println("Syntax Error: " + string + line_number);
		setCodeGeneration = false;
	}

	private static void syntaxError(String string) {
		syntaxError(string, 0);
	}

}
