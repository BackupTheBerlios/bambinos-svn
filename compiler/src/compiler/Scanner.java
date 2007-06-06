package compiler;

import compiler.Ident.TokenID;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.EOFException;
import java.util.Vector;



public class Scanner {

	// Ident und lookadhead als Objekt
	static Ident ident = new Ident();
	static Ident identLook = new Ident();
	
	private static RandomAccessFile file;
	
	private static Byte currentByte;
	private static Byte nextByte;
	// nextNextByte is the byte next to nextByte
	private static Byte nextNextByte;
	
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
				identifier.type = TokenID.TEOF;
				symbolFound = true;
			// Numbers
			} else if((currentByte > 47) && (currentByte < 58)) {
				identifier = readNumberSymbol();
				symbolFound = true;
			// negative numbers
			} else if((currentByte == 45) && (nextByte > 47) && (nextByte < 58)) {
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
			// double quote: start of a string-value
			} else if(currentByte == 34) {
				identifier = readStringValue();
				symbolFound = true;
			// single quote: start of a char-value
			} else if(currentByte == 39) {
				identifier = readCharValue();
				symbolFound = true;
			// space
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
			} else if((currentByte == 46) && (charIsLetter(nextByte))) {
					identifier = readTextSymbol();
					symbolFound = true;
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
		
		// "!" 
		if(currentByte == 33) {
			currentIdentifier.type = TokenID.TNOT;	
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
		// "*"
		} else if(currentByte == 42) {
				currentIdentifier.type = TokenID.TMULT;
		// "%"
		} else if(currentByte == 37) {
				currentIdentifier.type = TokenID.TMOD;
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
				nextNextByte = file.readByte();
			}
				

			currentByte = nextByte;
			nextByte = nextNextByte;
			nextNextByte = file.readByte();
		} catch(EOFException eof) {
			nextNextByte = null;
		} catch(IOException io) {
			System.out.println("Error reading file: " + io.toString());	
		}
		
	}
	
	/**
	 * Analyzes a symbol, that starts with a digit != 0. If the symbol does not end with a correct follow-symbol (look at ebnf)
	 * it returns an error-symbol. Otherwise it's a number-symbol
	 * @return TNUmber
	 */
	public static Ident readNumberSymbol() {
		
		Ident currentIdentifier = new Ident();
		boolean endOfSymbol = false;
		boolean errorSymbol = true;
		String symbolValue = new String();
		
		byte[] followSymbol = {32, 33, 37, 38, 41, 42, 43, 44, 45, 47, 59, 60, 61, 62, 93, 124};
		
		
		
		while (endOfSymbol == false) {
			
			symbolValue = symbolValue + (char)(int)currentByte;
			
			if (charIsDigit(nextByte)) {
				
				readNextByte();
				
			} else {
			
				//// checks if the nextByte is part of the followSymbols
				for (int i = 0; i < followSymbol.length; i++) {
					if (nextByte == followSymbol[i]) {
						errorSymbol = false;
						endOfSymbol = true;
					} else {
						endOfSymbol = true;
					}
				}
				
			}
		}
			
		if (errorSymbol == false) {
			currentIdentifier.type = TokenID.TNUMBER;	
			currentIdentifier.setIdentValue(symbolValue);
		} else {
			currentIdentifier.type = TokenID.TERROR;
		}
		
		
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
		boolean errorSymbol = false;
		boolean symbolIsIdentifier = false;

		/* tests if the simpleIdentifier starts with a "."
		 * then we need to check if the next Symbol is a letter
		*/
		if (currentByte == 46) {
			symbolValue = symbolValue + (char)(int)currentByte;
			
			// if it's no letter, don't check for identifier but return a TDOT-Symbol
			if (charIsLetter(nextByte) == false) {
				endOfSymbol = true;
				currentIdentifier.type = TokenID.TDOT;
			} else {
				readNextByte();
				symbolIsIdentifier = true;
			}
				
		}
		
		// followSymbol definies the available symbols that can follow the identifier
		byte[] followSymbols = {61, 60, 62, 33, 32, 38, 124, 59, 40, 41, 91, 93, 44, 42, 47, 37, 43, 45};
		
		while (endOfSymbol == false) {

			symbolValue = symbolValue + (char)(int)currentByte;

			if (charIsLetter(nextByte)) {
				readNextByte();
				
			// if nextByte is a digit, the symbol must be a identifier and not a reserved symbol
			} else if (charIsDigit(nextByte)) {
				symbolIsIdentifier = true;
				readNextByte();
				
			// if nextByte is a dot (46), the symbol must be a identifier and not a reserved symbol
			} else if (nextByte == 46) {
				symbolIsIdentifier = true;
				endOfSymbol = true;
				//readNextByte();			
			// if nextByte is a bracket (91), the symbol must be a identifier and not a reserved symbol
			} else if ((nextByte == 91) && (nextNextByte == 93)){
				symbolIsIdentifier = false;
				endOfSymbol = true;
				readNextByte();		
				symbolValue = symbolValue + (char)(int)currentByte;
				readNextByte();
				symbolValue = symbolValue + (char)(int)currentByte;
			// if the nextByte is not one of the above, then the symbol ends
			} else {
				
				boolean followSymbolFound = false;
				//// checks if the nextByte is part of the followSymbols
				for (int i = 0; i < followSymbols.length; i++) {
					if (nextByte == followSymbols[i]) {
						followSymbolFound = true;
					}
				}
				
				if (followSymbolFound == false) {
					errorSymbol = true;
				} 
				
				endOfSymbol = true;
			}
				

		}

		if (errorSymbol == true) {
			currentIdentifier.type = TokenID.TERROR;
		} else {
			
			if (symbolIsIdentifier = false) {
				currentIdentifier.type = TokenID.TSIDENT;
			} else {
				currentIdentifier = getIdentifier(symbolValue);	
			}			
			
		}
		
		currentIdentifier.setIdentValue(symbolValue);
		
		return currentIdentifier;
	}
	
	/**
	 * Reads a text until the end-symbol " is reached
	 * @return Ident
	 */
	public static Ident readStringValue() {
		
		Ident currentIdentifier = new Ident();
		String symbolValue = new String();

		// read NextByte because the currentByteValue is "
		readNextByte();
		while (currentByte != 34) {
			
			symbolValue = symbolValue + (char)(int)currentByte;
			readNextByte();
		}
		
		currentIdentifier.type = TokenID.TSTRING_VALUE;
		currentIdentifier.setIdentValue(symbolValue);
		
		return currentIdentifier;
	}
	
	/**
	 * Reads a text until the end-symbol ' is reached
	 * The text must only be 1 symbol. Else an error is returned
	 * @return Ident
	 */
	public static Ident readCharValue() {
		
		Ident currentIdentifier = new Ident();
		
		// read NextByte because the currentByteValue is '
		readNextByte();
		
		// the currentByteValue is the value of the char and the next ByteValue must be '
		// otherwise an error is generated. 
		
		if (currentByte == 39) {
			currentIdentifier.type = TokenID.TCHAR_VALUE;
			currentIdentifier.setIdentValue("");
		} else if (nextByte == 39) {
			currentIdentifier.type = TokenID.TCHAR_VALUE;
			currentIdentifier.setIdentValue("" + (char)(int)currentByte);
			readNextByte();
		} else {
			currentIdentifier.type = TokenID.TERROR;
			currentIdentifier.setIdentValue("");
		}
		
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
		} else if (symbolValue.compareTo("return") == 0) {
			identifier.type = TokenID.TRETURN;
		} else if (symbolValue.compareTo("void") == 0) {
			identifier.type = TokenID.TVOID;
		} else if (symbolValue.compareTo("null") == 0) {
			identifier.type = TokenID.TNULL;
		} else if (symbolValue.compareTo("new") == 0) {
			identifier.type = TokenID.TNEW;
		} else if (symbolValue.compareTo("if") == 0) {
			identifier.type = TokenID.TIF;
		} else if (symbolValue.compareTo("else") == 0) {
			identifier.type = TokenID.TELSE;
		} else if (symbolValue.compareTo("while") == 0) {
			identifier.type = TokenID.TWHILE;
		} else if (symbolValue.compareTo("int") == 0) {
			identifier.type = TokenID.TINT;
		} else if (symbolValue.compareTo("int[]") == 0) {
			identifier.type = TokenID.TINT_ARRAY;
		} else if (symbolValue.compareTo("boolean") == 0) {
			identifier.type = TokenID.TBOOL;
		} else if (symbolValue.compareTo("bool[]") == 0) {
			identifier.type = TokenID.TBOOL_ARRAY;
		} else if (symbolValue.compareTo("char") == 0) {
			identifier.type = TokenID.TCHAR;
		} else if (symbolValue.compareTo("char[]") == 0) {
			identifier.type = TokenID.TCHAR_ARRAY;
		} else if (symbolValue.compareTo("String") == 0) {
			identifier.type = TokenID.TSTRING;
		} else if (symbolValue.compareTo("String[]") == 0) {
			identifier.type = TokenID.TSTRING_ARRAY;
		} else if (symbolValue.compareTo("true") == 0) {
			identifier.type = TokenID.TTRUE;
		} else if (symbolValue.compareTo("false") == 0) {
			identifier.type = TokenID.TFALSE;
		} else {
			identifier.type = TokenID.TSIDENT;
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
