package compiler;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.*;


import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import com.sun.xml.internal.ws.streaming.XMLReader;
import org.w3c.dom.Document;

import compiler.SymbolTableCell.ClassType;
import compiler.Util.IllegalTokenException;

/** 
 * Representing a symbol table list
 * Symbol table list is a Linked List consisting of Symbol Table Cells.
 *
 */
public class SymbolTableList {

	private LinkedList<SymbolTableCell> symList = new LinkedList<SymbolTableCell>();

	private Vector<String> moduleList = new Vector<String>();
	
	
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
	 * @param String ClassType DataType int String boolean (set boolean always to true !)
	 */
	public void addSym(String name, ClassType classType, TypeDesc type, int size,
			boolean globalScope) {

		boolean foundSymbol = false;
		ListIterator<SymbolTableCell> iterator = symList.listIterator();

		/*add method declarations always to global scope*/
		if (classType == SymbolTableCell.ClassType.method)
			scope = 0;

		/* check if symbol does exist already  */
		if (scope == 0 && iterator.hasNext() && getSymbol(name) != null)
			foundSymbol = true;
		/* check the global scope if global scope is selected */
		else if (scope == 1 && symList.getLast().methodSymbols.getSymbol(name) != null)
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
			symList.addLast(new SymbolTableCell(name, classType, type, offset, size, globalScope));
		}

		/* add local vars in the sublist of the last method */
		if (scope == 1) {
			symList.getLast().methodSymbols.addSym(name, classType, type, size, false);
		}

		/* if scope is 1 a method declaration does already exist, but to add the next Method to the global scope (class scope) do following:
		 * set scope to 0 and add the method to the class symbol list and set scope to 1 again*/
		if (classType == SymbolTableCell.ClassType.method) {
			scope = 1; /* switch scope to local vars after a method declaration because class variables are not allowed now ! */
		}

	}

	/**
	 * Adds a moduleName to the moduleList. A moduleName is read from the import statement
	 * @param moduleName
	 * @author Lacki
	 */
	public void addModule(String moduleName) {
		if (moduleName.equals("") == false) {
			moduleList.addElement(moduleName);
		}
	}
	
		
	/**
	 * Writes the content of the moduleList in a XML-File (the symbolfile)
	 * @author lacki
	 * @param symbolFile
	 */
	public void exportModuleAnchors(SymbolFile symbolFile) {
		
		symbolFile.appendModuleAnchors(moduleList);		
	}
	
	
	/**
	 * Writes the content of the symbolTable in a XML-File (the symbolfile)
	 * @author lacki
	 * @param symbolFile
	 */
	public void exportSymbols(SymbolFile symbolFile) {
		
		symbolFile.appendLine("<symbols>");
		
		ListIterator<SymbolTableCell> iter = symList.listIterator();
		
		int i = 0;
		while (iter.hasNext()) {
			SymbolTableCell currentCell = iter.next();
			i++;

			if ((currentCell.isGlobalScope()) || (currentCell.getOffset() > 0)) {
				
				if (currentCell.getClassType() == SymbolTableCell.ClassType.var) {
					
					symbolFile.appendLine("<variable>");
					
					symbolFile.appendLine("<type>" + currentCell.getType().base + "</type>");
					symbolFile.appendLine("<name>" + currentCell.getName() + "</name>");
					
					symbolFile.appendLine("</variable>");
				
				} else if (currentCell.getClassType() == SymbolTableCell.ClassType.array) {
					
					symbolFile.appendLine("<array>");
					symbolFile.appendLine("<type>" + currentCell.getType().base + "</type>");
					symbolFile.appendLine("<name>" + currentCell.getName() + "</name>");
					symbolFile.appendLine("<size>" + currentCell.getSize() + "</size>");
					symbolFile.appendLine("</array>");					
					
				} else if (currentCell.getClassType() == SymbolTableCell.ClassType.method) {
					
					symbolFile.appendLine("<method>");
					symbolFile.appendLine("<type>" + currentCell.getType().base + "</type>");
					symbolFile.appendLine("<name>" + currentCell.getName() + "</name>");
					symbolFile.appendLine("<size>" + currentCell.getType().getSize() + "</size>");
							
					currentCell.methodSymbols.exportSymbols(symbolFile);
					
					symbolFile.appendLine("</method>");
					
				}	
			}
			
		}
		
		symbolFile.appendLine("</symbols>");	
		
	}
	
	
	public void importModuleAnchors(SymbolFile symbolFile) {
		
		Vector<String> modules = new Vector<String>();
		
		modules = symbolFile.readModuleAnchors();
		
		for (int i = 0; i < modules.size(); i++) {
			System.out.println("module: " + modules.elementAt(i));
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
				returnCell = currentCell;
				/*when var is a method then only search the global list and skip last local*/
				if (returnCell.getClassType() == SymbolTableCell.ClassType.method)
					break;
			}

			/*search local vars*/
			if (currentCell.getClassType() == SymbolTableCell.ClassType.method && !iter.hasNext()) {
				SymbolTableCell tmp = currentCell.methodSymbols.getSymbol(name);
				if (tmp!=null)
					returnCell = tmp;
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
			System.out.println(i + " Name: " + currentCell.getName() + " type: " +
					currentCell.getType().getBase().toString() + " size:  " +
					currentCell.getSize() + " offset: " + currentCell.getOffset() +
					" globalScope: " + currentCell.isGlobalScope());
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
