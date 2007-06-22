package compiler;

import java.util.LinkedList;
import java.util.ListIterator;

import compiler.SymbolTableCell.ClassType;
import compiler.Util.IllegalTokenException;

/** 
 * Representing a symbol table list
 * Symbol table list is a Linked List consisting of Symbol Table Cells.
 *
 */
public class SymbolTableList {

	private LinkedList<SymbolTableCell> symList = new LinkedList<SymbolTableCell>();

	private int scope = 0; // 0 ... add global symbols 1 ... add local symbols
	private int offset = 0; // global offset. First Symbol starts with 0 (4 bytes = 1 offset)

	/**
	 * Add new Symbol Tabel entry to the linked list
	 * First Add global symbols then local symbols in the sub linked list of each method
	 * 
	 * When the first method is added, then no class variables can be added. All variables following a method x
	 * will be added in the sub linked list of the method x. When the next method y is added, 
	 * all variables will be added in the method y and so on. 
	 * 
	 * @param String ClassType DataType int String
	 */
	public void addSym(String name, ClassType classType, TypeDesc type, int size) {

		boolean foundSymbol = false;
		ListIterator<SymbolTableCell> iterator = symList.listIterator();

		/*add method declarations always to global scope*/
		if (classType == SymbolTableCell.ClassType.method)
			scope = 0;

		/* check if symbol does exist already  */
		if (scope == 0 && iterator.hasNext() && getSymbol(name) != null)
			foundSymbol = true;
		/* check the global scope if global scope is selected */
		else if (scope == 1 &&
				symList.getLast().methodSymbols.getSymbol(name) != null)
			foundSymbol = true;

		if (foundSymbol) {
			try {
				Parser.syntaxError("Multiple declaration of field: " + name);
			} catch (IllegalTokenException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}

		/* add entry to list */
		if (scope == 0) {
			offset = offset - size;
			symList.addLast(new SymbolTableCell(name, classType, type, offset,
					size));
		}

		/* add local vars in the sublist of the last method */
		if (scope == 1)
			symList.getLast().methodSymbols.addSym(name, classType, type, size);

		/* if scope is 1 a method declaration does already exist, but to add the next Method to the global scope (class scope) do following:
		 * set scope to 0 and add the method to the class symbol list and set scope to 1 again*/
		if (classType == SymbolTableCell.ClassType.method) {
			scope = 1; /* switch scope to local vars after a method declaration because class variables are not allowed now ! */
		}

	}

	/**
	 * Identifier for searching is the name of the variable or method ....
	 * First the global scope will be search, next the last local scope. This means the scope of the last added method.
	 * 
	 * @param name
	 * @return SymbolTableCell
	 */
	public SymbolTableCell getSymbol(String name) {

		ListIterator<SymbolTableCell> iter = symList.listIterator();
		SymbolTableCell currentCell;
		SymbolTableCell returnCell = null; // first fields are assigned then when found it will be overwritten with local vars

		while (iter.hasNext()) {

			currentCell = iter.next();
			/* find vars in current scope */
			if (currentCell.getName().equals(name)) {
				System.out.println("Zelle () gefunden");
				returnCell = currentCell;
				/*when var is a method then only search the global list and skip last local*/
				if (returnCell.getClassType() == SymbolTableCell.ClassType.method)
					break;
			}

			/*search local vars*/
			if (currentCell.getClassType() == SymbolTableCell.ClassType.method &&
					!iter.hasNext()) {
				returnCell = currentCell.methodSymbols.getSymbol(name);
			}
		}

		return returnCell;
	}

	/**
	 * Searching for a SymboltableCell in a specific method.
	 * 
	 * @param String method in which to search for the variable name.
	 * @param String name of the searched variable
	 * @return SymbolTableCell
	 */
	public SymbolTableCell getLocalSymbol(String method, String name) {
		return getSymbol(method).methodSymbols.getSymbol(name);
	}

	public void printSymbolTable() {
		ListIterator<SymbolTableCell> iter = symList.listIterator();
		System.out.println("Symbol Table:");
		int i = 0;
		while (iter.hasNext()) {
			SymbolTableCell currentCell = iter.next();
			i++;
			System.out.println(i + " Name: " + currentCell.getName() +
					" type: " + currentCell.getType().getBase().toString() +
					" size:  " + currentCell.getSize() + " offset: " +
					currentCell.getOffset() + " value: ");
			;
			if (currentCell.getClassType() == SymbolTableCell.ClassType.method) {
				System.out.println("Symbol Table method: ");
				currentCell.methodSymbols.printSymbolTable();
				System.out.println(" END Symbol Table method ");
			}

		}
	}

	public int getCurrentOffset() {
		return offset;
	}

	public void fixOffset(int fix) {
		this.offset += fix;
	}

}
