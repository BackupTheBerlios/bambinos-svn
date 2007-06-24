package compiler;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.Vector;

import sun.security.util.BitArray;

import compiler.Util.TypeErrorException;

public class CodeGenerator {

	// format 1 instructions
	private static final int ADDI = 0, SUBI = 1, MULI = 2, DIVI = 3, MODI = 4,
			CMPI = 5, CHKI = 6, ANDI = 7, BICI = 8, ORI = 9, XORI = 10,
			LSHI = 11, ASHI = 12, LDW = 13, LDB = 14, POP = 15, STW = 16,
			STB = 17, PSH = 18, BEQ = 19, BNE = 20, BLT = 21, BGE = 22,
			BGT = 23, BLE = 24, HIGHEST_FORMAT_1 = 29;

	// format 2 instructions
	private static final int ADD = 30, SUB = 31, MUL = 32, DIV = 33, MOD = 34,
			CMP = 35, CHK = 36, AND = 37, BIC = 38, OR = 39, XOR = 40,
			LSH = 41, ASH = 42, PRNI = 43, PRNC = 44, HIGHEST_FORMAT_2 = 49;

	// format 3 instructions
	private static final int BSR = 50, RET = 51, HIGHEST_FORMAT_3 = 63;

	public static SymbolTableList symbolTable;

	/* Generate primitive Data Types according to the Class Type Descriptor we defined
	 * This data types are needed for the Symbol table entries
	 */
	final static TypeDesc INTTYPE = new TypeDesc(2, TypeDesc.DataType.intT, 1);
	final static TypeDesc BOOLTYPE = new TypeDesc(2, TypeDesc.DataType.boolT, 1);
	final static TypeDesc CHARTYPE = new TypeDesc(2, TypeDesc.DataType.charT, 1);

	// TODO passt noch nicht hab ihn nur mal angelegt um weitermachen zu koennen
	final static TypeDesc STRINGTYPE = new TypeDesc(2, TypeDesc.DataType.charT,
			1);

	// Register Pointer
	static int topReg;

	final static int LNK = 31; // define Linkregister
	final static int SP = 30; // define Stackpointer
	final static int FP = 29; // define Framepointer

	static int PC = 1;

	// Vector whith the created opCode
	static Vector<OpCodeElement> opCode = new Vector<OpCodeElement>();

	// some helper, fixup, ... fields
	private static int methodFix;
	private static int mainAddr = 0;

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
			b = second;
		}
	}

	public CodeGenerator() {

		/* create SymbolList */
		CodeGenerator.symbolTable = new SymbolTableList();
		topReg = 0;
		putOpCode(new OpCodeElement("ADD", ADD, SP, 0, 4096));

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
	 * @param code
	 */
	public static void putOpCode(OpCodeElement code) {
		opCode.add(code);
		PC += 1;
	}

	/** 
	 * Parser calls this method and writes the value of an Identifier into the next free Register, if the type is the same as passed.
	 * 
	 *  @param SymbolTableCell TypeDes
	 *  @return TypeDesc
	 * @throws TypeErrorException 
	 */
	public static void loadWordType(SymbolTableCell cell, TypeDesc type)
			throws TypeErrorException {

		// sollte immer eine Var sein, sonst stimmt der Aufruf vom Parser aus nicht. dann waere irgendwo ein logische Fehler
		// to enable assertions compile with javac flag "-ea"
		assert (cell.getClassType() != SymbolTableCell.ClassType.var) : "INTERNAL ERROR IN CODE GEN. in writeIdentifierToRegister() cell is not a variable. BAD CLASS TYPE !";

		typeChecking(cell, type);
		putOpCode(new OpCodeElement("LDW", LDW, nextReg(), 0, cell.getOffset()));
	}

	/**
	 * x+1 in this expression the methods take care about the 1
	 * ADDI nextRegister,0,1
	 */
	public static void addI(int val) {
		putOpCode(new OpCodeElement("ADDI", ADDI, nextReg(), 0, val));
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
	 */
	public static void putImOp2Reg(String kind, int value) {
		int op = 0;
		if (kind.equals("ADD"))
			op = ADDI;
		else if (kind.equals("SUB"))
			op = SUBI;
		else if (kind.equals("MUL"))
			op = MULI;
		else if (kind.equals("MOD"))
			op = MODI;
		else if (kind.equals("DIV"))
			op = DIVI;
		putOpCode(new OpCodeElement(kind, op, topReg, topReg, value));
	}

	/**
	 * STW 1,0,obj.val
	 * 
	 * @param cell
	 */
	public static void storeWord(SymbolTableCell cell) {
		putOpCode(new OpCodeElement("STW", STW, getCurrentReg(), 0, cell
				.getOffset()));
		decreaseReg();
	}

	/**
	 * Type Safe
	 * @param cell
	 * @throws TypeErrorException 
	 */
	public static void storeWordCell(SymbolTableCell cell, TypeDesc type)
			throws TypeErrorException {
		typeChecking(cell, type);
		storeWord(cell);
	}

	/**
	 * push last register entrie onto stack
	 */
	public static void pushRegister() {
		putOpCode(new OpCodeElement("PSH", PSH, getCurrentReg(), SP, 1));
	}

	private static void typeChecking(SymbolTableCell cell, TypeDesc type)
			throws TypeErrorException {
		if (cell.getType().equals(type)) {
			;
		} else {
			throw new TypeErrorException("Illegal type Error, expected: " +
					type.getBase().toString());
		}
	}

	public static int methodCall(int proc) {
		putOpCode(new OpCodeElement("BSR", BSR, 0, 0, proc));
		return (PC - 2); // is only used for fixing up the main method entry after global vars
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
		putOpCode(new OpCodeElement("ADD", ADD, SP, 0, FP));
		putOpCode(new OpCodeElement("POP", POP, FP, SP, 1));
		putOpCode(new OpCodeElement("POP", POP, LNK, SP, 1));
		putOpCode(new OpCodeElement("RET", RET, 0, 0, LNK));
		// fixup size of method prolog 
		opCode.get(methodFix).c = size + 1;
	}

	/**
	 * Need not to validat if main is already set, because Symboltable does already verify unique Identifiers
	 */
	public static void setMainPC() {
		mainAddr = PC;
	}

	/**
	 * prints integer variables
	 * @param offset
	 */
	public static void printIO(int offset) {
		putOpCode(new OpCodeElement("PRNI", PRNI, FP, offset));
	}

	public static void fixMainProc(int vecPos, int proc) {
		opCode.get(vecPos).c = proc;
	}

	public static void write2File() {

		try {
			RandomAccessFile output = new RandomAccessFile(
					"integer_outputfile1.txt", "rw");

			output.writeInt(0);

			for (int i = 0; i < opCode.size(); i++) {
				int number = 0;

				// F1
				if (opCode.get(i).Instruction < HIGHEST_FORMAT_1) {

					int sign = 0;
					if (opCode.get(i).c < 0) {
						sign = 1;
						opCode.get(i).c = 0 - opCode.get(i).c;
					}

					number = (opCode.get(i).Instruction << 26) +
							(opCode.get(i).a << 21) + (opCode.get(i).b << 16) +
							(sign << 15) + (opCode.get(i).c);

				}
				// F2
				else if (opCode.get(i).Instruction < HIGHEST_FORMAT_2)
					number = (opCode.get(i).Instruction << 26) +
							(opCode.get(i).a << 21) + (opCode.get(i).b << 16) +
							(opCode.get(i).extra << 5) + (opCode.get(i).c);

				// F3
				else if (opCode.get(i).Instruction < HIGHEST_FORMAT_3)
					number = (opCode.get(i).Instruction << 26) +
							(opCode.get(i).c);

				output.writeInt(number);
				System.out.println("PC " + (i + 1) + "   " +
						opCode.get(i).opString + " (" +
						opCode.get(i).Instruction + ") " + opCode.get(i).a +
						" " + opCode.get(i).b + " " + opCode.get(i).c);
			}
			output.close();
		} catch (IOException io) {

		}

	}
}
