

import static compiler.Ident.TokenID.TIDENT;
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
 * @author Gratz Rupert 1
 */
public class Parser {

	/* When set true Code Generation will be performed.
	 * Code Generation will stop on any parsing error!
	 */
	static boolean setCodeGeneration = true;
	static Ident currentToken = new Ident();

	//Liste von tokens, die bei jedem Zeilenende an den Code Generator weitergegeben werden
	static ArrayList<Ident> tokenList = new ArrayList<Ident>();

	public static void main(String[] args) {

		Scanner
				.importSource("/folk/rgratz/share/docu/uni/compiler/ws/compiler/src/compiler/Scanner.java");
		program();
	}

	/**
	 * Initial method in EBNF
	 */
	static private void program() {
		debug("Method: program");
		nextToken();

		if (currentToken.type.equals(TPACKAGE)) {
			packageDeclaration(); // optional 1x
			nextToken();
		}

		while (currentToken.type.equals(TIMPORT)) {
			packageImport(); // optional 0,n
			nextToken();
		}

		expect(TPUBLIC);
		classDeclaration();
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
		tokenList.add(currentToken);
		expect(TIDENT);

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
			syntaxError(currentToken.type.toString(), currentToken.line_number);

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

	private static void syntaxError(String string) {
		syntaxError(string, 0);
	}

}
