package compiler;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class SymbolFile {

	private String filename;
	private RandomAccessFile file;

	public SymbolFile(String filename) {

		this.filename = filename.concat(".sym");

	}

	/**
	 * Deletes an old symbolfile and creates a new one
	 * @author Lacki
	 */
	private void createFile() {

		this.file = null;

		File tmpFile = new File(this.filename);
		tmpFile.delete();

		try {
			this.file = new RandomAccessFile(this.filename, "rw");
		} catch (IOException io) {
			System.out.println("ERROR: Cannot open to symbolfile for writing");
		}
	}

	/**
	 * Writes a String to the symbolfile closed by a "\n"
	 * @param symbolFile 
	 * @param line
	 * @author Lacki
	 */
	public void appendLine(String line) {

		try {
			this.file.writeBytes(line + "\n");
		} catch (IOException io) {
			System.out.println("ERROR: Cannot append text to symbolfile");
		}

	}

	/**
	 * Creates the xmlfile and Writes the xml-header and the first xml-tag to the file
	 * @author Lacki
	 */
	public void writeHeader() {

		this.createFile();

		try {
			this.file = new RandomAccessFile(this.filename, "rw");
		} catch (IOException io) {
			System.out.println("ERROR: Cannot open to symbolfile for writing");
		}

		appendLine("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
		appendLine("<symbolfile>");

	}

	/**
	 * Writes the footer "</symbolfile>" to the symbolfile
	 * @author Lacki
	 */
	public void writeFooter() {

		appendLine("</symbolfile>");

	}

	/**
	 * Writes the content of the moduleList to the symbolfile
	 * @author lacki
	 * @param moduleList
	 */
	public void appendModuleAnchors(Vector<String> moduleList) {

		this.appendLine("<modules>");

		for (int i = 0; i < moduleList.size(); i++) {
			this.appendLine("<module>");
			this.appendLine("<name>" + moduleList.elementAt(i) + "</name>");
			this.appendLine("</module>");
		}

		this.appendLine("</modules>");

	}

//	/**
//	 * 
//	 * @param filename
//	 * @return SymbolTableList
//	 */
//	public SymbolTableList createSymboltableFromFile(String filename) {
//
//		DOMParser parser = new DOMParser();
//
//		try {
//
//			parser.parse(this.filename);
//
//			Document doc = parser.getDocument();
//
//			Node root = doc.g
//
//			NodeList nodes = root.getChildNodes();
//
//			for (int i = 0; i < nodes.getLength(); i++) {
//
//				if (nodes.item(i).getNodeName() == "modules") {
//					modules = nodes.item(i).getChildNodes();
//
//					for (int j = 0; j < modules.getLength(); j++) {
//
//						Node module = modules.item(j);
//
//						NodeList moduleParameters = module.getChildNodes();
//
//						for (int k = 0; k < moduleParameters.getLength(); k++) {
//
//							Node name = moduleParameters.item(k);
//
//							if (name.getNodeName().equals("name")) {
//								System.out.println(name.getNodeValue());
//								moduleList.addElement(name.getNodeValue());
//							}
//
//						}
//
//					}
//
//					break;
//				}
//			}
//		} catch (SAXException sax) {
//			System.out.println("ERROR: Invalid xml-syntax");
//			return null;
//		} catch (IOException io) {
//			System.out.println("ERROR: reading symbolfile");
//			return null;
//		}
//
//	}

	private String readModuleName(NodeList nodes) {
		String name = new String();

		for (int i = 0; i < nodes.getLength(); i++) {

//			System.out.println("name: " + nodes.item(i).getNodeName());
//			System.out.println("type: " + nodes.item(i).getNodeType());
//			System.out.println("value: " + nodes.item(i).getNodeValue());

			if (nodes.item(i).getNodeName().equals("name")) {
				/*
				if (nodes.item(i).getNodeType() == 1) {
					System.out.println(nodes.item(i).getNodeType());
				}
				*/
				name = nodes.item(i).getTextContent();

			} else {
				name = readModuleName(nodes.item(i).getChildNodes());
			}

		}

		return name;
	}

}
