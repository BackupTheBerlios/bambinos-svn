
import java.io.InputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

class Token {
	int kind;    // token kind
	int pos;     // token position in the source text (starting at 0)
	int col;     // token column (starting at 0)
	int line;    // token line (starting at 1)
	String val;  // token value
	Token next;  // ML 2005-03-11 Peek tokens are kept in linked list
}

//-----------------------------------------------------------------------------------
// Buffer
//-----------------------------------------------------------------------------------
class Buffer {
	public static final int EOF = Character.MAX_VALUE + 1;
	private static final int MAX_BUFFER_LENGTH = 64 * 1024; // 64KB
	private byte[] buf;   // input buffer
	private int bufStart; // position of first byte in buffer relative to input stream
	private int bufLen;   // length of buffer
	private int fileLen;  // length of input stream
	private int pos;      // current position in buffer
	private RandomAccessFile file; // input stream (seekable)

	Buffer(InputStream s) {
		try {
			fileLen = bufLen = s.available();
			buf = new byte[bufLen];
			s.read(buf, 0, bufLen);
			pos = 0;
			bufStart = 0;
		} catch (IOException e){
			throw new FatalError("Error on filling the buffer ");
		}
	}

	Buffer(String fileName) {
		try {
			file = new RandomAccessFile(fileName, "r");
			fileLen = bufLen = (int) file.length();
			if (bufLen > MAX_BUFFER_LENGTH) bufLen = MAX_BUFFER_LENGTH;
			buf = new byte[bufLen];
			bufStart = Integer.MAX_VALUE; // nothing in buffer so far
			setPos(0); // setup buffer to position 0 (start)
			if (bufLen == fileLen) Close();
		} catch (IOException e) {
			throw new FatalError("Could not open file " + fileName);
		}
	}
	
	protected Buffer(Buffer b) { // called in UTF8Buffer constructor
		buf = b.buf;
		bufStart = b.bufStart;
		bufLen = b.bufLen;
		fileLen = b.fileLen;
		pos = b.pos;
		file = b.file;
		b.file = null;
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
		Close();
	}

	protected void Close() {
		if (file != null) {
			try {
				file.close();
				file = null;
			} catch (IOException e) {
				throw new FatalError(e.getMessage());
			}
		}
	}

	int Read() {
		if (pos < bufLen) {
			return buf[pos++] & 0xff;  // mask out sign bits
		} else if (getPos() < fileLen) {
			setPos(getPos());         // shift buffer start to pos
			return buf[pos++] & 0xff; // mask out sign bits
		} else {
			return EOF;
		}
	}

	int Peek() {
		int curPos = getPos();
		int ch = Read();
		setPos(curPos);
		return ch;
	}

	String GetString(int beg, int end) {
	    int len = end - beg;
	    char[] buf = new char[len];
	    int oldPos = getPos();
	    setPos(beg);
	    for (int i = 0; i < len; ++i) buf[i] = (char) Read();
	    setPos(oldPos);
	    return new String(buf);
	}

	int getPos() {
		return pos + bufStart;
	}

	void setPos(int value) {
		if (value < 0) value = 0;
		else if (value > fileLen) value = fileLen;
		if (value >= bufStart && value < bufStart + bufLen) { // already in buffer
			pos = value - bufStart;
		} else if (file != null) { // must be swapped in
			try {
				file.seek(value);
				bufLen = file.read(buf);
				bufStart = value; pos = 0;
			} catch(IOException e) {
				throw new FatalError(e.getMessage());
			}
		} else {
			pos = fileLen - bufStart; // make getPos() return fileLen
		}
	}
}

//-----------------------------------------------------------------------------------
// UTF8Buffer
//-----------------------------------------------------------------------------------
class UTF8Buffer extends Buffer {
	UTF8Buffer(Buffer b) { super(b); }

	int Read() {
		int ch;
		do {
			ch = super.Read();
			// until we find a uft8 start (0xxxxxxx or 11xxxxxx)
		} while ((ch >= 128) && ((ch & 0xC0) != 0xC0) && (ch != EOF));
		if (ch < 128 || ch == EOF) {
			// nothing to do, first 127 chars are the same in ascii and utf8
			// 0xxxxxxx or end of file character
		} else if ((ch & 0xF0) == 0xF0) {
			// 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
			int c1 = ch & 0x07; ch = super.Read();
			int c2 = ch & 0x3F; ch = super.Read();
			int c3 = ch & 0x3F; ch = super.Read();
			int c4 = ch & 0x3F;
			ch = (((((c1 << 6) | c2) << 6) | c3) << 6) | c4;
		} else if ((ch & 0xE0) == 0xE0) {
			// 1110xxxx 10xxxxxx 10xxxxxx
			int c1 = ch & 0x0F; ch = super.Read();
			int c2 = ch & 0x3F; ch = super.Read();
			int c3 = ch & 0x3F;
			ch = (((c1 << 6) | c2) << 6) | c3;
		} else if ((ch & 0xC0) == 0xC0) {
			// 110xxxxx 10xxxxxx
			int c1 = ch & 0x1F; ch = super.Read();
			int c2 = ch & 0x3F;
			ch = (c1 << 6) | c2;
		}
		return ch;
	}
}

//-----------------------------------------------------------------------------------
// StartStates  -- maps charactes to start states of tokens
//-----------------------------------------------------------------------------------
class StartStates {
	class Elem {
		int key, val;
		Elem next;
		Elem(int key, int val) { this.key = key; this.val = val; }
	}

	Elem[] tab = new Elem[128];

	void set(int key, int val) {
		Elem e = new Elem(key, val);
		int k = key % 128;
		e.next = tab[k]; tab[k] = e;
	}

	int state(int key) {
		Elem e = tab[key % 128];
		while (e != null && e.key != key) e = e.next;
		return e == null ? 0: e.val;
	}
}

//-----------------------------------------------------------------------------------
// Scanner
//-----------------------------------------------------------------------------------
public class Scanner {
	static final char EOL = '\n';
	static final int  eofSym = 0;
	static final int maxT = 44;
	static final int noSym = 44;


	public Buffer buffer; // scanner buffer

	Token t;           // current token
	int ch;            // current input character
	int pos;           // byte position of current character
	int col;           // column number of current character
	int line;          // line number of current character
	int oldEols;       // EOLs that appeared in a comment;
	StartStates start; // maps initial token character to start state

	Token tokens;      // list of tokens already peeked (first token is a dummy)
	Token pt;          // current peek token
	
	char[] tokenText = new char[16]; // token text used in NextToken(), dynamically enlarged
	
	public Scanner (String fileName) {
		buffer = new Buffer(fileName);
		Init();
	}
	
	public Scanner(InputStream s) {
		buffer = new Buffer(s);
		Init();
	}
	
	void Init () {
		pos = -1; line = 1; col = 0;
		oldEols = 0;
		NextCh();
		if (ch == 0xEF) { // check optional byte order mark for UTF-8
			NextCh(); int ch1 = ch;
			NextCh(); int ch2 = ch;
			if (ch1 != 0xBB || ch2 != 0xBF) {
				throw new FatalError("Illegal byte order mark at start of file");
			}
			buffer = new UTF8Buffer(buffer); col = 0;
			NextCh();
		}
		start = new StartStates();
		for (int i = 49; i <= 57; ++i) start.set(i, 9);
		for (int i = 65; i <= 90; ++i) start.set(i, 11);
		for (int i = 97; i <= 122; ++i) start.set(i, 11);
		start.set(61, 1); 
		start.set(62, 2); 
		start.set(60, 3); 
		start.set(33, 4); 
		start.set(38, 5); 
		start.set(124, 7); 
		start.set(48, 10); 
		start.set(34, 12); 
		start.set(39, 15); 
		start.set(46, 33); 
		start.set(123, 21); 
		start.set(125, 22); 
		start.set(40, 23); 
		start.set(44, 24); 
		start.set(41, 25); 
		start.set(91, 26); 
		start.set(93, 27); 
		start.set(42, 28); 
		start.set(47, 29); 
		start.set(37, 30); 
		start.set(43, 31); 
		start.set(45, 32); 
		start.set(Buffer.EOF, -1);

		pt = tokens = new Token();  // first token is a dummy
	}
	
	void NextCh() {
		if (oldEols > 0) { ch = EOL; oldEols--; }
		else {
			pos = buffer.getPos();
			ch = buffer.Read(); col++;
			// replace isolated '\r' by '\n' in order to make
			// eol handling uniform across Windows, Unix and Mac
			if (ch == '\r' && buffer.Peek() != '\n') ch = EOL;
			if (ch == EOL) { line++; col = 0; }
		}

	}
	

	boolean Comment0() {
		int level = 1, pos0 = pos, line0 = line, col0 = col;
		NextCh();
		if (ch == '/') {
			NextCh();
			for(;;) {
				if (ch == 13) {
					NextCh();
					if (ch == 10) {
						level--;
						if (level == 0) { oldEols = line - line0; NextCh(); return true; }
						NextCh();
					}
				} else if (ch == Buffer.EOF) return false;
				else NextCh();
			}
		} else {
			buffer.setPos(pos0); NextCh(); line = line0; col = col0;
		}
		return false;
	}

	boolean Comment1() {
		int level = 1, pos0 = pos, line0 = line, col0 = col;
		NextCh();
		if (ch == '*') {
			NextCh();
			for(;;) {
				if (ch == '*') {
					NextCh();
					if (ch == '/') {
						level--;
						if (level == 0) { oldEols = line - line0; NextCh(); return true; }
						NextCh();
					}
				} else if (ch == '/') {
					NextCh();
					if (ch == '*') {
						level++; NextCh();
					}
				} else if (ch == Buffer.EOF) return false;
				else NextCh();
			}
		} else {
			buffer.setPos(pos0); NextCh(); line = line0; col = col0;
		}
		return false;
	}

	
	void CheckLiteral() {
		String lit = t.val;
		if (lit.compareTo("package") == 0) t.kind = 11;
		else if (lit.compareTo("import") == 0) t.kind = 12;
		else if (lit.compareTo("public") == 0) t.kind = 14;
		else if (lit.compareTo("class") == 0) t.kind = 15;
		else if (lit.compareTo("static") == 0) t.kind = 18;
		else if (lit.compareTo("void") == 0) t.kind = 19;
		else if (lit.compareTo("final") == 0) t.kind = 23;
		else if (lit.compareTo("new") == 0) t.kind = 24;
		else if (lit.compareTo("while") == 0) t.kind = 27;
		else if (lit.compareTo("if") == 0) t.kind = 28;
		else if (lit.compareTo("else") == 0) t.kind = 29;
		else if (lit.compareTo("return") == 0) t.kind = 30;
		else if (lit.compareTo("NULL") == 0) t.kind = 32;
		else if (lit.compareTo("true") == 0) t.kind = 38;
		else if (lit.compareTo("false") == 0) t.kind = 39;
		else if (lit.compareTo("int") == 0) t.kind = 40;
		else if (lit.compareTo("boolean") == 0) t.kind = 41;
		else if (lit.compareTo("char") == 0) t.kind = 42;
		else if (lit.compareTo("String") == 0) t.kind = 43;
	}

	Token NextToken() {
		while(ch == ' ' ||
			ch >= 9 && ch <= 10 || ch == 13
		) NextCh();
		if (ch == '/' && Comment0() ||ch == '/' && Comment1()) return NextToken();
		t = new Token();
		t.pos = pos; t.col = col; t.line = line; 
		int state = start.state(ch);
		char[] tval = tokenText; // local variables are more efficient
		int tlen = 0;
		tval[tlen++] = (char)ch; NextCh();
		
		boolean done = false;
		while (!done) {
			if (tlen >= tval.length) {
				char[] newBuf = new char[2 * tval.length];
				System.arraycopy(tval, 0, newBuf, 0, tval.length);
				tokenText = tval = newBuf;
			}
			switch (state) {
				case -1: { t.kind = eofSym; done = true; break; } // NextCh already done 
				case 0: { t.kind = noSym; done = true; break; }   // NextCh already done
				case 1:
					{t.kind = 1; done = true; break;}
				case 2:
					{t.kind = 2; done = true; break;}
				case 3:
					{t.kind = 3; done = true; break;}
				case 4:
					{t.kind = 4; done = true; break;}
				case 5:
					if (ch == '&') {tval[tlen++] = (char)ch; NextCh(); state = 6; break;}
					else {t.kind = noSym; done = true; break;}
				case 6:
					{t.kind = 5; done = true; break;}
				case 7:
					if (ch == '|') {tval[tlen++] = (char)ch; NextCh(); state = 8; break;}
					else {t.kind = noSym; done = true; break;}
				case 8:
					{t.kind = 6; done = true; break;}
				case 9:
					if (ch >= '0' && ch <= '9') {tval[tlen++] = (char)ch; NextCh(); state = 9; break;}
					else {t.kind = 7; done = true; break;}
				case 10:
					{t.kind = 7; done = true; break;}
				case 11:
					if (ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'Z' || ch >= 'a' && ch <= 'z') {tval[tlen++] = (char)ch; NextCh(); state = 11; break;}
					else {t.kind = 8; t.val = new String(tval, 0, tlen); CheckLiteral(); return t;}
				case 12:
					if (ch <= 9 || ch >= 11 && ch <= 12 || ch >= 14 && ch <= '!' || ch >= '#' && ch <= '&' || ch >= '(' && ch <= '[' || ch >= ']' && ch <= 65535) {tval[tlen++] = (char)ch; NextCh(); state = 13; break;}
					else {t.kind = noSym; done = true; break;}
				case 13:
					if (ch <= 9 || ch >= 11 && ch <= 12 || ch >= 14 && ch <= '!' || ch >= '#' && ch <= '&' || ch >= '(' && ch <= '[' || ch >= ']' && ch <= 65535) {tval[tlen++] = (char)ch; NextCh(); state = 13; break;}
					else if (ch == '"') {tval[tlen++] = (char)ch; NextCh(); state = 14; break;}
					else {t.kind = noSym; done = true; break;}
				case 14:
					{t.kind = 9; done = true; break;}
				case 15:
					if (ch <= 9 || ch >= 11 && ch <= 12 || ch >= 14 && ch <= '!' || ch >= '#' && ch <= '&' || ch >= '(' && ch <= '[' || ch >= ']' && ch <= 65535) {tval[tlen++] = (char)ch; NextCh(); state = 16; break;}
					else if (ch == 92) {tval[tlen++] = (char)ch; NextCh(); state = 17; break;}
					else {t.kind = noSym; done = true; break;}
				case 16:
					if (ch == 39) {tval[tlen++] = (char)ch; NextCh(); state = 19; break;}
					else {t.kind = noSym; done = true; break;}
				case 17:
					if (ch >= ' ' && ch <= '~') {tval[tlen++] = (char)ch; NextCh(); state = 18; break;}
					else {t.kind = noSym; done = true; break;}
				case 18:
					if (ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'f') {tval[tlen++] = (char)ch; NextCh(); state = 18; break;}
					else if (ch == 39) {tval[tlen++] = (char)ch; NextCh(); state = 19; break;}
					else {t.kind = noSym; done = true; break;}
				case 19:
					{t.kind = 10; done = true; break;}
				case 20:
					{t.kind = 13; done = true; break;}
				case 21:
					{t.kind = 16; done = true; break;}
				case 22:
					{t.kind = 17; done = true; break;}
				case 23:
					{t.kind = 20; done = true; break;}
				case 24:
					{t.kind = 21; done = true; break;}
				case 25:
					{t.kind = 22; done = true; break;}
				case 26:
					{t.kind = 25; done = true; break;}
				case 27:
					{t.kind = 26; done = true; break;}
				case 28:
					{t.kind = 33; done = true; break;}
				case 29:
					{t.kind = 34; done = true; break;}
				case 30:
					{t.kind = 35; done = true; break;}
				case 31:
					{t.kind = 36; done = true; break;}
				case 32:
					{t.kind = 37; done = true; break;}
				case 33:
					if (ch == '*') {tval[tlen++] = (char)ch; NextCh(); state = 20; break;}
					else {t.kind = 31; done = true; break;}

			}
		}
		t.val = new String(tval, 0, tlen);
		return t;
	}
	
	// get the next token (possibly a token already seen during peeking)
	public Token Scan () {
		if (tokens.next == null) {
			return NextToken();
		} else {
			pt = tokens = tokens.next;
			return tokens;
		}
	}

	// get the next token, ignore pragmas
	public Token Peek () {
		if (pt.next == null) {
			do {
				pt = pt.next = NextToken();
			} while (pt.kind > maxT); // skip pragmas
		} else {
			do {
				pt = pt.next;
			} while (pt.kind > maxT);
		}
		return pt;
	}

	// make sure that peeking starts at current scan position
	public void ResetPeek () { pt = tokens; }

} // end Scanner

