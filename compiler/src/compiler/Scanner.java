package compiler;

import static compiler.Ident.TokenID.*;

import compiler.Ident.TokenID;

public class Scanner {

	// Ident und lookadhead als Objekt
	static Ident ident = new Ident();
	static Ident identLook = new Ident();

	static void testSetID(TokenID tid) {

		System.out.println("Ordinal number " + tid.ordinal());
	}

	/**
	 * Interface for Scanner Parser communication. 
	 * Returns the current token
	 * 
	 * @return IDENT
	 */
	public static Ident getSym() {

		ident.ident_type=TPACKAGE;
		return ident;

	}

	/**
	 * Interface for Scanner Parser communication.
	 * Returns the lookahead token
	 * 
	 * @return IDENT
	 */
	public static Ident lookahead() {

		return identLook;

	}

}
