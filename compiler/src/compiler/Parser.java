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
import static compiler.Ident.TokenID.TPRINT;
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
import static compiler.Util.debug1;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;
import java.util.Vector;

import compiler.Ident.TokenID;
import compiler.SymbolTableCell.ClassType;
import compiler.Util.IllegalTokenException;
import compiler.Util.TypeErrorException;

/**
 * Start Compiling -> fetch token from Scanner -> initiazile Code Generation
 * when sentence is complete. A sentence is complete when a semicolon is
 * expected, or .... (if,while,...)
 * 
 * @author wondn ruap
 */
public class Parser {

	/*
	 * When set true Code Generation will be performed. Code Generation will
	 * stop on any parsing error!
	 */
	static boolean setCodeGeneration = true;
	private static int countTLBRACES;

	static Ident currentToken = new Ident();

	// Liste von tokens, die bei jedem Zeilenende an den Code Generator
	// weitergegeben werden
	static ArrayList<Ident> tokenList = new ArrayList<Ident>();

	private static Vector<Integer> condFixup = null;
	private static Stack<Vector<Integer>> condFixStack = new Stack<Vector<Integer>>();

	private static int booleanLevel;
	private static boolean condMode;
	private static boolean classMember;
	private static ArrayList<Integer> fixReturn = new ArrayList<Integer>();

	// private static HashMap<String,SymbolTableList> symList = new
	// HashMap<String, SymbolTableList>();

	public static void main(String[] args) {
		@SuppressWarnings("unused")
		Scanner scanny = new Scanner();

		Scanner.importSource(args[0].concat(args[1]));

		/* initialize Code Generator */
		new CodeGenerator();

		program();
		// while (true){
		// nextToken();
		// if (currentToken.type == TEOF)
		// break;
		// }

		// CodeGenerator.symbolTable.printSymbolTable();

		System.out.println("");
		System.out.println("Compiling " + args[1] + ".");

		String[] name = new String[1];
		if (args[1].endsWith("com"))
			name = args[1].split(".com");

		if (args[1].endsWith("java"))
			name = args[1].split(".java");

		CodeGenerator.writeOutputFile(name[0]);
		writeSymbolfile(name[0]);

		System.out.println("Thanks for using ComPiler.");
		System.out.println("");

	}

	/**
	 * Fetch next Token from Scanner. The static currentToken variable does
	 * always contain the current token identifier.
	 * 
	 */
	private static void nextToken() {
		currentToken = Scanner.getSym();
		debug1("Current Token: " + currentToken.type + " line: " + currentToken.lineNumber);

	}

	/**
	 * To ensure the correctness of the next token.
	 * 
	 * Week error handling: Mssing tokens which are expected will be inserted,
	 * and a warning will be printed.
	 * 
	 * Strong Error detection: wrong token comes => Syntax Error. This means: =>
	 * goto next Strong Symbol and continue parsing => Code generation will stop !
	 * 
	 * @param expectedID
	 * @throws IllegalTokenException
	 */
	private static void expect(ErrorLevel l, TokenID expectedID) throws IllegalTokenException {
		boolean nextToken = true;
		if (currentToken.type != expectedID) {

			/*
			 * Error Handling week Errors (Missing any kind of Brackets, ...)
			 * missing token will be inserted
			 */
			if (l == ErrorLevel.WEEK) {
				System.out.println("Warning: missing symbol: " + expectedID + " in line " +
						currentToken.lineNumber);

				if (expectedID == TSEMICOLON)
					;
				else
					tokenList.add(new Ident(expectedID)); // insert missing
				// Identifier into
				// code Generation
				// List

				nextToken = false; // dont fetch next token on Week Error !
			}
			// Strong Error Level
			else {
				syntaxError("Error: mismatch token: " + currentToken.type.toString() +
						" expected: " + expectedID.toString() + " in line: " +
						currentToken.lineNumber);
			}
		}

		if (currentToken.type == TSEMICOLON)
			generateCode(); // This can work here, because when semicolon is
		// missing, it will be inserted !
		else {
			if (nextToken)
				tokenList.add(currentToken); // dont add Semicolon 2 list
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
			syntaxError("Error: Token: \"Error\" received, thats not good ! in line " +
					currentToken.lineNumber);
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
	 * print Warning when Array List is not empty. And make the list empty. !
	 * Should never happen !
	 */
	private static void arrayListEmpty(String method) {
		if (tokenList.isEmpty() == false) {
			Util
					.debug3("INTERNAL ERROR IN Parser.java " + method +
							" ; The Parser Token List is not empty but it should be !! Empty the List now.");
			tokenList.clear();
		}
	}

	/**
	 * Start Code generation for listed tokens in the global Variable:
	 * tokenList.
	 * 
	 * @return null
	 */
	private static void generateCode() {
		int i = 0;
		String print = "     SENTENCE: ";
		while (i < tokenList.size()) {
			print = print.concat((new Ident(tokenList.get(i).type).type.toString() + " "));
			i++;
		}
		debug1(print);
		tokenList.clear();
	}

	/**
	 * Adds a new Entry to the symbol table. This works like this:
	 * 
	 * Add all symbols. First class symbols will be added. When a method x
	 * declaration is added, then the following symbols will be add in the
	 * sublist of the method x. When the next method y is added, all following
	 * symbols will be added to the sublist of the method y.
	 * 
	 * @param name
	 * @param classType
	 * @param type
	 * @param intValue
	 * @param stringValue
	 * @param arrayElements
	 *            (when no array set to 1)
	 */
	private static void add2SymTable(String name, ClassType classType, TypeDesc type, int arraySize) {
		int size = type.getSize() * arraySize;
		CodeGenerator.symbolTable.addSym(name, classType, type, size, true);
	}

	/**
	 * Continue parsing on Syntax Errors, but stop Code Generation !
	 * 
	 */
	public static void syntaxError(String string) throws IllegalTokenException {
		setCodeGeneration = false;
		throw new IllegalTokenException(string);
	}

	/** ********* START OF PRODUCTION RULES: ******************** */

	static private void program() {
		debug1("Method: program");
		nextToken(); // initialization

		if (currentToken.type.equals(TPACKAGE)) {
			try {
				packageDeclaration();
			} catch (IllegalTokenException e1) {
				e1.printStackTrace();
			} // optional 1x
		}

		if (currentToken.type.equals(TIMPORT)) {
			while (currentToken.type.equals(TIMPORT))
				try {
					packageImport(); // optional 0,n
				} catch (IllegalTokenException e) {
					while (currentToken.type.ordinal() < STRONG_SYM_CB.ordinal())
						nextToken();
					e.printStackTrace();
				}
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

		String packageName = new String();
		String moduleName = new String();

		SymbolTableList symTable = new SymbolTableList();

		for (int i = 0; i < tokenList.size(); i++) {
			if (tokenList.get(i).type == TokenID.TSIDENT) {

				if (packageName.equals("") == false) {
					packageName = packageName + "/" + tokenList.get(i).value;
				} else {
					packageName = tokenList.get(i).value;
				}

			}
		}
		moduleName = tokenList.get(tokenList.size() - 1).value;
		CodeGenerator.symbolTable.addModule(packageName);
		expectWeak(TSEMICOLON);

		SymbolTableList list = new SymbolTableList();
		ParseSymbolFile symFile = new ParseSymbolFile(moduleName + ".sym", list);
		CodeGenerator.ObjectTypes.put(moduleName, new TypeDesc(0, list, symFile.sum));
		// list.printSymbolTable();

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
	 * Error Handling in classBlock: When something goes wrong in here, he
	 * always searches for the next "public" token !
	 */
	private static void classBlock() {
		debug1("ClassBlock");
		try {
			while (currentToken.type == TSIDENT || currentToken.type.startSetSimpleDeclaration()) {
				if (currentToken.type == TSIDENT)
					objectDeclaration();
				if (currentToken.type.startSetSimpleDeclaration())
					simpleDeclaration();
			}
			// add BSR instruction, programm needs jump to main
			int fixMainPC = CodeGenerator.methodCall(-5);

			while (currentToken.type == TPUBLIC)
				methodDeclaration();

			// fix main jump in opcode
			if (CodeGenerator.mainAddr != -50) {
				int proc = CodeGenerator.symbolTable.getSymbol("main").getProc();
				CodeGenerator.fixMainProc(fixMainPC, proc);
			}

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
		TypeDesc returnType; // TODO weiss noch nicht ob man den
		// Rueckgabewert ueberhaupt pruefen sollen,
		// overkill ???

		if (currentToken.type == TVOID) {
			expect(TVOID);
			verifyReturnType++;
		} else if (currentToken.type.startSetDataType()) {
			returnType = dataType(); // return type
			verifyReturnType++;
		} else
			syntaxError("Illegal Method Declaration, Return Type not valid. Token: " +
					currentToken.type.toString() + "not valid. in line:" + currentToken.lineNumber);

		// method Name
		String name = currentToken.value;
		if (name.equals("main")) {
			CodeGenerator.setMainPC();
		}
		if (currentToken.type == TSIDENT) {
			expect(TSIDENT);
			verifyReturnType++;
		}

		// Frage was vergisst man mehr Return Type oder Klammer ?? ich denke
		// return type !
		if (verifyReturnType < 2)
			syntaxError("Illegal Method Declaration, no return type specified. Token: " +
					currentToken.type.toString() + "not valid. in line:" + currentToken.lineNumber);

		expect(TLPAREN);

		// Add method to symboltable
		int offsetBefore = CodeGenerator.symbolTable.getCurrentOffset();

		// TODO
		// 1. null = type ist nicht so einfach. Bei Objekten muss ich
		// irgendwie den Typ vorher deklarieren und dann schauen welcher und
		// irgendwo suchen (siehe Type typen die allgemein definiert sind
		// INTYPE, BOOLTYPE
		// 2. null ist value ; Methode hat keinen Wert !
		add2SymTable(name, SymbolTableCell.ClassType.method, CodeGenerator.INTTYPE, 0);

		// Parameter:
		Vector<String> paramVector = new Vector<String>(); // TODO vorerst,
		// replace Vector
		// due Performance
		// issue
		Vector<TypeDesc> typeVector = new Vector<TypeDesc>(); // TODO vorerst,
		// brauche dann ein
		// Objekt statt
		// String

		if (currentToken.type.startSetSimpleDeclaration()) { // if
			// dataTypeDescriptor
			// (startSet is
			// dataType )
			typeVector.add(dataTypeDescriptor());
			paramVector.add(tokenList.get(tokenList.size() - 1).value);
			while (currentToken.type == TCOMMA || currentToken.type.startSetSimpleDeclaration()) {
				expectWeak(TCOMMA);
				typeVector.add(dataTypeDescriptor());
				paramVector.add(tokenList.get(tokenList.size() - 1).value);

			}
		}
		expectWeak(TRPAREN);
		expectWeak(TLBRACES);

		// add parameters with positive offsets+2 relative from the frame
		// pointer
		// Add parameters to symboltable call by ref noch nicht implementiert !!
		// TODO ich adde einfach mal nur Integer types ;
		// muss in der Form int x, ... sein -- arrays, objekte geht noch nicht
		CodeGenerator.symbolTable.getSymbol(name).methodSymbols.fixOffset(paramVector.size() + 2);
		int i = 0;
		while (i < paramVector.size()) {
			add2SymTable(paramVector.get(i), SymbolTableCell.ClassType.var, typeVector.get(i), 1);
			i++;
		}
		CodeGenerator.symbolTable.getSymbol(name).methodSymbols.fixOffset(-2);

		// create method declaration Assembler Code
		int proc = CodeGenerator.methodPrologue();

		// fix proc start of method
		CodeGenerator.symbolTable.getSymbol(name).setProc(proc);

		bodyBlock();

		// size consist only of the method entries, as it is programmed here:
		int size = CodeGenerator.symbolTable.getSymbol(name).methodSymbols.getCurrentOffset(); // last offset of method's sublist

		if (size < 0)
			size = 0 - size;

		// fix offset in methods Symbol table cell
		CodeGenerator.symbolTable.getSymbol(name).fixSizeAndOffset(size, offsetBefore + size - 1); // first element starts with offset
		// 0 in methods sublist, so increase
		// size fixup
		// fix global offset counter in Symbol table list
		CodeGenerator.symbolTable.fixOffset(size);

		expectWeak(TRBRACES);

		//fix Return jumps
		CodeGenerator.fixReturnJumps(fixReturn);
		fixReturn.clear();
		// End of Method
		if (!name.equals("main"))
			CodeGenerator.methodEpilogue(size);
		else
			CodeGenerator.methodEpilogueMain(size, true);

		// // When main is finished jump to End of opCode
		// if (name.equals("main")) {
		//			
		// }
	}

	private static void objectDeclaration() throws IllegalTokenException {
		debug1("objectDeclaration");
		object();
		objectDeclarationSuffix();
		expectWeak(TSEMICOLON);
	}

	/**
	 * int x; int x = expression; int x = 3 + 4; int x = a + 4; char x = c;
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
	 * int x=3; OK int Parser.a=7; Is Type Safe ! the type of "int" will be used
	 * to ensure Type safety !!
	 * 
	 * @throws IllegalTokenException
	 */
	private static void primitiveDeclaration() throws IllegalTokenException {
		debug1("primitiveDeclaration");
		TypeDesc type = primitive();
		identifier();

		if (currentToken.type == TEQL) {
			Item item = assignmentSuffix();

			// type checking:
			if (item.type != type)
				syntaxError("Invalid Type assignment, in line :" + currentToken.lineNumber);

			// initialisiere INT mit 0 wenn kein = kommt !
		} else if (type.equals(CodeGenerator.INTTYPE)) {
			CodeGenerator.addI(0);
		}

		add2SymTable(tokenList.get(1).getIdentValue(), SymbolTableCell.ClassType.var, type, 1);
		CodeGenerator.storeWord(CodeGenerator.symbolTable.getSymbol(tokenList.get(1).value));
	}

	/**
	 * int[] x = new int[4];
	 * 
	 */
	private static void primitiveArrayDeclaration() throws IllegalTokenException {
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
		add2SymTable(tokenList.get(1).getIdentValue(), SymbolTableCell.ClassType.array, type,
				arraySize);
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

	private static void objectDeclarationAssignmentMethodCall() throws IllegalTokenException {
		arrayListEmpty("objectDeclarationAssignmentMethodCall");
		debug1("objectDeclarationAssignmentMethodCall");
		// object();
		identifier();
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
			syntaxError("Illelag token: " + currentToken.type.toString() + " in objectDeclaration" +
					currentToken.lineNumber);
		}
	}

	private static Item assignmentSuffix() throws IllegalTokenException {
		debug1("assignmentSuffix");
		Item returnItem = null;
		expect(TEQL);
		returnItem = expression();
		if (returnItem.mode == 1 || returnItem.mode == 2) {
			int i = 1;
			while (tokenList.get(tokenList.size() - i).type == TRPAREN) {
				i++;
				if (tokenList.get(tokenList.size() - i).type != TNUMBER)
					System.out
							.println("INTERNAL ERROR IN PARSER. this should never ever happen, probably your computer is defect, buy a new one !");
			}
			putValue2Reg(tokenList.size() - i);
		}
		return returnItem;
	}

	private static Item methodCallSuffix() throws IllegalTokenException {
		debug1("methodCallSuffix");
		expect(TLPAREN);
		int procMethod = 0;
		String foreignClassMember = null;
		// check the proc, the absolute programm counter where the method starts
		boolean member = true;
		Item returnItem = null;
		if (classMember) {
			procMethod = getIdentifersCell(tokenList.size() - 2).getProc(); // TODO
			returnItem = new Item(3, getIdentifersCell(tokenList.size() - 2).getType(), 0);
		} else {
			procMethod = -55;
			String className = tokenList.get(tokenList.size() - 4).value;
			foreignClassMember = className + "." + tokenList.get(tokenList.size() - 2).value;
			// check if class exists
			if (!CodeGenerator.ObjectTypes.containsKey(className))
				syntaxError("Module does not exist in line: " + currentToken.lineNumber);
			SymbolTableCell cell = CodeGenerator.ObjectTypes.get(className).fields
					.getGlobalSymbol(tokenList.get(tokenList.size() - 2).value);
			returnItem = new Item(3, cell.getType(), 0);
			if (cell == null)
				syntaxError("Module: " + className + " does not contain a symbol: " +
						tokenList.get(tokenList.size() - 2).value + " in line: " +
						currentToken.lineNumber);
			member = false;
		}

		if (currentToken.type.startSetExpression()) {
			Item item = expression();
			if (item.mode == 1 || item.mode == 2)
				putValue2Reg(tokenList.size() - 1);
			// Load parameters
			CodeGenerator.pushRegister();
		}
		while (currentToken.type == TCOMMA || currentToken.type.startSetExpression()) {
			expectWeak(TCOMMA);
			Item item = expression();
			if (item.mode == 1 || item.mode == 2)
				putValue2Reg(tokenList.size() - 1);
			// Load parameters
			CodeGenerator.pushRegister();
		}
		expect(TRPAREN);

		// Method Call
		int fixup = CodeGenerator.methodCall(procMethod);
		if (!member) {
			CodeGenerator.fixupTable.put(foreignClassMember, fixup+1);
		}
		return returnItem;

	}

	private static void arrayDeclarationSuffix() throws IllegalTokenException {
		debug1("arrayDeclarationSuffix");
		int type = 0; // 1 ... array
		Item item, itemArray = null;
		if (currentToken.type == TLBRACK) {
			itemArray = arraySelector();
			// x[8]=
			type = 1;
		}
		if (currentToken.type == TSIDENT)
			objectDeclarationSuffix();
		else if (currentToken.type == TEQL) {
			item = assignmentSuffix();

			if (item.mode == 1)
				item.type = CodeGenerator.symbolTable.getSymbol(
						tokenList.get(tokenList.size() - 1).value).getType();

			// y = 4+u; // type Safe expression returns type and y will only be
			// put on the register when it
			// is from the same type ! else a Error will be printed
			if (type == 0) {
				SymbolTableCell cell = CodeGenerator.symbolTable.getSymbol(tokenList.get(0).value);
				try {
					if (!compareItemType(new Item(0, cell.getType(), 0), item))
						syntaxError("Invalid type assignment in line: " + currentToken.lineNumber);
					CodeGenerator.storeWordCell(cell, item.type);
				} catch (TypeErrorException e) {
					typeError();
					e.printStackTrace();
				}
			} else if (type == 1) {
				// Type Safety

				if (compareItemType(itemArray, item))
					CodeGenerator.storeWordArray(itemArray.globalScope);
				else
					syntaxError("Not Type Safe, in array, in line: " + currentToken.lineNumber);
			}

		}
	}

	/**
	 * bodyBlock() does call any of a if, while, return, declarations,
	 * assignments statement
	 * 
	 * Error Handling in the body Block: On Strong Errors bodyBlock does sync to
	 * one of those tokens "while" "if" "return" "int" "boolean" "char" "String"
	 * "int[]" "boolean[]" "char[]" "String[]" simple identifier
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
				else if (currentToken.type == TPRINT)
					print();
				else if (currentToken.type == TRBRACES) {
					if (countTLBRACES <= 2)
						return;
				} else if (currentToken.type.ordinal() < STRONG_SYM_CB.ordinal())
					nextToken();
				else {
					debug1("END BODY BLOCK");
					break;
				}
			} catch (IllegalTokenException e) {
				e.printStackTrace();
				Util.debug2("ERROR SYNC IN BODY BLOCK");
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
			SymbolTableCell cell = getIdentifersCell(tokenList.size() - 1);
			putValue2Reg(tokenList.size() - 1);
			CodeGenerator.printIO(cell.getType());
		} else if (currentToken.type == TNUMBER) {// || currentToken.type ==
			// TMINUS) {
			value(null);
			putValue2Reg(tokenList.size() - 1);
			CodeGenerator.printIO(CodeGenerator.INTTYPE);
		} else if (currentToken.type == TCHAR_VALUE) {
			value(null);
			putValue2Reg(tokenList.size() - 1);
			CodeGenerator.printIO(CodeGenerator.CHARTYPE);
		} else if (currentToken.type == TTRUE || currentToken.type == TFALSE) {
			value(null);
			putValue2Reg(tokenList.size() - 1);
			CodeGenerator.printIO(CodeGenerator.BOOLTYPE);
		} else if (currentToken.type == TSTRING_VALUE)
			expect(TSTRING_VALUE);
		else
			System.out.println("Nothing to print in line " + currentToken.lineNumber);

		expectWeak(TRPAREN);
		expectWeak(TSEMICOLON);
	}

	private static void whileStatement() throws IllegalTokenException {
		debug1("whileStatement");
		condFixStack.push(condFixup);
		condFixup = new Vector<Integer>();
		expect(TWHILE);
		expectWeak(TLPAREN);
		int whileLoopStart = CodeGenerator.PC;
		condMode = true;
		expression();
		condMode = false;
		expectWeak(TRPAREN);
		expectWeak(TLBRACES);
		int trueJump = CodeGenerator.PC;
		bodyBlock();
		expectWeak(TRBRACES);
		// fixup
		CodeGenerator.elseAndLoopJump(whileLoopStart);
		CodeGenerator.fixConditionJump(condFixup, CodeGenerator.PC, trueJump);
		condFixup = condFixStack.pop();
	}

	private static void ifStatement() throws IllegalTokenException {
		debug1("ifStatement");
		condFixStack.push(condFixup);
		condFixup = new Vector<Integer>();
		expect(TIF);
		expectWeak(TLPAREN);
		condMode = true;
		expression();
		condMode = false;
		expectWeak(TRPAREN);
		expectWeak(TLBRACES);
		int trueJump = CodeGenerator.PC;
		bodyBlock();
		expectWeak(TRBRACES);
		if (currentToken.type == TELSE) {
			int elsePC = CodeGenerator.elseAndLoopJump(-100);
			// fixup condition jump false
			CodeGenerator.fixConditionJump(condFixup, elsePC + 2, trueJump);
			expect(TELSE);
			expectWeak(TLBRACES);
			bodyBlock();
			expectWeak(TRBRACES);
			// fix else jump
			CodeGenerator.fixConditionJump(elsePC, CodeGenerator.PC, trueJump);
		} else {
			CodeGenerator.fixConditionJump(condFixup, CodeGenerator.PC, trueJump);
		}
		condFixup = condFixStack.pop();
	}

	private static Item returnStatement() throws IllegalTokenException {
		debug1("returnStatement");
		expect(TRETURN);
		Item returnItem;
		returnItem = expression();
		if (returnItem.mode == 1 || returnItem.mode == 2)
			putValue2Reg(tokenList.size() - 1);
		expectWeak(TSEMICOLON);
		fixReturn.add(CodeGenerator.methodCall(-43));
		return returnItem;
	}

	private static TypeDesc dataTypeDescriptor() throws IllegalTokenException {
		debug1("dataTypeDescriptor");
		TypeDesc type = dataType();
		identifier();
		if (currentToken.type == TLBRACK)
			arraySelector();
		return type;
	}

	private static Item expression() throws IllegalTokenException {
		return expression(null);
	}

	// werden nicht in Registern eingetragen !! aufrufer muss selbst dafuer
	// sorgen, falls er das braucht
	private static Item expression(LinkedList<fixUps> orMap) throws IllegalTokenException {
		if (orMap == null)
			orMap = new LinkedList<fixUps>();
		booleanLevel += 1;
		debug1("expression");
		Item returnItem = null;
		returnItem = andExpression(orMap);
		while (currentToken.type == TOR) {
			expect(TOR);
			Vector<Integer> veci = new Vector<Integer>();
			Iterator<fixUps> iti = orMap.iterator();
			int count = 1;
			while (iti.hasNext()) {
				fixUps fixi = iti.next();
				if (booleanLevel <= fixi.level) {
					count++;
					veci.add(fixi.position);
					fixi.level = booleanLevel - 1;
				}
			}
			int index = count;
			while (count > 2) {
				CodeGenerator.fixOrJumps(veci.get(index - count), false, false);
				count--;
			}
			CodeGenerator.fixOrJumps(veci.get(index - count), true, false);
			andExpression(orMap);
		}
		booleanLevel -= 1;
		return returnItem;
	}

	private static Item andExpression(LinkedList<fixUps> fixupMap) throws IllegalTokenException {
		debug1("andExpression");
		Item returnItem = null;
		returnItem = relation(fixupMap);
		while (currentToken.type == TAND) {
			expect(TAND);
			Vector<Integer> veci = new Vector<Integer>();
			Iterator<fixUps> iti = fixupMap.iterator();
			int count = 1;
			while (iti.hasNext()) {
				fixUps fixi = iti.next();
				if (booleanLevel <= fixi.level) {
					count++;
					veci.add(fixi.position);
					fixi.level = booleanLevel - 1;
				}
			}
			int index = count;
			while (count > 2) {
				CodeGenerator.fixOrJumps(veci.get(index - count), false, true);
				count--;
			}
			// CodeGenerator.fixOrJumps(veci.get(index-count), true);
			relation(fixupMap);
		}
		return returnItem;
	}

	public static class fixUps {
		int position;
		int level;

		public fixUps(int position, int level) {
			this.position = position;
			this.level = level;
		}
	}

	/**
	 * Method is Typesafe "+" "-" does only work on INTTYPE
	 * 
	 * @throws IllegalTokenException
	 */
	private static Item term(LinkedList<fixUps> vec) throws IllegalTokenException {
		debug1("term");
		Item item1 = factor(vec);
		if (item1 == null && currentToken.type == TMINUS) {
			item1 = new Item(2, CodeGenerator.INTTYPE, 0);
		}
		Item item2 = null, returnItem = null;

		if (currentToken.type == TPLUS || currentToken.type == TMINUS) {
			int indexFirst = tokenList.size() - 1; // y=(z)+4 geht so nicht ist
			// aber auch fis
			String termKind = getOperation();
			item2 = factor(vec);
			returnItem = delayedCodeGen(termKind, indexFirst, item1, item2);
			if (currentToken.type == TPLUS || currentToken.type == TMINUS) {
				returnItem = delayedCodeGen(getOperation(), indexFirst, returnItem, expression());
			}
		}
		if (item2 != null)
			return returnItem;

		return item1;
	}

	/**
	 * Type Checking happens here. Operations "*" "/" mod can only occur on
	 * INTTYPE
	 * 
	 * @throws IllegalTokenException
	 */
	private static Item factor(LinkedList<fixUps> vec) throws IllegalTokenException {
		debug1("factor");
		Item item1 = value(vec);
		Item returnItem = null, item2 = null;

		if (currentToken.type == TMULT || currentToken.type == TDIV || currentToken.type == TMOD) {
			int indexFirst = tokenList.size() - 1; // y=(z)*4 geht so nicht ist
			// aber auch fis
			String factorKind = getOperation();
			item2 = value(vec);
			returnItem = delayedCodeGen(factorKind, indexFirst, item1, item2);
			if (currentToken.type == TMULT || currentToken.type == TDIV ||
					currentToken.type == TMOD) {
				returnItem = delayedCodeGen(getOperation(), indexFirst, returnItem, expression());
			}
		}
		if (item2 != null)
			return returnItem;

		return item1;
	}

	private static String getOperation() throws IllegalTokenException {
		if (currentToken.type == TMULT) {
			expect(TMULT);
			return "MUL";
		} else if (currentToken.type == TDIV) {
			expect(TDIV);
			return "DIV";
		} else if (currentToken.type == TMOD) {
			expect(TMOD);
			return "MOD";
		} else if (currentToken.type == TPLUS) {
			expect(TPLUS);
			return "ADD";
		} else if (currentToken.type == TMINUS) {
			expect(TMINUS);
			return "SUB";
		}
		return null;
	}

	/**
	 * This method is used in factor and term is responsible for handling the
	 * delayed Code Generation for arthmetic operations;
	 * 
	 * 
	 * @param factorKind
	 * @param indexFirst
	 * @param item1
	 * @param item2
	 * @return
	 * @throws IllegalTokenException
	 */
	private static Item delayedCodeGen(String factorKind, int indexFirst, Item item1, Item item2)
			throws IllegalTokenException {
		Item returnItem = null;
		if (item2 == null)
			syntaxError("Invalid type, in line: " + currentToken.lineNumber);

		if (item1.mode == 3 && item2.mode == 3) { // operator on 2 Registers
			// operator on Registers
			CodeGenerator.putOperation2Reg(factorKind);
			return new Item(3, CodeGenerator.INTTYPE);
		} else if (item1.mode == 2 && item2.mode == 2) { // Immediate 2 times

			if (item2.type != CodeGenerator.INTTYPE) // type checking
				syntaxError("Invalid type, in line " + currentToken.lineNumber);

			// operator on immediate at compiling
			int val1 = item1.val;
			int val2 = item2.val;

			if (factorKind.equals("MUL"))
				val2 = val1 * val2;

			if (factorKind.equals("DIV"))
				val2 = val1 / val2;

			if (factorKind.equals("ADD"))
				val2 = val1 + val2;

			if (factorKind.equals("SUB"))
				val2 = val1 - val2;

			if (factorKind.equals("MOD"))
				val2 = val1 % val2;

			tokenList.get(tokenList.size() - 1).value = String.valueOf(val2);

			// return immediate type
			return new Item(2, CodeGenerator.INTTYPE, val2);

		} else if (item1.mode == 1 && item2.mode == 1) { // two variables in
			// memory
			if (putIdentifiers2Reg(indexFirst) != CodeGenerator.INTTYPE)
				syntaxError("Invalid type for arithmetic  operation in line " +
						currentToken.lineNumber);
			if (putIdentifiers2Reg(tokenList.size() - 1) != CodeGenerator.INTTYPE)
				syntaxError("Invalid type for arithmetic  operation in line " +
						currentToken.lineNumber);

			CodeGenerator.putOperation2Reg(factorKind);
			return new Item(3, CodeGenerator.INTTYPE);
		} // register and variable
		else if ((item1.mode == 1 && item2.mode == 3)) { // 1 var 2.register
			if (putIdentifiers2Reg(indexFirst) != CodeGenerator.INTTYPE)
				syntaxError("Invalid type for arithmetic  operation in line " +
						currentToken.lineNumber);
			CodeGenerator.putOperation2Reg(factorKind);
			return new Item(3, CodeGenerator.INTTYPE);

		} else if ((item1.mode == 3 && item2.mode == 1)) {
			putIdentifiers2Reg(tokenList.size() - 1); // 1
			// reg
			// 2.
			// var
			CodeGenerator.putOperation2Reg(factorKind);
			return new Item(3, CodeGenerator.INTTYPE);

		} else if ((item1.mode == 3 && item2.mode == 2)) { // 1. reg 2. imm
			CodeGenerator.putImOp2Reg(factorKind, item2.val);
			return new Item(3, CodeGenerator.INTTYPE);

		} else if ((item1.mode == 2 && item2.mode == 3)) { // 1. imm 2. reg
			if (factorKind.equals("SUB")) {
				CodeGenerator.invertValofLastReg();
				factorKind = "ADD";
			}
			CodeGenerator.putImOp2Reg(factorKind, item1.val);
			return new Item(3, CodeGenerator.INTTYPE);

		} else if ((item1.mode == 1 && item2.mode == 2)) { // 1. var 2. imm
			if (putIdentifiers2Reg(indexFirst) != CodeGenerator.INTTYPE)
				syntaxError("Invalid type for arithmetic  operation in line " +
						currentToken.lineNumber);
			CodeGenerator.putImOp2Reg(factorKind, item2.val);
			return new Item(3, CodeGenerator.INTTYPE);

		} else if ((item1.mode == 2 && item2.mode == 1)) { // 1. imm 2. var
			if (putIdentifiers2Reg(tokenList.size() - 1) != CodeGenerator.INTTYPE)
				syntaxError("Invalid type for arithmetic  operation in line " +
						currentToken.lineNumber);

			if (factorKind.equals("SUB")) {
				CodeGenerator.invertValofLastReg();
				factorKind = "ADD";
			}
			CodeGenerator.putImOp2Reg(factorKind, item1.val);
			return new Item(3, CodeGenerator.INTTYPE);
		}
		return returnItem;
	}

	/**
	 * Objekt Item (WIRTH new BOOK page 65)
	 * 
	 * @author rgratz
	 */
	private static class Item {
		int mode; // 1 .. var (y) --- 2 ... Immediate (19) ---- 3 ... Register

		TypeDesc type;
		int val;
		boolean globalScope;

		public Item(int mode, TypeDesc type, int value) {
			this.mode = mode;
			this.type = type;
			this.val = value;
		}

		public Item(int mode, TypeDesc type) {
			this.mode = mode;
			this.type = type;
		}

		public Item(int mode, TypeDesc type, boolean globalScope) {
			this.mode = mode;
			this.type = type;
			this.globalScope = globalScope;
		}

	}

	private static boolean compareItemType(Item item1, Item item2) {
		if (item1.type == item2.type)
			return true;
		return false;
	}

	/**
	 * 
	 * @return Item 1.. var 2.. const 3.. reg
	 * @throws IllegalTokenException
	 */
	private static Item value(LinkedList<fixUps> fixupMap) throws IllegalTokenException {
		debug1("value");
		Item returnItem = null;
		if (currentToken.type == TSIDENT) {
			identifier();
			returnItem = new Item(1, null);
			if (currentToken.type == TLBRACK) {
				returnItem = arraySelector();
				// load indexed array to register
				CodeGenerator.loadWordArray(returnItem.globalScope); // TODO
				// scope
				// returnItem = new Item(3, null);
			}
			if (currentToken.type == TLPAREN) {
				returnItem = methodCallSuffix();
			}
		} else if (currentToken.type == TNUMBER) {
			returnItem = new Item(2, CodeGenerator.INTTYPE, intValue()); // return
			// Immediate
		} else if (currentToken.type == TCHAR_VALUE) {
			returnItem = new Item(2, CodeGenerator.CHARTYPE, Integer.parseInt(currentToken.value)); // return
			expect(TCHAR_VALUE);
		} else if (currentToken.type == TTRUE || currentToken.type == TFALSE)
			returnItem = booleanValue();
		else if (currentToken.type == TSTRING_VALUE)
			expect(TSTRING_VALUE);
		else if (currentToken.type == TNULL)
			expect(TNULL);
		else if (currentToken.type == TNOT) {
			expect(TNOT);
			value(fixupMap);
		} else if (currentToken.type == TLPAREN) {
			expect(TLPAREN);
			returnItem = expression(fixupMap);
			expect(TRPAREN);
		}
		return returnItem;
	}

	/**
	 * @return int value, serves as position for fixup Control instructions
	 * @throws IllegalTokenException
	 */
	private static Item relation(LinkedList<fixUps> fixupMap) throws IllegalTokenException {
		debug1("condition");

		Item item = term(fixupMap);
		Item item2 = null;
		if (currentToken.type.startSetRelation()) {
			int op = 0;
			if (item.mode == 1)
				item.type = putValue2Reg(tokenList.size() - 1);
			else if (item.mode == 2)
				putValue2Reg(tokenList.size() - 1);
			if (currentToken.type == TEQL) {
				expect(TEQL);
				expect(TEQL);
				op = CodeGenerator.BNE;
			} else if (currentToken.type == TNOT) {
				expect(TNOT);
				expect(TEQL);
				op = CodeGenerator.BEQ;
			} else if (currentToken.type == TGTR) {
				expect(TGTR); // i>j
				op = CodeGenerator.BLE;
			} else if (currentToken.type == TLEQ) {
				expect(TLEQ); // i<= j
				op = CodeGenerator.BGT;
			} else if (currentToken.type == TLSS) {
				expect(TLSS); // i<j BGE
				op = CodeGenerator.BGE;
			} else if (currentToken.type == TGEQ) {
				expect(TGEQ); // i >= j
				op = CodeGenerator.BLT;
			}

			item2 = term(fixupMap);
			if (item2.mode == 1)
				item2.type = putValue2Reg(tokenList.size() - 1);
			else if (item2.mode == 2)
				putValue2Reg(tokenList.size() - 1);

			// type Checking
			if (!compareItemType(item, item2)) {
				syntaxError("Cannot compare incompatible types in line: " + currentToken.lineNumber);
			}

			int pcPos = CodeGenerator.relation(op);
			fixupMap.add(new fixUps(pcPos, booleanLevel));
			condFixup.add(pcPos);
		} else if (condMode && item2 == null) {
			if (item.mode == 2 && item.type != null && item.type == CodeGenerator.BOOLTYPE) {
				int pcPos = CodeGenerator.boolAss(true, item.val);
				fixupMap.add(new fixUps(pcPos, booleanLevel));
				condFixup.add(pcPos);
			} else if (getIdentifersCell(tokenList.size() - 1) != null &&
					getIdentifersCell(tokenList.size() - 1).getType() != null &&
					getIdentifersCell(tokenList.size() - 1).getType() == CodeGenerator.BOOLTYPE) {
				putValue2Reg(tokenList.size() - 1);
				int pcPos = CodeGenerator.boolAss(false, item.val);
				fixupMap.add(new fixUps(pcPos, booleanLevel));
				condFixup.add(pcPos);
			}
		}
		return item;
	}

	private static int intValue() throws IllegalTokenException {
		debug1("intValue");
		expect(TNUMBER);
		return Integer.parseInt(tokenList.get(tokenList.size() - 1).getIdentValue()); // returns last entrie from the token
	}

	private static Item booleanValue() throws IllegalTokenException {
		debug1("booleanValue");
		if (currentToken.type == TTRUE) {
			expect(TTRUE);
			tokenList.get(tokenList.size() - 1).value = "1";
			return (new Item(2, CodeGenerator.BOOLTYPE, 1));
		} else if (currentToken.type == TFALSE) {
			expect(TFALSE);
			tokenList.get(tokenList.size() - 1).value = "0";
			return (new Item(2, CodeGenerator.BOOLTYPE, 0));
		} else
			syntaxError("Wrong token " + currentToken.type.toString() +
					", boolean Value expected, in line: " + currentToken.lineNumber);
		return null;
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
					", primitive datatype expected, in line: " + currentToken.lineNumber);
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
					", primitive Array datatype expected, in line: " + currentToken.lineNumber);
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
					", datatype expected in line: " + currentToken.lineNumber);
		return null;
	}

	static private void identifier() throws IllegalTokenException {
		expect(TSIDENT);
		classMember = true;
		if (currentToken.type == TDOT) {
			// Util.x
			expect(TDOT);
			expect(TSIDENT);
			classMember = false;
		}
	}

	/**
	 * The value of the identfier will be put on the next free register in the
	 * code generator
	 * 
	 * @param write2Register
	 * @throws IllegalTokenException
	 */
	static private TypeDesc putIdentifiers2Reg(int index) {
		SymbolTableCell cell = getIdentifersCell(index);
		assert (cell == null) : "ERROR IN PARSER. no previous Identifier found in  function putLastIdentifier()";
		CodeGenerator.loadWordType(cell);
		return cell.getType();
	}

	// Identifiers and Numbers y=9; y=i; used when only one is after "="
	static private TypeDesc putValue2Reg(int index) {
		if (tokenList.get(index).type == TSIDENT)
			return putIdentifiers2Reg(index);
		else
			CodeGenerator.addI(Integer.parseInt(tokenList.get(index).value));
		return null;
	}

	/**
	 * returns the cell of an Identfier
	 * 
	 * e.g Identifier at index 4 is x returns cell of the Identifier. First in
	 * global then in local scope
	 * 
	 * e.g Identifier at index 4 is x and Identifier at index 3 is method. then
	 * the var of the method will be returned
	 * 
	 * 
	 * @param index
	 * @return
	 */
	private static SymbolTableCell getIdentifersCell(int index) {
		SymbolTableCell cell = null;
		// get last Identfier(s)
		if (tokenList.get(index).type == TSIDENT) {
			// search in global then in local table and return the cell with the
			// appropriate name
			cell = CodeGenerator.symbolTable.getSymbol(tokenList.get(index).value);
			if (index > 0 && tokenList.get(index - 1).type == TDOT) {
				if (tokenList.get(index - 2).type == TSIDENT) {
					// selector: method.x
					// TODO improve more levels Object.method.x
					// fetch value of the cell of another method
					cell = CodeGenerator.symbolTable.getSymbol(tokenList.get(index - 2).value);
					cell = cell.methodSymbols.getSymbol(tokenList.get(index).value);
				}
			}
		}
		return cell;
	}

	private static void typeError() {
		System.out.println("ERROR invalid type in line: " + currentToken.lineNumber);
		// TODO stop Code Generation
	}

	/**
	 * puts the offset of the indexed element to the last Register
	 * 
	 * @return
	 * @throws IllegalTokenException
	 */
	private static Item arraySelector() throws IllegalTokenException {
		debug1("arraySelector");
		// remember position of simple Identifier in tokenList
		int sIdentPos = tokenList.size() - 1;
		expect(TLBRACK);
		Item item = null;
		if (currentToken.type.startSetExpression()) {
			item = expression();

			// index musst always be of integer type !
			if (item.type != CodeGenerator.INTTYPE)
				syntaxError("Invalid type in array Selector in line: " + currentToken.lineNumber);

			if (item.mode == 1 || item.mode == 2)
				putValue2Reg(tokenList.size() - 1);

			// calc offset of array element which must be added to array index
			SymbolTableCell cell = CodeGenerator.symbolTable
					.getSymbol(tokenList.get(sIdentPos).value);
			int calc = cell.getOffset() + cell.getSize();

			item = new Item(3, cell.getType(), cell.isGlobalScope());

			if (calc < 0)
				CodeGenerator.putImOp2Reg("ADD", 0 - calc);

			if (!cell.isGlobalScope())
				CodeGenerator.invertValofLastReg();
		}
		expectWeak(TRBRACK);
		return item;
	}

	private static void writeSymbolfile(String name) {
		SymbolFile symbolFile = new SymbolFile(name);
		symbolFile.writeHeader();
		CodeGenerator.symbolTable.exportModuleAnchors(symbolFile);
		CodeGenerator.symbolTable.exportSymbols(symbolFile);
		symbolFile.writeFooter();
	}
}
