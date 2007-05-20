package compiler;

import compiler.Ident.TokenID;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.EOFException;



public class Scanner {

	// Ident und lookadhead als Objekt
	static Ident ident = new Ident();
	static Ident identLook = new Ident();
	
	private static RandomAccessFile file;
	
	private static Byte currentByte;
	private static Byte nextByte;
	
		
	/**
	 * Initialization for Scanner. It opens a filehandle 
	 * @author lacki
	 * @param String filepath
	 * @return void
	 */
	static void importSource(String filename) {

		try {
			file = new RandomAccessFile(filename, "r");	
		} catch(FileNotFoundException fileNotFound) {
			System.out.println("File " + filename.toString() + " does not exist: " + fileNotFound.toString());
		}
		
	}
	
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

		Ident identifier = new Ident();
		boolean symbolFound = false;
		
		
		while (symbolFound == false) {
			
			readNextByte();
			
			if (currentByte == null) {
				identifier = null;
				symbolFound = true;
			// Numbers
			} else if((currentByte > 47) && (currentByte < 58)) {
				identifier = readNumberSymbol();
				symbolFound = true;
			// text	(capital letter)
			} else if((currentByte > 64) && (currentByte < 91)) {
				identifier = readTextSymbol();
				symbolFound = true;
				//System.out.println(identifier.getIdentValue() + " - " + identifier.type.toString());
			// text (small letter)	
			} else if((currentByte > 96) && (currentByte < 123)) {
				identifier = readTextSymbol();
				symbolFound = true;
				//System.out.println(identifier.getIdentValue() + " - " + identifier.type.toString());
			// double quote
			} else if(currentByte == 32) {
				//System.out.println("space - NOP");
			// tabulator
			} else if(currentByte == 9) {
				//System.out.println("tab - NOP");
			} else if(currentByte == 10) {
				//System.out.println("new line - NOP");
			// comment	
			} else if((currentByte == 47) &&  (nextByte == 47)) {
				passSinglelineComment();
			} else if((currentByte == 47) &&  (nextByte == 42)) {
				passMultilineComment();
			} else {
				identifier = readSymbol();
				//System.out.println("ident: " + ident.type.toString());
				
				if (identifier != null) {
					symbolFound = true;		
				}
					
			}				

		}
		return identifier;

	}
	
	public static void passMultilineComment() {
		// eoc...end of comment reached
		boolean eoc = false;
			
		while (eoc == false) {
				
			if ((currentByte == 42) && (nextByte == 47)) {
				eoc = true;
			}		
			readNextByte();
				
		}			
	}
	
	public static void passSinglelineComment() {
		while (nextByte != 10) {
			readNextByte();
		}
		
		
	}

	/**
	 * Analyzes a single-symbol and returns a identifier or null, if it's unknown
	 * @return Ident
	 */
	public static Ident readSymbol() {

		String symbolValue = new String();
		symbolValue = "";
		Ident currentIdentifier = new Ident();
		
		// "!" or "!="
		if(currentByte == 33) {
			
			if (nextByte == 61) {
				currentIdentifier.type = TokenID.TNEQ;
			} else {
				currentIdentifier.type = TokenID.TNOT;	
			}
			
		// double quote
		} else if(currentByte == 34) {
			currentIdentifier.type = TokenID.TDQUOTE;
		// single quote	
		} else if(currentByte == 39) {
			currentIdentifier.type = TokenID.TSQUOTE;
		// ";"
		} else if(currentByte == 59) {
			currentIdentifier.type = TokenID.TSEMICOLON;
		// "{"
		} else if(currentByte == 123) {
			currentIdentifier.type = TokenID.TLBRACES;
		// "}"
		} else if(currentByte == 125) {
			currentIdentifier.type = TokenID.TRBRACES;
		// "&&"
		} else if((currentByte == 38) && (nextByte == 38)) {
			currentIdentifier.type = TokenID.TAND;
			symbolValue = symbolValue + (char)(int)currentByte;
			readNextByte();
		// "("
		} else if(currentByte == 40) {
			currentIdentifier.type = TokenID.TLPAREN;
		// ")"
		} else if(currentByte == 41) {
			currentIdentifier.type = TokenID.TRPAREN;
		// "+"
		} else if(currentByte == 43) {
			currentIdentifier.type = TokenID.TPLUS;
		// ","
		} else if(currentByte == 44) {
			currentIdentifier.type = TokenID.TCOMMA;
		// "-"
		} else if(currentByte == 45) {
			currentIdentifier.type = TokenID.TMINUS;
		// "/"
		} else if(currentByte == 47) {
				currentIdentifier.type = TokenID.TDIV;
		// ":"
		} else if(currentByte == 58) {
			currentIdentifier.type = TokenID.TCOLON;
		// "="
		} else if(currentByte == 61) {
			currentIdentifier.type = TokenID.TEQL;
		// "["
		} else if(currentByte == 91) {
			currentIdentifier.type = TokenID.TLBRACK;
		// "]"
		} else if(currentByte == 93) {
			currentIdentifier.type = TokenID.TRBRACK;
		// "<" and "<="
		} else if(currentByte == 60) {
			
			if (nextByte == 61) {
				currentIdentifier.type = TokenID.TLEQ;
				symbolValue = symbolValue + (char)(int)currentByte;
				readNextByte();
			} else {
				currentIdentifier.type = TokenID.TLSS;
			}
		// ">" and ">="
		} else if(currentByte == 62) {
			
			if (nextByte == 61) {
				currentIdentifier.type = TokenID.TGEQ;
				symbolValue = symbolValue + (char)(int)currentByte;
				readNextByte();
			} else {
				currentIdentifier.type = TokenID.TGTR;
			}
		// "||"
		} else if((currentByte == 124) && (nextByte == 124)) {
			currentIdentifier.type = TokenID.TOR;
			symbolValue = symbolValue + (char)(int)currentByte;
			readNextByte();
		} else {
			System.out.println("unknown value: " + currentByte);
		}
		
		symbolValue = symbolValue + (char)(int)currentByte;
		
		//currentIdentifier.setIdentValue(symbolValue);
		return currentIdentifier;
	}
	
	/**
	 *  Reads the next byte from the source-file. 
	 */
	public static void readNextByte() {
		try {
			
			if (currentByte == null) {
				nextByte = file.readByte();
			}

			currentByte = nextByte;
			nextByte = file.readByte();
		} catch(EOFException eof) {
			nextByte = null;
		} catch(IOException io) {
			System.out.println("Error reading file: " + io.toString());	
		} 
	}
	
	/**
	 * Analyzes a symbol, that starts with a digit != 0. 
	 * This results in a number
	 * @return Ident
	 */
	public static Ident readNumberSymbol() {
		
		Ident currentIdentifier = new Ident();
		boolean endOfSymbol = false;
		
		while (endOfSymbol == false) {
			
			if (charIsDigit(nextByte)) {
				readNextByte();
			} else {
				endOfSymbol = true;
			}
			
		}
		
		currentIdentifier.type = TokenID.TIDENT;
		
		return currentIdentifier;
	}
	
	/**
	 * Analyzes a symbol, that starts with a Char
	 * This can result in an identifier or in a reserved keyword
	 * @return Ident
	 */
	public static Ident readTextSymbol() {
		
		Ident currentIdentifier = new Ident();
		
		String symbolValue = new String();
		boolean endOfSymbol = false;
		boolean symbolIsIdentifier = false;
		

		
		while (endOfSymbol == false) {

			symbolValue = symbolValue + (char)(int)currentByte;

			if (charIsLetter(nextByte)) {
				readNextByte();
				
			// if nextByte is a digit, the symbol must be a identifier and not a reserved symbol
			} else if (charIsDigit(nextByte)) {
				symbolIsIdentifier = true;
				readNextByte();
				
			// if nextByte is a dot (47), the symbol must be a identifier and not a reserved symbol
			} else if (nextByte == 46) {
				symbolIsIdentifier = true;
				readNextByte();			
			
			// if the nextByte is not one of the above, then the symbol ends
			} else {
				endOfSymbol = true;
			}
				

		}
		
		currentIdentifier = getIdentifier(symbolValue);
		return currentIdentifier;
	}
	
	/**
	 * Finds the TokenID of an identifier for a given String
	 * @param symbolValue The String to find the TokenID for
	 * @return Ident
	 */
	public static Ident getIdentifier(String symbolValue) {
		Ident identifier = new Ident();
		
		//identifier.setIdentValue(symbolValue);
		
		if (symbolValue.compareTo("package") == 0) {
			identifier.type = TokenID.TPACKAGE;
		} else if (symbolValue.compareTo("import") == 0) {
			identifier.type = TokenID.TIMPORT;
		} else if (symbolValue.compareTo("class") == 0) {
			identifier.type = TokenID.TCLASS;
		} else if (symbolValue.compareTo("public") == 0) {
			identifier.type = TokenID.TPUBLIC;
		} else if (symbolValue.compareTo("static") == 0) {
			identifier.type = TokenID.TSTATIC;
		} else if (symbolValue.compareTo("final") == 0) {
			identifier.type = TokenID.TFINAL;
		} else if (symbolValue.compareTo("return") == 0) {
			identifier.type = TokenID.TRETURN;
		} else if (symbolValue.compareTo("null") == 0) {
			identifier.type = TokenID.TNULL;
		} else if (symbolValue.compareTo("new") == 0) {
			identifier.type = TokenID.TNEW;
		} else if (symbolValue.compareTo("if") == 0) {
			identifier.type = TokenID.TIF;
		} else if (symbolValue.compareTo("then") == 0) {
			identifier.type = TokenID.TELSE;
		} else if (symbolValue.compareTo("else") == 0) {
			identifier.type = TokenID.TELSE;
		} else if (symbolValue.compareTo("while") == 0) {
			identifier.type = TokenID.TWHILE;
		} else {
			identifier.type = TokenID.TIDENT;
		}
		
		return identifier;
	}

	
	/**
	 * Checks if a Ascii-Value is a letter. Returns true if it is one, otherwise false
	 * @param character
	 * @return boolean 
	 */
	public static boolean charIsLetter(Byte character) {
		
		// if character-ascii-code is between 65 and 90 or 07 and 123, then it's a letter
		if ((character > 64) && (character < 91)) {
			return true;
		} else if ((character > 96) && (character < 123)) {
			return true;
		} else {
			return false;
		}
		
	}
	
	/**
	 * Checks if a Ascii-Value is a letter. Returns true if it is one, otherwise false
	 * @param character
	 * @return boolean 
	 */
	public static boolean charIsDigit(Byte character) {
		
		// if character-ascii-code is between 48 and 57, then it's a digit
		if ((character > 47) && (character < 58)) {
			return true;
		} else {
			return false;
		}
		
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
