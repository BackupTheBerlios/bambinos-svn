package compiler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import sun.reflect.generics.scope.Scope;

import compiler.SymbolTableCell.ClassType;

public class ParseSymbolFile extends DefaultHandler {
	static private Writer out = null;
	private StringBuffer textBuffer = null;
	private boolean startpars = false;
	private SymbolTableCell cell;
	static SymbolTableList list;
	private int nameMode; // 1.. type 2.. name 3.. size 0.. nothing
	private boolean globalScope = true;
	private String methodName;
	private boolean methodMode;

	public ParseSymbolFile(String fileName, SymbolTableList list) {
		ParseSymbolFile.list = list;
		parse(fileName);

	}

	public ParseSymbolFile(SymbolTableList list) {
		ParseSymbolFile.list = list;
	}

	public static void parse(String fileName) {
		try {
			// Use an instance of ourselves as the SAX event handler
			DefaultHandler handler = new ParseSymbolFile(list);
			// Parse the input with the default (non-validating) parser
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			saxParser.parse(new File(fileName), handler);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void startElement(String namespaceURI, String localName, // local name
			String qName, // qualified name
			Attributes attrs) throws SAXException {
		echoTextBuffer();
		String eName = ("".equals(localName)) ? qName : localName;
		if (startpars) {
			addString("<" + eName); // element name
			//echoString(">");
		} else if (eName.equals("symbols")) //start parsing
			startpars = true;
	}

	public void endElement(String namespaceURI, String localName, // local name
			String qName) // qualified name
			throws SAXException {
		echoTextBuffer();
		String eName = ("".equals(localName)) ? qName : localName;
		addString("</" + eName + ">"); // element name
	}

	public void characters(char[] buf, int offset, int len) throws SAXException {
		String s = new String(buf, offset, len);
		if (textBuffer == null)
			textBuffer = new StringBuffer(s);
		else
			textBuffer.append(s);
	}

	// ---- Helper methods ----

	// Display text accumulated in the character buffer
	private void echoTextBuffer() throws SAXException {
		if (textBuffer == null)
			return;
		addString(textBuffer.toString());
		textBuffer = null;
	}

	// Wrap I/O exceptions in SAX exceptions, to
	// suit handler signature requirements
	private void addString(String s) throws SAXException {
		if (s.equals("<variable") || s.equals("<array")) {
			cell = new SymbolTableCell(ClassType.var, globalScope, false);
		} else if (s.equals("</variable>") || s.equals("</array>")) {
			if (cell != null) {
				if (globalScope)
					list.add(cell);
				else
					list.getSymbol(methodName).methodSymbols.add(cell);
			}
		} else if (s.equals("<method")) {
			cell = new SymbolTableCell(ClassType.var, true, true);
			methodMode=true;
			globalScope = false;
//		} else if (s.equals("</symbolfile>")) {
//			if (globalScope)
//				list.add(cell);
//			else
//				list.getSymbol(methodName).methodSymbols.add(cell);
		} else if (s.equals("<symbols") && cell != null) {
			list.add(cell);
			methodMode=false;
			cell = new SymbolTableCell(ClassType.var, globalScope, false);
		} else if (s.equals("</symbols>"))
			globalScope = true;

		if (nameMode != 0) {
			if (nameMode == 1)
				cell.setType(s);
			else if (nameMode == 2) {
				cell.setName(s);
				if (methodMode)
					methodName = s;
			} else if (nameMode == 3)
				cell.setSize(Integer.parseInt(s));
			nameMode = 0;
		}

		if (s.equals("<type"))
			nameMode = 1;
		else if (s.equals("<name"))
			nameMode = 2;
		else if (s.equals("<size"))
			nameMode = 3;

		try {
			if (null == out)
				out = new OutputStreamWriter(System.out, "UTF8");
//			out.write(s);
//			out.flush();
		} catch (IOException ex) {
			throw new SAXException("I/O error", ex);
		}
	}
}