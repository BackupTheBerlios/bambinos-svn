package compiler;

import java.util.ArrayList;

import compiler.Scanner;
import compiler.Ident.TokenID;
import static compiler.Ident.TokenID.*;

/**
 * Start Compiling 
 * 		-> fetch token from Scanner 
 * 		-> initiazile Code Generation
 * 
 * @author Gratz Rupert
 */
public class Parser {

	static Ident currentToken = new Ident();
	static boolean setCodeGeneration = true;

	//Liste von tokens, die bei jedem Zeilenende an den Code Generator weitergegeben werden
	static ArrayList<Ident> tokenList = new ArrayList<Ident>();

	public static void main(String[] args) {

		Scanner.testSetID(TOR); // nur zum Testen
		program();
	}

	static private void program() {
		nextToken();
		switch (currentToken.ident_type) {
		case TPACKAGE:
			packageDeclaration(); //optional 1x
			break;
		case TIMPORT:
			packageImport(); //optional 0,n
			break;
		case TPUBLIC:
			classDeclaration(); //obligat
			break;
		default:
			syntaxError("Illegal class header, at line: ",
					currentToken.line_number);
			classDeclaration(); // go on parsing
		}
	}

	/**
	 * EBNF: packageDeclaration="package" identifier ;
	 * 
	 */
	static private void packageDeclaration() {
		tokenList.add(currentToken);
		expect(TIDENT);
		expect(TSEMICOLON);
		// TODO sentence is complete, start with Code generation !		

	}

	static private void packageImport() {

	}

	static private void classDeclaration() {

	}

	/**
	 * Fetch next Token from Scanner.
	 * The static currentToken variable does always contain the current token identifier.
	 * 
	 */
	private static void nextToken() {
		currentToken = Scanner.getSym();
	}

	/**
	 * To ensure the right, next token !
	 * If the wrong token comes => Syntax Error, perform:
	 * 		=> goto next Strong Symbol and continue parsing
	 * 		=> Code generation will stop ! 
	 * 
	 * @param expectedID
	 */
	private static void expect(TokenID expectedID) {
		nextToken();
		if (currentToken.ident_type != expectedID) {
			syntaxError(currentToken.ident_type.toString(),
					currentToken.line_number);

		} else {
			tokenList.add(currentToken);
		}
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

}
