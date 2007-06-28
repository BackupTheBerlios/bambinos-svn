

public class Parser {
	static final int _EOF = 0;
	static final int _equal = 1;
	static final int _greater = 2;
	static final int _smaller = 3;
	static final int _not = 4;
	static final int _AND = 5;
	static final int _OR = 6;
	static final int _number = 7;
	static final int _simpleIdentifier = 8;
	static final int _StringValue = 9;
	static final int _charValue = 10;
	static final int maxT = 44;

	static final boolean T = true;
	static final boolean x = false;
	static final int minErrDist = 2;

	public Token t;    // last recognized token
	public Token la;   // lookahead token
	int errDist = minErrDist;
	
	Scanner scanner;
	Errors errors;

	

	public Parser(Scanner scanner) {
		this.scanner = scanner;
		errors = new Errors();
	}

	void SynErr (int n) {
		if (errDist >= minErrDist) errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr (String msg) {
		if (errDist >= minErrDist) errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}
	
	void Get () {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) { ++errDist; break; }

			la = t;
		}
	}
	
	void Expect (int n) {
		if (la.kind==n) Get(); else { SynErr(n); }
	}
	
	boolean StartOf (int s) {
		return set[s][la.kind];
	}
	
	void ExpectWeak (int n, int follow) {
		if (la.kind == n) Get();
		else {
			SynErr(n);
			while (!StartOf(follow)) Get();
		}
	}
	
	boolean WeakSeparator (int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) { Get(); return true; }
		else if (StartOf(repFol)) return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}
	
	void program() {
		if (la.kind == 11) {
			packageDeclaration();
		}
		while (la.kind == 12) {
			packageImport();
		}
		classDeclaration();
	}

	void packageDeclaration() {
		Expect(11);
		identifier();
	}

	void packageImport() {
		Expect(12);
		identifier();
		if (la.kind == 13) {
			Get();
		}
	}

	void classDeclaration() {
		Expect(14);
		Expect(15);
		Expect(8);
		Expect(16);
		classBlock();
		Expect(17);
	}

	void identifier() {
		Expect(8);
		while (la.kind == 31) {
			Get();
			Expect(8);
		}
	}

	void classBlock() {
		while (la.kind == 18) {
			datatypeDeclaration();
		}
		while (la.kind == 14) {
			methodDeclaration();
		}
	}

	void datatypeDeclaration() {
		Expect(18);
		if (la.kind == 23) {
			Get();
		}
		datatypeDescriptor();
		if (la.kind == 1) {
			Get();
			if (StartOf(1)) {
				expression();
			} else if (la.kind == 24) {
				Get();
				if (la.kind == 8 || la.kind == 25 || la.kind == 43) {
					if (la.kind == 8 || la.kind == 43) {
						object();
						Expect(20);
						if (StartOf(1)) {
							expression();
						}
					} else {
						Get();
						Expect(7);
						Expect(26);
						Expect(22);
					}
				} else if (la.kind == 40 || la.kind == 41 || la.kind == 42) {
					primitive();
					Expect(25);
					Expect(7);
					Expect(26);
				} else SynErr(45);
			} else SynErr(46);
		}
	}

	void methodDeclaration() {
		Expect(14);
		Expect(18);
		if (la.kind == 19) {
			Get();
		} else if (StartOf(2)) {
			datatype();
		} else SynErr(47);
		Expect(8);
		Expect(20);
		if (StartOf(2)) {
			datatypeDescriptor();
			while (la.kind == 21) {
				Get();
				datatypeDescriptor();
			}
		}
		Expect(22);
		Expect(16);
		bodyBlock();
		Expect(17);
	}

	void datatype() {
		if (la.kind == 40 || la.kind == 41 || la.kind == 42) {
			primitive();
		} else if (la.kind == 8 || la.kind == 43) {
			object();
		} else SynErr(48);
	}

	void datatypeDescriptor() {
		datatype();
		identifier();
		if (la.kind == 25) {
			arraySelector();
		}
	}

	void bodyBlock() {
		while (StartOf(3)) {
			if (la.kind == 27) {
				whileStatement();
			} else if (la.kind == 28) {
				ifStatement();
			} else if (la.kind == 30) {
				returnStatement();
			} else if (la.kind == 8) {
				assOrMethodCall();
			} else {
				datatypeDeclaration();
			}
		}
	}

	void expression() {
		term();
		while (la.kind == 5 || la.kind == 6) {
			if (la.kind == 5) {
				Get();
			} else {
				Get();
			}
			term();
		}
	}

	void object() {
		if (la.kind == 43) {
			Get();
		} else if (la.kind == 8) {
			identifier();
		} else SynErr(49);
	}

	void primitive() {
		if (la.kind == 40) {
			Get();
		} else if (la.kind == 41) {
			Get();
		} else if (la.kind == 42) {
			Get();
		} else SynErr(50);
	}

	void assOrMethodCall() {
		identifier();
		if (la.kind == 20) {
			methodCall();
		} else if (la.kind == 1 || la.kind == 25) {
			assignment();
		} else SynErr(51);
	}

	void methodCall() {
		Expect(20);
		if (StartOf(1)) {
			expression();
			while (la.kind == 21) {
				Get();
				expression();
			}
		}
		Expect(22);
	}

	void assignment() {
		if (la.kind == 25) {
			arraySelector();
		}
		Expect(1);
		if (StartOf(1)) {
			expression();
		} else if (la.kind == 8) {
			identifier();
			Expect(20);
			if (StartOf(1)) {
				expression();
				while (la.kind == 21) {
					Get();
					expression();
				}
			}
			Expect(22);
		} else SynErr(52);
	}

	void arraySelector() {
		Expect(25);
		if (StartOf(1)) {
			expression();
		}
		Expect(26);
	}

	void whileStatement() {
		Expect(27);
		Expect(20);
		condition();
		Expect(22);
		Expect(16);
		bodyBlock();
		Expect(17);
	}

	void ifStatement() {
		Expect(28);
		Expect(20);
		condition();
		Expect(22);
		Expect(16);
		bodyBlock();
		Expect(17);
		if (la.kind == 29) {
			Get();
			Expect(16);
			bodyBlock();
			Expect(17);
		}
	}

	void returnStatement() {
		Expect(30);
		expression();
	}

	void condition() {
		expression();
		if (StartOf(4)) {
			if (la.kind == 1) {
				Get();
				Expect(1);
			} else if (la.kind == 4) {
				Get();
				Expect(1);
			} else if (la.kind == 2) {
				Get();
			} else if (la.kind == 2) {
				Get();
				Expect(1);
			} else if (la.kind == 3) {
				Get();
			} else {
				Get();
				Expect(1);
			}
			expression();
		}
	}

	void value() {
		switch (la.kind) {
		case 8: {
			identifier();
			if (la.kind == 25) {
				arraySelector();
			}
			break;
		}
		case 7: case 37: {
			intValue();
			break;
		}
		case 10: {
			Get();
			break;
		}
		case 38: case 39: {
			booleanValue();
			break;
		}
		case 9: {
			Get();
			break;
		}
		case 32: {
			Get();
			break;
		}
		case 4: {
			Get();
			value();
			break;
		}
		default: SynErr(53); break;
		}
	}

	void intValue() {
		if (la.kind == 37) {
			Get();
		}
		Expect(7);
	}

	void booleanValue() {
		if (la.kind == 38) {
			Get();
		} else if (la.kind == 39) {
			Get();
		} else SynErr(54);
	}

	void factor() {
		value();
		while (la.kind == 33 || la.kind == 34 || la.kind == 35) {
			if (la.kind == 33) {
				Get();
			} else if (la.kind == 34) {
				Get();
			} else {
				Get();
			}
			value();
		}
	}

	void term() {
		factor();
		while (la.kind == 36 || la.kind == 37) {
			if (la.kind == 36) {
				Get();
			} else {
				Get();
			}
			factor();
		}
	}



	public void Parse() {
		la = new Token();
		la.val = "";		
		Get();
		program();

		Expect(0);
	}

	private boolean[][] set = {
		{T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,x,x,x, T,x,x,T, T,T,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,x,x,x, x,T,T,T, x,x,x,x, x,x},
		{x,x,x,x, x,x,x,x, T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, T,T,T,T, x,x},
		{x,x,x,x, x,x,x,x, T,x,x,x, x,x,x,x, x,x,T,x, x,x,x,x, x,x,x,T, T,x,T,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x},
		{x,T,T,T, T,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x,x,x, x,x}

	};
} // end Parser


class Errors {
	public int count = 0;                                    // number of errors detected
	public java.io.PrintStream errorStream = System.out;     // error messages go to this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line, 1=column, 2=text
	
	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, line); }
		pos = b.indexOf("{1}");
		if (pos >= 0) { b.delete(pos, pos+3); b.insert(pos, column); }
		pos = b.indexOf("{2}");
		if (pos >= 0) b.replace(pos, pos+3, msg);
		errorStream.println(b.toString());
	}
	
	public void SynErr (int line, int col, int n) {
		String s;
		switch (n) {
			case 0: s = "EOF expected"; break;
			case 1: s = "equal expected"; break;
			case 2: s = "greater expected"; break;
			case 3: s = "smaller expected"; break;
			case 4: s = "not expected"; break;
			case 5: s = "AND expected"; break;
			case 6: s = "OR expected"; break;
			case 7: s = "number expected"; break;
			case 8: s = "simpleIdentifier expected"; break;
			case 9: s = "StringValue expected"; break;
			case 10: s = "charValue expected"; break;
			case 11: s = "\"package\" expected"; break;
			case 12: s = "\"import\" expected"; break;
			case 13: s = "\".*\" expected"; break;
			case 14: s = "\"public\" expected"; break;
			case 15: s = "\"class\" expected"; break;
			case 16: s = "\"{\" expected"; break;
			case 17: s = "\"}\" expected"; break;
			case 18: s = "\"static\" expected"; break;
			case 19: s = "\"void\" expected"; break;
			case 20: s = "\"(\" expected"; break;
			case 21: s = "\",\" expected"; break;
			case 22: s = "\")\" expected"; break;
			case 23: s = "\"final\" expected"; break;
			case 24: s = "\"new\" expected"; break;
			case 25: s = "\"[\" expected"; break;
			case 26: s = "\"]\" expected"; break;
			case 27: s = "\"while\" expected"; break;
			case 28: s = "\"if\" expected"; break;
			case 29: s = "\"else\" expected"; break;
			case 30: s = "\"return\" expected"; break;
			case 31: s = "\".\" expected"; break;
			case 32: s = "\"NULL\" expected"; break;
			case 33: s = "\"*\" expected"; break;
			case 34: s = "\"/\" expected"; break;
			case 35: s = "\"%\" expected"; break;
			case 36: s = "\"+\" expected"; break;
			case 37: s = "\"-\" expected"; break;
			case 38: s = "\"true\" expected"; break;
			case 39: s = "\"false\" expected"; break;
			case 40: s = "\"int\" expected"; break;
			case 41: s = "\"boolean\" expected"; break;
			case 42: s = "\"char\" expected"; break;
			case 43: s = "\"String\" expected"; break;
			case 44: s = "??? expected"; break;
			case 45: s = "invalid datatypeDeclaration"; break;
			case 46: s = "invalid datatypeDeclaration"; break;
			case 47: s = "invalid methodDeclaration"; break;
			case 48: s = "invalid datatype"; break;
			case 49: s = "invalid object"; break;
			case 50: s = "invalid primitive"; break;
			case 51: s = "invalid assOrMethodCall"; break;
			case 52: s = "invalid assignment"; break;
			case 53: s = "invalid value"; break;
			case 54: s = "invalid booleanValue"; break;
			default: s = "error " + n; break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr (int line, int col, String s) {	
		printMsg(line, col, s);
		count++;
	}
	
	public void SemErr (String s) {
		errorStream.println(s);
		count++;
	}
	
	public void Warning (int line, int col, String s) {	
		printMsg(line, col, s);
	}
	
	public void Warning (String s) {
		errorStream.println(s);
	}
} // Errors


class FatalError extends RuntimeException {
	public FatalError(String s) { super(s); }
}

