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
import static compiler.Ident.TokenID.TGEQ;
import static compiler.Ident.TokenID.TGTR;
import static compiler.Ident.TokenID.TIF;
import static compiler.Ident.TokenID.TIMPORT;
import static compiler.Ident.TokenID.TINT;
import static compiler.Ident.TokenID.TINT_ARRAY;
import static compiler.Ident.TokenID.TLBRACES;
import static compiler.Ident.TokenID.TLBRACK;
import static compiler.Ident.TokenID.TLEQ;
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
import static compiler.Util.debug1;

import java.util.ArrayList;
import java.util.Vector;

import compiler.Ident.TokenID;
import compiler.SymbolTableCell.ClassType;
import compiler.Util.IllegalTokenException;
import compiler.Util.TypeErrorException;

/**
 * Start Compiling 
 * 		-> fetch token from Scanner 
 * 		-> initiazile Code Generation when sentence is complete. A sentence is complete when a semicolon is expected, or .... (if,while,...)
 * 
 * @author wondn ruap
 */
public class Parser {

	/* When set true Code Generation will be performed.
	* Code Generation will stop on any parsing error!
	*/
	static boolean setCodeGeneration = true;
	private static int countTLBRACES;
	private static CodeGenerator genCode;

	static Ident currentToken = new Ident();

	//Liste von tokens, die bei jedem Zeilenende an den Code Generator weitergegeben werden
	static ArrayList<Ident> tokenList = new ArrayList<Ident>();

	public static void main(String[] args) {
		@SuppressWarnings("unused")
		Scanner scanny = new Scanner();

		Scanner.importSource(args[0]);

		/* initialize Code Generator */
		genCode = new CodeGenerator();

		program();
//		while (true){
//			nextToken();
//			if (currentToken.type == TEOF)
//				break;
//		}

		CodeGenerator.symbolTable.printSymbolTable();

		System.out.println("ASSEMBLERCODE: ");

		CodeGenerator.write2File();

	}

	/**
	 * Fetch next Token from Scanner.
	 * The static currentToken variable does always contain the current token identifier.
	 * 
	 */
	private static void nextToken() {
		currentToken = Scanner.getSym();
		debug1("Current Token: " + currentToken.type + " line: " +
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
					// generateCode(); nein doch nicht, mache jedes einzeln da nicht immer Code generiert werden soll
					// sonder bei Deklarationen einfach ein Eintrag in die Symboltable erfolgt TODO 
					;
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

	private static void expectWeak(TokenID id) throws IllegalTokenException {
		expect(ErrorLevel.WEEK, id);
	}

	private static enum ErrorLevel {
		WEEK, STRONG;
	}

	/**
	 *  print Warning when Array List is not empty. And make the list empty. !
	 *  Should never happen !
	 * 
	 * 
	 */
	private static void arrayListEmpty(String method) {
		if (tokenList.isEmpty() == false) {
			System.out
					.println("INTERNAL ERROR IN Parser.java "+method+" ; The Parser Token List is not empty but it should be !! Empty the List now.");
			tokenList.clear();
		}
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
		debug1(print);
		tokenList.clear();
	}

	/**
	 * Adds a new Entry to the symbol table. This works like this:
	 * 
	 * Add all symbols. First class symbols will be added. When a method x declaration is added, 
	 * then the following symbols will be add in the sublist of the method x. When the next method y is added, 
	 * all following symbols will be added to the sublist of the method y.
	 * 
	 * @param name
	 * @param classType
	 * @param type
	 * @param intValue
	 * @param stringValue
	 * @param arrayElements (when no array set to 1)
	 */
	private static void add2SymTable(String name, ClassType classType,
			TypeDesc type, int arraySize) {
		int size = type.getSize() * arraySize;
		CodeGenerator.symbolTable.addSym(name, classType, type, size);
	}

	/**
	 * Continue parsing on Syntax Errors, but stop Code Generation !
	 * 
	 */
	public static void syntaxError(String string) throws IllegalTokenException {
		setCodeGeneration = false;
		throw new IllegalTokenException(string);
	}

	/*********** START OF PRODUCTION RULES: *********************/

	static private void program() {
		debug1("Method: program");
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
		expectWeak(TSEMICOLON);
	}

	static private void packageImport() throws IllegalTokenException {
		expect(TIMPORT);
		identifier();
		expectWeak(TSEMICOLON);
	}

	static private void classDeclaration() {
		debug1("Class declaration");
		try {
			expectWeak(TPUBLIC);
			expectWeak(TCLASS);
			expect(TSIDENT);
			expectWeak(TLBRACES);
			classBlock();
			expectWeak(TRBRACES);
			expectWeak(TEOF);
		} catch (IllegalTokenException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Error Handling in classBlock:
	 * When something goes wrong in here, he always searches for the next "public" token there the next method starts !
	 */
	private static void classBlock() {
		debug1("ClassBlock");
		try {
			while (currentToken.type == TSIDENT ||
					currentToken.type.startSetSimpleDeclaration()) {
				if (currentToken.type == TSIDENT)
					objectDeclaration();
				if (currentToken.type.startSetSimpleDeclaration())
					simpleDeclaration();
			}
			// add BSR instruction, programm needs jump to main
			int fixMainPC=CodeGenerator.methodCall(-100);
			
			while (currentToken.type == TPUBLIC)
				methodDeclaration();

			// fix main jump in opcode
			int proc=CodeGenerator.symbolTable.getSymbol("main").getProc();
			CodeGenerator.fixMainProc(fixMainPC, proc);
			
			
		} catch (IllegalTokenException e) {
			e.printStackTrace();
			while (currentToken.type != TPUBLIC)
				nextToken();
			classBlock();
		}
	}

	private static void methodDeclaration() throws IllegalTokenException {
		debug1("methodDeclaration");
		arrayListEmpty("methodDeclaration");
		expect(TPUBLIC);
		expectWeak(TSTATIC);
		int verifyReturnType = 0;
		TypeDesc returnType; // TODO weiss noch nicht ob man den Rueckgabewert ueberhaupt pruefen sollen, overkill ???

		if (currentToken.type == TVOID) {
			expect(TVOID);
			verifyReturnType++;
		} else if (currentToken.type.startSetDataType()) {
			returnType = dataType(); // return type
			verifyReturnType++;
		} else
			syntaxError("Illegal Method Declaration, Return Type not valid. Token: " +
					currentToken.type.toString() +
					"not valid. in line:" +
					currentToken.lineNumber);

		// method Name
		String name = currentToken.value;
		if (name.equals("main")) {
			CodeGenerator.setMainPC();
		}
		if (currentToken.type == TSIDENT) {
			expect(TSIDENT);
			verifyReturnType++;
		}

		// Frage was vergisst man mehr Return Type oder Klammer ?? ich denke return type !
		if (verifyReturnType < 2)
			syntaxError("Illegal Method Declaration, no return type specified. Token: " +
					currentToken.type.toString() +
					"not valid. in line:" +
					currentToken.lineNumber);

		expect(TLPAREN);

		// Add method to symboltable
		int offsetBefore = CodeGenerator.symbolTable.getCurrentOffset();

		// TODO
		// 1. null = type ist nicht so einfach. Bei Objekten muss ich 
		// irgendwie den Typ vorher deklarieren und dann schauen welcher und irgendwo suchen (siehe Type typen die allgemein definiert sind 
		// INTYPE, BOOLTYPE
		// 2. null ist value ; Methode hat keinen Wert !
		add2SymTable(name, SymbolTableCell.ClassType.method,
				CodeGenerator.INTTYPE, 0);

		// Parameter:
		Vector<String> paramVector = new Vector<String>(); // TODO vorerst, brauche dann ein Objekt statt String

		if (currentToken.type.startSetSimpleDeclaration()) { // if dataTypeDescriptor (startSet is dataType )
			dataTypeDescriptor();
			paramVector.add(tokenList.get(tokenList.size() - 1).value);
			while (currentToken.type == TCOMMA ||
					currentToken.type.startSetSimpleDeclaration()) {
				expectWeak(TCOMMA);
				dataTypeDescriptor();
				paramVector.add(tokenList.get(tokenList.size() - 1).value);
			}
		}
		expectWeak(TRPAREN);
		expectWeak(TLBRACES);

		// add parameters with positive offsets+2 relative from the frame pointer
		// Add parameters to symboltable call by ref noch nicht implementiert !!
		// TODO ich adde einfach mal nur Integer types ; 
		// muss in der Form int x, ... sein -- arrays, objekte geht noch nicht
		CodeGenerator.symbolTable.getSymbol(name).methodSymbols
				.fixOffset(paramVector.size() + 3);
		int i = 0;
		while (i < paramVector.size()) {
			add2SymTable(paramVector.get(i), SymbolTableCell.ClassType.var,
					CodeGenerator.INTTYPE, 1);
			i++;
		}
		CodeGenerator.symbolTable.getSymbol(name).methodSymbols.fixOffset(-2);

		// create method declaration Assembler Code
		int proc=CodeGenerator.methodPrologue();
		
		// fix proc start of method
		CodeGenerator.symbolTable.getSymbol(name).setProc(proc);
		

		bodyBlock();

		// size consist only of the method entries, as it is programmed here:
		int size = CodeGenerator.symbolTable.getSymbol(name).methodSymbols
				.getCurrentOffset(); // last offset of method's sublist

		// fix offset in methods Symbol table cell 
		CodeGenerator.symbolTable.getSymbol(name).fixSizeAndOffset(size,
				offsetBefore + size - 1); // first element starts with offset 0 in methods sublist, so increase size fixup
		// fix global offset counter in Symbol table list
		CodeGenerator.symbolTable.fixOffset(size);

		expectWeak(TRBRACES);

		// End of Method
		CodeGenerator.methodEpilogue(size);
		
//		// When main is finished jump to End of opCode
//		if (name.equals("main")) {
//			
//		}
	}

	private static void objectDeclaration() throws IllegalTokenException {
		debug1("objectDeclaration");
		object();
		objectDeclarationSuffix();
		expectWeak(TSEMICOLON);
	}

	/**
	 * int x;
	 * int x = expression;
	 * int x = 3 + 4; 
	 * int x = a + 4;
	 * char x = c;
	 * 
	 * @throws IllegalTokenException
	 */
	private static void simpleDeclaration() throws IllegalTokenException {
		arrayListEmpty("simpleDeclaration");
		debug1("simpleDeclaration");
		if (currentToken.type.startSetPrimitive())
			primitiveDeclaration();
		else if (currentToken.type.startSetPrimitiveArray())
			primitiveArrayDeclaration();
		else if (currentToken.type == TSTRING)
			stringDeclaration();
		else if (currentToken.type == TSTRING_ARRAY)
			stringArrayDeclaration();

		expectWeak(TSEMICOLON);
	}

	/**
	 * int x=3; OK
	 * int Parser.a=7;
	 * Is Type Safe ! the type of "int" will be used to ensure Type safety !!
	 * 
	 * @throws IllegalTokenException
	 */
	private static void primitiveDeclaration() throws IllegalTokenException {
		debug1("primitiveDeclaration");
		TypeDesc type = primitive();
		identifier();

		// TODO Discuss : wenn id mehr als 2 token, zb.: Parser.methA geht noch nicht !! 
		// aber bei primitiveDeclaration geht parser.methA doch eh nicht oder ?
		if (currentToken.type == TEQL) {
			assignmentSuffix();
			// initialisiere INT mit 0 wenn kein = kommt ! // TODO gibt noch Fehler bei Charactern and boolean !!!
		} else if (type.equals(CodeGenerator.INTTYPE)) {
			CodeGenerator.addI(0);
		}
		add2SymTable(tokenList.get(1).getIdentValue(),
				SymbolTableCell.ClassType.var, type, 1);
		// Store Word
		CodeGenerator.storeWord(CodeGenerator.symbolTable.getSymbol(tokenList
				.get(1).value));
	}

	/**
	 * int[] x = new int[4];
	 * 
	 */
	private static void primitiveArrayDeclaration()
			throws IllegalTokenException {
		debug1("primitiveArrayDeclaration");
		TypeDesc type = primitiveArray();
		identifier(); // nur simple supported at the moment
		expectWeak(TEQL);
		// TODO improve can be over here
		expectWeak(TNEW);
		primitive();
		expectWeak(TLBRACK);
		int arraySize = Integer.parseInt(currentToken.value);
		expect(TNUMBER);
		expectWeak(TRBRACK);
		add2SymTable(tokenList.get(1).getIdentValue(),
				SymbolTableCell.ClassType.array, type, arraySize);
	}

	private static void stringDeclaration() throws IllegalTokenException {
		debug1("stringDeclaration");
		expect(TSTRING);
		identifier();
		expectWeak(TEQL);
		expectWeak(TNEW);
		expectWeak(TSTRING);
		expectWeak(TLPAREN);
		if (currentToken.type == TSTRING_VALUE)
			expect(TSTRING_VALUE);
		expectWeak(TRPAREN);
	}

	private static void stringArrayDeclaration() throws IllegalTokenException {
		debug1("stringArrayDeclaration");
		expect(TSTRING_ARRAY);
		identifier();
		expectWeak(TEQL);
		expectWeak(TNEW);
		expectWeak(TSTRING);
		expectWeak(TLBRACK);
		expect(TNUMBER);
		expectWeak(TRBRACK);
	}

	private static void objectDeclarationAssignmentMethodCall()
			throws IllegalTokenException {
		debug1("objectDeclarationAssignmentMethodCall");
		object();
		if (currentToken.type == TEQL || currentToken.type == TSIDENT ||
				currentToken.type == TLBRACK)
			arrayDeclarationSuffix();
		else if (currentToken.type == TLPAREN)
			methodCallSuffix();
		expectWeak(TSEMICOLON);
	}

	private static void objectDeclarationSuffix() throws IllegalTokenException {
		debug1("objectDeclarationSuffix");
		identifier();
		expectWeak(TEQL);
		expectWeak(TNEW);
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
				expectWeak(TRBRACK);
			}
		} else if (currentToken.type.startSetPrimitive()) {
			primitive();
			expectWeak(TLBRACK);
			expectWeak(TNUMBER);
			expectWeak(TRBRACK);
		} else {
			syntaxError("Illelag token: " + currentToken.type.toString() +
					" in objectDeclaration" + currentToken.lineNumber);
		}
	}

	private static Item assignmentSuffix() throws IllegalTokenException {
		debug1("assignmentSuffix");
		Item returnItem = null;
		expect(TEQL);
		returnItem = expression();
		if (returnItem.mode == 1 || returnItem.mode == 2)
			putValue2Reg(CodeGenerator.INTTYPE, tokenList.size() - 1);
		return returnItem;
	}

	private static void methodCallSuffix() throws IllegalTokenException {
		debug1("methodCallSuffix");
		expect(TLPAREN);
		// check the proc, the absolute programm counter where the method starts
		int procMethod = getIdentifersCell(tokenList.size()-2).getProc(); // TODO wenn Methode Klassen Attribut
		
		if (currentToken.type.startSetExpression()) {
			expression();
			// Load parameters
			CodeGenerator.pushRegister();
		}
		while (currentToken.type == TCOMMA ||
				currentToken.type.startSetExpression()) {
			expectWeak(TCOMMA);
			expression();
			// Load parameters
			CodeGenerator.pushRegister();
		}
		expect(TRPAREN);
		
		// Method Call
		CodeGenerator.methodCall(procMethod);
	}

	private static void arrayDeclarationSuffix() throws IllegalTokenException {
		debug1("arrayDeclarationSuffix");
		if (currentToken.type == TLBRACK)
			arraySelector();
		else if (currentToken.type == TSIDENT)
			objectDeclarationSuffix();
		else if (currentToken.type == TEQL) {
			Item item = assignmentSuffix();
			// y = 4+u; // type Safe expression returns type and y will only be put on the register when it 
			// is from the same type ! else a Error will be printed

			SymbolTableCell cell = CodeGenerator.symbolTable
					.getSymbol(tokenList.get(0).value);
			try {
				CodeGenerator.storeWordCell(cell, item.type);
			} catch (TypeErrorException e) {
				typeError();
				e.printStackTrace();
			}

		}
	}

	/**
	 * bodyBlock() does call any of a if, while, return, declarations, assignments statement
	 * 
	 * Error Handling in the body Block:
	 * On Strong Errors bodyBlock does sync to one of those tokens
	 * "while" 	"if" "return" "int"	 "boolean" "char" "String" "int[]" "boolean[]" "char[]" "String[]" simple identifier
	 */
	private static void bodyBlock() {
		debug1("bodyBlock");
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
				else if (currentToken.type == TPRINT) {
					print();
				} else {
					debug1("END BODY BLOCK");
					break;
				}
			} catch (IllegalTokenException e) {
				e.printStackTrace();
				System.out.println("ERROR SYNC IN BODY BLOCK");
				// goto next Sync Token and retry statement
				while (currentToken.type.ordinal() < STRONG_SYM_BB.ordinal())
					nextToken();
				bodyBlock();
			}
		}
	}

	private static void print() throws IllegalTokenException {
		arrayListEmpty("print");
		expect(TPRINT);
		expectWeak(TLPAREN);
		if (currentToken.type == TSIDENT) {
			identifier();
			CodeGenerator.printIO(getIdentifersCell(tokenList.size())
					.getOffset());
		} else if (currentToken.type == TNUMBER || currentToken.type == TMINUS)
			intValue();
		else if (currentToken.type == TCHAR_VALUE)
			expect(TCHAR_VALUE);
		else if (currentToken.type == TSTRING_VALUE)
			expect(TSTRING_VALUE);
		else
			System.out.println("Nothing to print in line" +
					currentToken.lineNumber);

		expectWeak(TRPAREN);
		expectWeak(TSEMICOLON);
	}

	private static void whileStatement() throws IllegalTokenException {
		debug1("whileStatement");
		expect(TWHILE);
		expectWeak(TLPAREN);
		condition();
		expectWeak(TRPAREN);
		expectWeak(TLBRACES);
		bodyBlock();
		expectWeak(TRBRACES);
	}

	private static void ifStatement() throws IllegalTokenException {
		debug1("ifStatement");
		expect(TIF);
		expectWeak(TLPAREN);
		condition();
		expectWeak(TRPAREN);
		expectWeak(TLBRACES);
		bodyBlock();
		expectWeak(TRBRACES);
		if (currentToken.type == TELSE) {
			expect(TELSE);
			expectWeak(TLBRACES);
			bodyBlock();
			expectWeak(TRBRACES);
		}
	}

	private static void returnStatement() throws IllegalTokenException {
		debug1("returnStatement");
		expect(TRETURN);
		expression();
		expectWeak(TSEMICOLON);
	}

	private static void dataTypeDescriptor() throws IllegalTokenException {
		debug1("dataTypeDescriptor");
		dataType();
		identifier();
		if (currentToken.type == TLBRACK)
			arraySelector();
	}

	// darf nur in Methoden local erfolgen werden immer glei ausgefuehrt und in Registern eingetragen !!!! 
	// TODO
	private static Item expression() throws IllegalTokenException {
		debug1("expression");
		Item returnItem = null;
		returnItem = term();
		while (currentToken.type == TAND || currentToken.type == TOR) {
			if (currentToken.type == TAND)
				expect(TAND);
			else if (currentToken.type == TOR)
				expect(TOR);
			term();
		}
		return returnItem;
	}

	/**
	 * Method is Typesafe "+" "-" does only work on INTTYPE
	 * @throws IllegalTokenException
	 */
	private static Item term() throws IllegalTokenException {
		debug1("term");
		Item item1 = factor();
		Item item2 = null, returnItem = null;

		while (currentToken.type == TPLUS || currentToken.type == TMINUS) {

			int indexFirst = tokenList.size() - 1; // y=(z)+4 geht so nicht ist aber auch fis

			String termKind = null;
			if (currentToken.type == TPLUS) {
				expect(TPLUS);
				termKind = "ADD";
			} else if (currentToken.type == TMINUS) {
				expect(TMINUS);
				termKind = "SUB";
			}

			item2 = factor();

			returnItem = delayedCodeGen(termKind, indexFirst, item1, item2);

		}
		if (item2 != null)
			return returnItem;

		return item1;
	}

	/**
	 * Type Checking happens here. 
	 * Operations "*"  "/" mod can only occur on INTTYPE 
	 * 
	 * @throws IllegalTokenException
	 */
	private static Item factor() throws IllegalTokenException {
		debug1("factor");
		Item item1 = value();
		Item returnItem = null, item2 = null;

		while (currentToken.type == TMULT || currentToken.type == TDIV ||
				currentToken.type == TMOD) {

			int indexFirst = tokenList.size() - 1; // y=(z)*4 geht so nicht ist aber auch fis

			String factorKind = null;
			if (currentToken.type == TMULT) {
				expect(TMULT);
				factorKind = "MUL";
			} else if (currentToken.type == TDIV) {
				expect(TDIV);
				factorKind = "DIV";
			} else if (currentToken.type == TMOD) {
				expect(TMOD); // TODO implement
				factorKind = "MOD";
			}

			item2 = value();

			returnItem = delayedCodeGen(factorKind, indexFirst, item1, item2);
		}
		if (item2 != null)
			return returnItem;

		return item1;
	}

	/**
	 * This method is used in factor and term is responsible for handling the delayed Code Generation for
	 * arthmetic operations;
	 * 
	 * 
	 * @param factorKind
	 * @param indexFirst
	 * @param item1
	 * @param item2
	 * @return
	 * @throws IllegalTokenException
	 */
	private static Item delayedCodeGen(String factorKind, int indexFirst,
			Item item1, Item item2) throws IllegalTokenException {
		Item returnItem = null;
		if (item2 == null)
			syntaxError("Invalid type, in line: " + currentToken.lineNumber);

		if (item1.mode == 3 && item2.mode == 3) { // operator on 2 Registers 
			// operator on Registers
			CodeGenerator.putOperation2Reg(factorKind);
			return new Item(3, CodeGenerator.INTTYPE);
		} else if (item1.mode == 2 && item2.mode == 2) { // Immediate 2 times

			if (item2.type != CodeGenerator.INTTYPE) // type checking
				syntaxError("Invalid type, in line" + currentToken.lineNumber);

			// operator on immediate at compiling 
			int val1 = item1.val;
			int val2 = item2.val;

			if (factorKind.equals("MUL"))
				val2 = val1 * val2;

			if (factorKind.equals("DIV"))
				val2 = val1 / val2;

			// return immediate type
			return new Item(2, CodeGenerator.INTTYPE, val2);

		} else if (item1.mode == 1 && item2.mode == 1) { // two variables in memory
			putIdentifiers2Reg(CodeGenerator.INTTYPE, indexFirst);
			putIdentifiers2Reg(CodeGenerator.INTTYPE, tokenList.size() - 1);
			CodeGenerator.putOperation2Reg(factorKind);
			return new Item(3, CodeGenerator.INTTYPE);
		} // register and variable 
		else if ((item1.mode == 1 && item2.mode == 3)) { // 1 var 2.register 
			putIdentifiers2Reg(CodeGenerator.INTTYPE, indexFirst);
			CodeGenerator.putOperation2Reg(factorKind);
			return new Item(3, CodeGenerator.INTTYPE);

		} else if ((item1.mode == 3 && item2.mode == 1)) {
			putIdentifiers2Reg(CodeGenerator.INTTYPE, tokenList.size() - 1); // 1 reg 2. var
			CodeGenerator.putOperation2Reg(factorKind);
			return new Item(3, CodeGenerator.INTTYPE);

		} else if ((item1.mode == 3 && item2.mode == 2)) { // 1. reg 2. imm
			CodeGenerator.putImOp2Reg(factorKind, item2.val);
			return new Item(3, CodeGenerator.INTTYPE);

		} else if ((item1.mode == 2 && item2.mode == 3)) { // 1. imm 2. reg
			CodeGenerator.putImOp2Reg(factorKind, item1.val);
			return new Item(3, CodeGenerator.INTTYPE);

		} else if ((item1.mode == 1 && item2.mode == 2)) { // 1. var 2. imm
			putIdentifiers2Reg(CodeGenerator.INTTYPE, indexFirst);
			CodeGenerator.putImOp2Reg(factorKind, item2.val);
			return new Item(3, CodeGenerator.INTTYPE);

		} else if ((item1.mode == 2 && item2.mode == 1)) { // 1. imm 2. var
			putIdentifiers2Reg(CodeGenerator.INTTYPE, tokenList.size() - 1);
			CodeGenerator.putImOp2Reg(factorKind, item1.val);
			return new Item(3, CodeGenerator.INTTYPE);
		}
		return returnItem;
	}

	/**
	 * Objekt Item (WIRTH new BOOK page 65)
	 * @author rgratz
	 */
	private static class Item {
		int mode; // 1 .. var (y)  --- 2 ... Immediate (19)  ---- 3 ... Register (last Register)
		TypeDesc type;
		int val;

		// TODO wirth hat noch a,r LONGINT ??

		public Item(int mode, TypeDesc type, int value) {
			this.mode = mode;
			this.type = type;
			this.val = value;
		}

		public Item(int mode, TypeDesc type) {
			this.mode = mode;
			this.type = type;
		}
	}

	private static Item value() throws IllegalTokenException {
		debug1("value");
		Item returnItem = null;
		if (currentToken.type == TSIDENT) {
			identifier();
			returnItem = new Item(1, null);
			if (currentToken.type == TLBRACK)
				arraySelector();
			if (currentToken.type == TLPAREN) {
				methodCallSuffix();
			}
		} else if (currentToken.type == TMINUS || currentToken.type == TNUMBER) {
			returnItem = new Item(2, CodeGenerator.INTTYPE, intValue()); // return Immediate
		} else if (currentToken.type == TCHAR_VALUE)
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
		} else if (currentToken.type == TLPAREN) {
			expect(TLPAREN);
			returnItem = expression();
			expect(TRPAREN);
		}
		return returnItem;
	}

	private static void condition() throws IllegalTokenException {
		debug1("condition");
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

	private static int intValue() throws IllegalTokenException {
		debug1("intValue");
		boolean neg = false;
		if (currentToken.type == TMINUS) {
			expect(TMINUS); // TODO minus is not in Number
			neg = true;
		}
		expect(TNUMBER);
		if (neg)
			return 0 - Integer.parseInt(tokenList.get(tokenList.size() - 1)
					.getIdentValue()); //returns last entrie from the token list
		else
			return Integer.parseInt(tokenList.get(tokenList.size() - 1)
					.getIdentValue()); //returns last entrie from the token list
	}

	private static void booleanValue() throws IllegalTokenException {
		debug1("booleanValue");
		if (currentToken.type == TTRUE)
			expect(TTRUE);
		else if (currentToken.type == TFALSE)
			expect(TFALSE);
		else
			syntaxError("Wrong token " + currentToken.type.toString() +
					", boolean Value expected, at line: " +
					currentToken.lineNumber);
	}

	private static TypeDesc primitive() throws IllegalTokenException {
		debug1("primitive");
		if (currentToken.type == TINT) {
			expect(TINT);
			return CodeGenerator.INTTYPE; // for Symboltable entry
		} else if (currentToken.type == TBOOL) {
			expect(TBOOL);
			return CodeGenerator.BOOLTYPE; // for Symboltable entry
		} else if (currentToken.type == TCHAR) {
			expect(TCHAR);
			return CodeGenerator.CHARTYPE;
		} else {
			syntaxError("Wrong token " + currentToken.type.toString() +
					", primitive datatype expected, at line: " +
					currentToken.lineNumber);
			return null;
		}
	}

	private static TypeDesc primitiveArray() throws IllegalTokenException {
		debug1("primitiveArray");
		if (currentToken.type == TINT_ARRAY) {
			expect(TINT_ARRAY);
			return CodeGenerator.INTTYPE; // for Symboltable entry
		} else if (currentToken.type == TBOOL_ARRAY) {
			expect(TBOOL_ARRAY);
			return CodeGenerator.BOOLTYPE;
		} else if (currentToken.type == TCHAR_ARRAY) {
			expect(TCHAR_ARRAY);
			return CodeGenerator.CHARTYPE;
		} else
			syntaxError("Wrong token " + currentToken.type.toString() +
					", primitive Array datatype expected, at line: " +
					currentToken.lineNumber);
		return null;
	}

	private static void object() throws IllegalTokenException {
		debug1("object");
		expect(TSIDENT);
	}

	private static TypeDesc dataType() throws IllegalTokenException {
		debug1("dataType");
		if (currentToken.type.startSetPrimitive())
			return primitive();
		else if (currentToken.type.startSetPrimitiveArray())
			return primitiveArray(); // TODO stimmt auch nicht so
		else if (currentToken.type == TSTRING) {
			expect(TSTRING);
			return CodeGenerator.STRINGTYPE;
		} else if (currentToken.type == TSTRING_ARRAY) {
			expect(TSTRING_ARRAY);
			// TODO return 
		} else if (currentToken.type == TSIDENT) {
			object();
			// TODO return
		} else
			syntaxError("Wrong token " + currentToken.type.toString() +
					", datatype expected at line: " + currentToken.lineNumber);
		return null;
	}

	static private void identifier() throws IllegalTokenException {
		expect(TSIDENT);
		while (currentToken.type == TDOT) {
			expect(TDOT);
			expect(TSIDENT);
		}
	}

	/**
	 * If TypeDesc of the last identifier(s) (x, method.y) does match, the value of the identfier 
	 * will be put on the next free register in the code generator
	 * 
	 * @param write2Register
	 * @throws IllegalTokenException
	 */
	static private void putIdentifiers2Reg(TypeDesc type, int index) {

		SymbolTableCell cell = getIdentifersCell(index);

		// cell can not be null; Logical programm error in parser.java
		assert (cell == null) : "ERROR IN PARSER. no previous Identifier found in  function putLastIdentifier()";

		// only write to Register if the type is the same as specified
		try {
			CodeGenerator.loadWordType(cell, type);
		} catch (TypeErrorException e) {
			typeError();
			e.printStackTrace();
		}
	}

	// Identifiers and Numbers y=9; y=i; used when only one is after "="
	static private void putValue2Reg(TypeDesc type, int index) {
		if (tokenList.get(index).type == TSIDENT)
			putIdentifiers2Reg(type, index);
		else if (tokenList.get(index).type == TNUMBER)
			CodeGenerator.addI(Integer.parseInt(tokenList.get(index).value));
	}

	/**
	 * returns the cell of an Identfier
	 * 
	 * e.g Identifier at index 4 is x
	 *returns cell of the Identifier. First in global then in local scope
	 *
	 *e.g Identifier at index 4 is x 
	 *and Identifier at index 3 is method. then the var of the method will be returned
	 * 
	 * 
	 * @param index
	 * @return
	 */
	private static SymbolTableCell getIdentifersCell(int index) {
		SymbolTableCell cell = null;
		// get last Identfier(s)
		if (tokenList.get(index).type == TSIDENT) {
			// search in global then in local table and return the cell with the appropriate name
			cell = CodeGenerator.symbolTable
					.getSymbol(tokenList.get(index).value);
			if (index >0 && tokenList.get(index - 1).type == TDOT) {
				if (tokenList.get(index - 2).type == TSIDENT) {
					// selector: method.x
					//TODO improve more levels Object.method.x
					// fetch value of the cell of another method
					cell = CodeGenerator.symbolTable.getSymbol(tokenList
							.get(index - 2).value);
					cell = cell.methodSymbols
							.getSymbol(tokenList.get(index).value);
				}
			}
		}
		return cell;
	}

	private static void typeError() {
		System.out.println("ERROR invalid type in line: " +
				currentToken.lineNumber);
		// TODO stop Code Generation
	}

	private static void arraySelector() throws IllegalTokenException {
		debug1("arraySelector");
		expect(TLBRACK);
		if (currentToken.type.startSetExpression())
			expression();
		expectWeak(TRBRACK);

	}
}
