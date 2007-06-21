package compiler;

import java.util.Vector;

import compiler.Util.TypeErrorException;

public class CodeGenerator {

	public static SymbolTableList symbolTable;

	/* Generate primitive Data Types according to the Class Type Descriptor we defined
	 * This data types are needed for the Symbol table entries
	 */
	final static TypeDesc INTTYPE = new TypeDesc(2, TypeDesc.DataType.intT, 1);
	final static TypeDesc BOOLTYPE = new TypeDesc(2, TypeDesc.DataType.boolT, 1);
	final static TypeDesc CHARTYPE = new TypeDesc(2, TypeDesc.DataType.charT, 1);

	// Register Pointer
	static int topReg;

	// Vector whith the created opCode
	static Vector<OpCodeElement> opCode = new Vector<OpCodeElement>();

	public static class OpCodeElement {
		String Instruction;
		int f, s, t;

		public OpCodeElement(String instruction, int first, int second,
				int third) {
			this.Instruction = instruction;
			f = first;
			s = second;
			t = third;
		}
	}

	public CodeGenerator() {

		/* create SymbolList */
		this.symbolTable = new SymbolTableList();
		topReg = 0;

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

	public static void putOpCode(OpCodeElement code) {
		opCode.add(code);
	}

	public static void printOpCode() {
		int i = 0;
		while (i < opCode.size()) {
			System.out.println(i + " " + opCode.get(i).Instruction + " " +
					opCode.get(i).f + "," + opCode.get(i).s + "," +
					opCode.get(i).t);
			i++;
		}
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
		putOpCode(new OpCodeElement("LDW", nextReg(), 0, cell.getOffset()));
	}

	/**
	 * x+1 from this expression the methods take care about the 1
	 * ADDI nextRegister,0,1
	 */
	public static void addI(int val) {
		putOpCode(new OpCodeElement("ADDI", nextReg(), 0, val));
	}

	/** 
	 * MUL 2,2,3 wirth neues Buch Seite 62
	 */
	public static void putOperation2Reg(String kind) {
		putOpCode(new OpCodeElement(kind, topReg - 1, topReg - 1, topReg));
		decreaseReg();
	}

	/**
	 * STW 1,0,obj.val
	 * 
	 * @param cell
	 */
	public static void storeWord(SymbolTableCell cell) {
		putOpCode(new OpCodeElement("STW", getCurrentReg(), 0, cell.getOffset()));
		decreaseReg();
	}

	/**
	 * Type Safe
	 * @param cell
	 * @throws TypeErrorException 
	 */
	public static void storeWord(SymbolTableCell cell, TypeDesc type)
			throws TypeErrorException {
		typeChecking(cell, type);
		storeWord(cell);
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

}
