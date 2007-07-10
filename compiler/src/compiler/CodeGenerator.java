package compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Vector;

import linker.ObjectFile;

import compiler.Util.TypeErrorException;

public class CodeGenerator {

	// format 1 instructions
	public static final int ADDI = 0, SUBI = 1, MULI = 2, DIVI = 3, MODI = 4,
			CMPI = 5, CHKI = 6, ANDI = 7, BICI = 8, ORI = 9, XORI = 10,
			LSHI = 11, ASHI = 12, LDW = 13, LDB = 14, POP = 15, STW = 16,
			STB = 17, PSH = 18, BEQ = 19, BNE = 20, BLT = 21, BGE = 22,
			BGT = 23, BLE = 24, HIGHEST_FORMAT_1 = 29;

	// format 2 instructions
	public static final int ADD = 30, SUB = 31, MUL = 32, DIV = 33, MOD = 34,
			CMP = 35, CHK = 36, AND = 37, BIC = 38, OR = 39, XOR = 40,
			LSH = 41, ASH = 42, PRNI = 43, PRNC = 44, PRNB = 45,
			HIGHEST_FORMAT_2 = 49;

	// format 3 instructions
	public static final int BSR = 50, RET = 51, HIGHEST_FORMAT_3 = 63;

	public static SymbolTableList symbolTable;

	/*
	 * Generate primitive Data Types according to the Class Type Descriptor we
	 * defined This data types are needed for the Symbol table entries
	 */
	final static TypeDesc INTTYPE = new TypeDesc(2, TypeDesc.DataType.intT, 1);
	final static TypeDesc BOOLTYPE = new TypeDesc(2, TypeDesc.DataType.boolT, 1);
	final static TypeDesc CHARTYPE = new TypeDesc(2, TypeDesc.DataType.charT, 1);

	public static HashMap<String, Integer> fixupTable = new HashMap<String, Integer>();
	public static HashMap<String, TypeDesc> ObjectTypes = new HashMap<String, TypeDesc>();
	/*
	 * Generate array with compile-time TypeDesc for arrays,objects,...
	 * 
	 */
	static Vector<TypeDesc> typeDescArray = new Vector<TypeDesc>();

	// TODO passt noch nicht hab ihn nur mal angelegt um weitermachen zu koennen
	final static TypeDesc STRINGTYPE = new TypeDesc(2, TypeDesc.DataType.charT,
			1);

	// Register Pointer
	static int topReg;

	static int heap;

	final static int LNK = 31; // define Linkregister
	final static int SP = 30; // define Stackpointer
	final static int FP = 29; // define Framepointer

	static int PC = 1;

	// Vector whith the created opCode
	static Vector<OpCodeElement> opCode = new Vector<OpCodeElement>();

	// some helper, fixup, ... fields
	private static int methodFix;
	public static int mainAddr = -50;

	public static int symbolTableLength;

	public static class OpCodeElement {
		int Instruction;
		String opString;
		int a, b, c, extra;

		// F1
		public OpCodeElement(String opString, int instruction, int first,
				int second, int third) {
			this.Instruction = instruction;
			this.opString = opString;
			a = first;
			b = second;
			c = third;

		}

		public OpCodeElement(String opString, int instruction, int first,
				int second) {
			this.Instruction = instruction;
			this.opString = opString;
			a = first;
			c = second;
		}
	}

	public CodeGenerator() {

		/* create SymbolList */
		CodeGenerator.symbolTable = new SymbolTableList();
		topReg = 0;
		heap = 0;
		putOpCode(new OpCodeElement("ADDI", ADDI, SP, 0, 4096));
	}

	public static int getCurrentReg() {
		return topReg;
	}

	public static int nextReg() {
		increaseReg();
		return topReg;
	}

	public static void increaseReg() {
		topReg++;
	}

	public static void decreaseReg() {
		topReg--;
	}

	/**
	 * Add opCode element to the vector
	 * 
	 * @param code
	 */
	public static void putOpCode(OpCodeElement code) {
		opCode.add(code);
		PC += 1;
	}

	/**
	 * Parser calls this method and writes the value of an Identifier into the
	 * next free Register
	 * 
	 * @param SymbolTableCell
	 *            TypeDes
	 * @return TypeDesc
	 * @throws TypeErrorException
	 */
	public static void loadWordType(SymbolTableCell cell, boolean global) {
		int b = FP;
		int offset = cell.getOffset();
		if (cell.isGlobalScope()) {
			b = heap;
			offset = 0 - offset; // heap must have positive offsets
		}
		assert (cell.getClassType() != SymbolTableCell.ClassType.var) : "INTERNAL ERROR IN CODE GEN. in writeIdentifierToRegister() cell is not a variable. BAD CLASS TYPE !";
		putOpCode(new OpCodeElement("LDW", LDW, nextReg(), b, offset));
	}

	public static void loadWordType(SymbolTableCell cell) {
		loadWordType(cell, false);
	}

	/**
	 * x+1 in this expression the methods take care about the 1 ADDI
	 * nextRegister,0,1
	 */
	public static void addI(int val) {
		putOpCode(new OpCodeElement("ADDI", ADDI, nextReg(), 0, val));
	}

	public static void invertValofLastReg() {
		putOpCode(new OpCodeElement("SUB", SUB, getCurrentReg(), 0,
				getCurrentReg()));

	}

	/**
	 * MUL 2,2,3 wirth neues Buch Seite 62
	 */
	public static void putOperation2Reg(String kind) {
		int op = 0;
		if (kind.equals("ADD"))
			op = ADD;
		else if (kind.equals("SUB"))
			op = SUB;
		else if (kind.equals("MUL"))
			op = MUL;
		else if (kind.equals("MOD"))
			op = MOD;
		else if (kind.equals("DIV"))
			op = DIV;
		putOpCode(new OpCodeElement(kind, op, topReg - 1, topReg - 1, topReg));
		decreaseReg();
	}

	/**
	 * Immediate
	 * 
	 * @param String
	 *            e.g ADD, SUB
	 */
	public static void putImOp2Reg(String kind, int value) {
		int op = 0;
		if (kind.equals("ADD"))
			op = ADDI;
		else if (kind.equals("SUB")) {
			op = SUBI;
		} else if (kind.equals("MUL"))
			op = MULI;
		else if (kind.equals("MOD"))
			op = MODI;
		else if (kind.equals("DIV"))
			op = DIVI;
		putOpCode(new OpCodeElement(kind.concat("I"), op, topReg, topReg, value));
	}

	/**
	 * Stores last Value of last Register into the give cell.
	 * 
	 * STW 1,0,obj.val
	 * 
	 * @param cell
	 */
	public static void storeWord(SymbolTableCell cell) {

		int b = FP;
		int offset = cell.getOffset();
		if (cell.isGlobalScope()) {
			b = heap;
			offset = 0 - offset; // heap must have positive offsets
		}
		putOpCode(new OpCodeElement("STW", STW, getCurrentReg(), b, offset));
		decreaseReg();
	}

	/**
	 * Adds frame pointer to last register and stores indexed array element
	 * 
	 * @param int
	 *            scope 1 ... local scope 2 ... global scope
	 */
	public static void storeWordArray(boolean globalScope) {

		if (globalScope)
			putOpCode(new OpCodeElement("ADD", ADD, getCurrentReg() - 1, heap,
					getCurrentReg() - 1));
		else
			putOpCode(new OpCodeElement("ADD", ADD, getCurrentReg() - 1, FP,
					getCurrentReg() - 1));

		putOpCode(new OpCodeElement("STW", STW, getCurrentReg(),
				getCurrentReg() - 1, 0));
		decreaseReg();
		decreaseReg();
	}

	public static void loadWordArray(boolean globalScope) {
		if (globalScope)
			putOpCode(new OpCodeElement("ADD", ADD, getCurrentReg(), heap,
					getCurrentReg()));
		else
			putOpCode(new OpCodeElement("ADD", ADD, getCurrentReg(), FP,
					getCurrentReg()));

		putOpCode(new OpCodeElement("LDW", LDW, getCurrentReg(),
				getCurrentReg(), 0));
	}

	/**
	 * Type Safe
	 * 
	 * @param cell
	 * @throws TypeErrorException
	 */
	public static void storeWordCell(SymbolTableCell cell, TypeDesc type)
			throws TypeErrorException {
		// typeChecking(cell);
		storeWord(cell);
	}

	/**
	 * push last register entrie onto stack
	 */
	public static void pushRegister() {
		putOpCode(new OpCodeElement("PSH", PSH, getCurrentReg(), SP, 1));
		decreaseReg();
	}

	// private static void typeChecking(SymbolTableCell cell, TypeDesc type)
	// throws TypeErrorException {
	// if (cell.getType().equals(type)) {
	// ;
	// } else {
	// throw new TypeErrorException("Illegal type Error, expected: " +
	// type.getBase().toString());
	// }
	// }

	public static int methodCall(int proc) {
		putOpCode(new OpCodeElement("BSR", BSR, 0, 0, proc));
		return (PC - 2); // is only used for fixing up the main method entry
		// after global vars
	}

	/**
	 * for method declarations
	 */
	public static int methodPrologue() {
		int methodsPC = PC;
		putOpCode(new OpCodeElement("PSH", PSH, LNK, SP, 1));
		putOpCode(new OpCodeElement("PSH", PSH, FP, SP, 1));
		putOpCode(new OpCodeElement("ADD", ADD, FP, 0, SP));
		putOpCode(new OpCodeElement("SUBI", SUBI, SP, SP, -100));
		// -100 needs to be replaced by the right size when method is finished
		// remember opcode Element of array in global variable
		methodFix = opCode.size() - 1;
		return methodsPC;
	}

	public static void methodEpilogue(int size) {
		methodEpilogueMain(size, false);
	}

	public static void methodEpilogueMain(int size, boolean main) {
		putOpCode(new OpCodeElement("ADD", ADD, SP, 0, FP));
		putOpCode(new OpCodeElement("POP", POP, FP, SP, 1));
		putOpCode(new OpCodeElement("POP", POP, LNK, SP, 1));
		if (!main)
			putOpCode(new OpCodeElement("RET", RET, 0, 0, LNK));
		// fixup size of method prolog
		opCode.get(methodFix).c = size;
	}

	/**
	 * Need not to validat if main is already set, because Symboltable does
	 * already verify unique Identifiers
	 */
	public static void setMainPC() {
		mainAddr = PC;
	}

	/**
	 * prints integer variables
	 * 
	 * @param offset
	 */
	public static void printIO(TypeDesc type) {
		if (type == INTTYPE)
			putOpCode(new OpCodeElement("PRNI", PRNI, 0, 0, getCurrentReg()));

		if (type == CHARTYPE)
			putOpCode(new OpCodeElement("PRNC", PRNC, 0, 0, getCurrentReg()));

		if (type == BOOLTYPE)
			putOpCode(new OpCodeElement("PRNB", PRNB, 0, 0, getCurrentReg()));
	}

	public static void fixMainProc(int vecPos, int proc) {
		opCode.get(vecPos).c = proc;
	}

	/*
	 * Conditions and repeats
	 */
	public static int relation(int op) {
		putOpCode(new OpCodeElement("CMP", CMP, getCurrentReg() - 1,
				getCurrentReg() - 1, getCurrentReg()));
		decreaseReg();
		putOpCode(new OpCodeElement("OP", op, getCurrentReg(), -100));
		decreaseReg();
		return PC - 2;
	}

	public static int elseAndLoopJump(int pos) {
		putOpCode(new OpCodeElement("BEQ", BEQ, 0, pos));
		return PC - 2;
	}

	public static void fixConditionJump(Vector<Integer> fixPC, int falseJump,
			int trueJump) {
		ListIterator<Integer> iter = fixPC.listIterator();
		while (iter.hasNext()) {
			OpCodeElement opElem = opCode.get(iter.next());
			if (opElem.c == -100) // False Jump
				opElem.c = falseJump;
			else if (opElem.c == -1000) // True Jump
				opElem.c = trueJump;
			else
				; // Jump is already fixed !
		}
	}

	public static void fixConditionJump(int fixPC, int falseJump, int trueJump) {
		OpCodeElement opElem = opCode.get(fixPC);
		if (opElem.c == -100) // False Jump
			opElem.c = falseJump;
		else if (opElem.c == -1000) // True Jump
			opElem.c = trueJump;

	}

	public static void writeObjectFile(String filename) {
		ObjectFile objectFile = new ObjectFile(filename.concat(".obj"), "rw");
		try {
			objectFile.file.writeInt(0);
			int number = (BSR << 26) + (mainAddr);
			objectFile.file.writeInt(number);
			HashMap<String, Integer> map = symbolTable.getGlobalSymList();
			objectFile.file.writeInt(symbolTableLength);
			objectFile.writeTable(map);
			// lenght of fixup Table
			objectFile.file.writeInt(getLenghtofTable(fixupTable));
			// fixup Table
			objectFile.writeTable(fixupTable);
			// opCode
			writeOpCode(objectFile.file);
			objectFile.file.close();
			System.out.println("Outputfile: " + filename.concat(".obj"));
			System.out.println("");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static int getLenghtofTable(HashMap<String, Integer> map) {
		int size = 0;
		for (String name : map.keySet()) {
			name = name.concat("=");
			int count = name.length();
			if (count % 4 == 0)
				size += name.length() / 4 + 1;
			else
				size += name.length() / 4 + 2;
		}
		return size;
	}

	/**
	 * if main is available and no Class is importet write binary file, esle
	 * write object file.
	 */
	public static void writeOutputFile(String name) {
		if (mainAddr != -50 && fixupTable.isEmpty())
			writeBinaryFile(name);
		else
			writeObjectFile(name);
	}

	public static void writeBinaryFile(String name) {

		File test = new File(name.concat(".bin"));
		test.delete();

		RandomAccessFile output = null;
		try {
			output = new RandomAccessFile(name.concat(".bin"), "rw");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			output.writeInt(1);
			writeOpCode(output);
			output.close();
			System.out.println("Outputfile: " + name.concat(".bin"));
			System.out.println("");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void writeOpCode(RandomAccessFile output) throws IOException {
		int i = 0;
		if (mainAddr == -50)
			i = 1;
		for (; i < opCode.size(); i++) {
			int number = 0;

			// F1
			if (opCode.get(i).Instruction < HIGHEST_FORMAT_1) {

				int sign = 0;
				if (opCode.get(i).c < 0) {
					sign = 1;
					opCode.get(i).c = 0 - opCode.get(i).c;
				}

				number = (opCode.get(i).Instruction << 26)
						+ (opCode.get(i).a << 21) + (opCode.get(i).b << 16)
						+ (sign << 15) + (opCode.get(i).c);

			}
			// F2
			else if (opCode.get(i).Instruction < HIGHEST_FORMAT_2)
				number = (opCode.get(i).Instruction << 26)
						+ (opCode.get(i).a << 21) + (opCode.get(i).b << 16)
						+ (opCode.get(i).extra << 5) + (opCode.get(i).c);

			// F3
			else if (opCode.get(i).Instruction < HIGHEST_FORMAT_3)
				number = (opCode.get(i).Instruction << 26) + (opCode.get(i).c);

			output.writeInt(number);
			Util.debug2("PC " + (i + 1) + "   "
					+ Integer.toBinaryString(number) + " "
					+ opCode.get(i).opString + " (" + opCode.get(i).Instruction
					+ ") " + opCode.get(i).a + " " + opCode.get(i).b + " "
					+ opCode.get(i).c);
		}

	}

	// make true jumps
	public static void fixOrJumps(int pos, boolean falseJumps, boolean and) {
		if (falseJumps) {
			int op = opCode.get(pos).Instruction;
			opCode.get(pos).Instruction = invertRelation(op);
			opCode.get(pos).c = -1000;
		} else {
			int op = opCode.get(pos).Instruction;
			if (and) {
				if (opCode.get(pos).c == -1000)
					opCode.get(pos).c = PC;
			} else {
				if (opCode.get(pos).c == -100)
					opCode.get(pos).c = PC;
			}
		}
	}

	public static int invertRelation(int op) {
		if (op == BNE)
			return BEQ;
		else if (op == BEQ)
			return BNE;
		else if (op == BLE)
			return BGT;
		else if (op == BGT)
			return BLE;
		else if (op == BGE)
			return BLT;
		else if (op == BLT)
			return BGE;
		else
			System.out
					.println("ERROR in Codegeneration by inverting a Relation");

		return 0;
	}

	public static int boolAss(boolean Imm, int value) {
		if (Imm)
			putOpCode(new OpCodeElement("ADDI", ADDI, nextReg(), 0, value));
		putOpCode(new OpCodeElement("BNE", BEQ, getCurrentReg(), -100));
		decreaseReg();
		return PC - 2;
	}
}
