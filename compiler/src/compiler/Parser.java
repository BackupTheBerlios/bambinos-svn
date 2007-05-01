package compiler;

import compiler.Scanner;
import static compiler.Ident.TokenID.*;

/**
 * Start Compiling 
 * 		-> fetch token from Scanner 
 * 		-> initiazile Code Generation
 * 
 * @author Gratz Rupert
 */
public class Parser {

	/* When set true Code Generation will be performed.
	 * Code Generation will stop on any parsing error!
	 */
	static boolean setCodeGeneration = true;
	static Ident currentToken = new Ident();
	
	public static void main(String[] args) {

		Scanner.testSetID(TOR);
		program();
	}

	static void nextToken() {
		currentToken=Scanner.getSym();
	}

	static private void program() {
		nextToken();
		switch (currentToken.ident_type) {
		case TPACKAGE:
			packageDeclaration();
			break;
		case TIMPORT:
			packageImport();
			break;
		case TPUBLIC:
			classDeclaration();
			break;
		default:
			error("Illegal class header, at line: ", currentToken.line_number);
			classDeclaration(); // go on parsing
		}
	}

	static private void packageDeclaration() {

	}

	static private void packageImport() {

	}

	static private void classDeclaration() {

	}

	static private void error(String string, int line_number) {
		System.out.println(string + line_number);
		setCodeGeneration = false;
	}

}
