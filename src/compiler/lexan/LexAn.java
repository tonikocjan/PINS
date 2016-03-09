package compiler.lexan;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import compiler.*;

/**
 * Leksikalni analizator.
 * 
 * @author sliva
 * @implementation Toni Kocjan
 */
public class LexAn {

	/** Ali se izpisujejo vmesni rezultati. */
	private boolean dump;

	/**
	 * File which is being parsed.
	 */
	private FileInputStream file = null;

	/**
	 * Buffer containg current word / symbol which is being processed.
	 */
	private StringBuilder word = null;
	
	/**
	 * Map containing all reserved keywords.
	 */
	private static final String[] keywords = new String[]{"logical", "integer", "string", "arr", "else", "for", "fun", "if", "then", "typ", "var", "where", "while"};
	private static Map<String, Integer> keywordsMap = null;

	/**
	 * Previous, current caracter.
	 */
	private int nxtCh = -1;

	/**
	 * Position.
	 */
	private int startCol = 1, startRow = 1;
	private int endCol = 0, endRow = 1;

	/**
	 * State-machine flags.
	 * 
	 * PINS contains three constants: 
	 * - logical: "true" or "false" 
	 * - numeric: decimal, hexadecimal and octal
	 * - string: sequence of characters inside single-quotes
	 */
	private boolean numeric = false;

	private boolean string = false;
	private boolean stringClosed = false;
	
	private boolean logical = false;
	private boolean identifier = false;
	
	private boolean comment = false;

	private boolean dontRead = false;

	/**
	 * -----------------------------
	 */

	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param sourceFileName
	 *            Ime izvorne datoteke.
	 * @param dump
	 *            Ali se izpisujejo vmesni rezultati.
	 */
	public LexAn(String sourceFileName, boolean dump) {
		this.dump = dump;

		try {
			this.file = new FileInputStream(sourceFileName);
			word = new StringBuilder();
			
			/**
			 * Construct keyword map. 
			 */
			keywordsMap = new HashMap<>();
			for (int i = 0; i < keywords.length; i++)
				keywordsMap.put(keywords[i], i + 30);
			
		} catch (FileNotFoundException e) {
			Report.report("File [ " + sourceFileName + " ] does not exist! Exiting.");
		}
	}

	/**
	 * Vrne naslednji simbol iz izvorne datoteke. Preden vrne simbol, ga izpise
	 * v datoteko z vmesnimi rezultati.
	 * 
	 * @return Naslednji simbol iz izvorne datoteke.
	 */
	public Symbol lexAn() {
		if (file == null) return null;
		
		try {
			// reset state-machine
			reset();

			Symbol s = parseSymbol();

			startCol = endCol;
			startRow = endRow;

			if (s == null) s = new Symbol(Token.EOF, "$", startRow, startCol, endRow, endCol);
			dump(s);
			
			return s;

		} catch (IOException e) {
			Report.report("Error while parsing input file! Exiting.");
		}

		return null;
	}

	private Symbol parseSymbol() throws IOException {
		while (true) {
			if (dontRead) {
				dontRead = false;
				
				// if EOF return EOF token
				if (nxtCh == -1)
					return new Symbol(Token.EOF, "$", new Position(endRow, endCol));
				
				/**
				 *  If previous character was an operator,
				 *  also check next character ('==', '!=' ...)
				 */
				Symbol op = isOperator(nxtCh);
				if (op != null) {
					// read next character
					int tmp = file.read();
					Symbol op2 = isOperator2(nxtCh, tmp);
					
					nxtCh = tmp;
					if (op2 != null) return op2;
					
					dontRead = true;
					return op;
				}
			}
			else {
				nxtCh = file.read();
				endCol++;
			}
			
			/**
			 * If EOF, return symbol and exit.
			 */
			if (nxtCh == -1) {
				dontRead = true;
				
				/**
				 * If in string state and string not closed,
				 * report lexal error.
				 */
				if (string && !stringClosed) {
					Report.report(new Position(startRow, startCol, endRow, endCol),
							"String literal not properly closed!");
					return null;
				}
				
				return returnSymbol();
			}
			
			/**
			 * If carriage return, skip it.
			 */
			if (nxtCh == 13) continue;
			
			/**
			 * If newline, exit comment state and return symbol.
			 */
			if (nxtCh == 10) {				
				/**
				 * If in string state and string not closed,
				 * report lexal error.
				 */
				if (string && !stringClosed) {
					Report.report(new Position(startRow, startCol, endRow, endCol),
							"String literal not properly closed!");
					return null;
				}
				
				comment = false;
				Symbol s = returnSymbol();
				
				// update counters
				startRow++;
				endRow++;
				startCol = 1;
				endCol = 0;				
				
				if (s != null) return s;
				continue;
			}
			
			/**
			 * Handle whitespaces. 
			 */
			if (nxtCh == 32 || nxtCh == 9) {
				int incr = (nxtCh == 32) ? 1 : 4;
				
				// if word is emtpy, increase startCol counter
				if (word.length() == 0) startCol += incr;
				// otherwise increase endRow counter
				else endCol += incr;
					
				if (!string) {
					Symbol s = returnSymbol();
					if (s != null) return s;
				}
				if (!string) continue;
			}
			
			/**
			 * If character is '#' and not in string state, enter comment state.
			 */
			if (nxtCh == 35 && !string) {
				comment = true;
				Symbol s = returnSymbol();
				if (s == null) continue;
				return s;
			}
			
			/**
			 * Skip characters while in comment state.
			 */
			if (comment) continue;
			
			/** 
			 * If not in string state, 
			 * first check if character is an operator (',', ';' .... )
			 */
			Symbol operator = isOperator(nxtCh);
			if (operator != null) {
				dontRead = true;
				
				Symbol s = returnSymbol();
				if (s != null) return s;
				
				continue;
			}
			
			/**
			 * If this is the first character, we can determine whether
			 * it will be a number, string or identifier.
			 * If current character is a single quote, it is a string. 
			 * If current character is numeric, it is a number.
			 * Otherwise an identifier.
			 */
			if (word.length() == 0) {
				if (nxtCh == '\'') string = true;
				else if (isNumeric(nxtCh)) numeric = true;
				else identifier = true;
				word.append((char)nxtCh);
				continue;
			}
			
			if (numeric) {
				/**
				 * If in numeric state, and character isn't a number,
				 * return symbol and exit numeric state.
				 */
				if (!isNumeric(nxtCh)) {
					Symbol s = returnSymbol();
					
					numeric = false;
					dontRead = true;
					
					return s;
				}
				word.append((char)nxtCh);
				continue;
			}
			
			if (string) {
				/**
				 * Mechanism for handling quotes.
				 * 
				 * When first single-quote is found, enter stringClosed state.
				 * When in stringClosed state, check if current character is again single-quote.
				 * If it is not, string ended.
				 * Otherwise single-quote is part of string.
				 */
				if (stringClosed) {
					if (nxtCh == '\'') stringClosed = false;
					else {
						dontRead = true;
						return returnSymbol();
					}
				}
				else { if (nxtCh == '\'') stringClosed = true; }
				
				/**
				 * If in string state, and character isn't valid,
				 * return symbol and exit string state.
				 */
				if (!(nxtCh >= 32 && nxtCh <= 126)) {
					Symbol s = returnSymbol();
					
					dontRead = true;
					
					return s;
				}
				word.append((char)nxtCh);
				continue;
			}
			
			if (identifier) {
				if (isNumeric(nxtCh) || 
					(nxtCh >= 'a' && nxtCh <= 'z') ||
					(nxtCh >= 'A' && nxtCh <= 'Z'))
					word.append((char)nxtCh);
				else {
					Report.report(new Position(startRow, startCol, endRow, endCol),
							"Invalid token in identifier!");
					return null;
				}
			}
		}
	}
	
	/**
	 * Check if character is a single-character operator.
	 */
	private Symbol isOperator(int ch) {
		if (ch == '+') return new Symbol(Token.ADD, "+", startRow, startCol, endRow, endCol);
		if (ch == '-') return new Symbol(Token.SUB, "-", startRow, startCol, endRow, endCol);
		if (ch == '*') return new Symbol(Token.MUL, "*", startRow, startCol, endRow, endCol);
		if (ch == '/') return new Symbol(Token.DIV, "/", startRow, startCol, endRow, endCol);
		if (ch == '%') return new Symbol(Token.MOD, "%", startRow, startCol, endRow, endCol);

		if (ch == '&') return new Symbol(Token.AND, "&", startRow, startCol, endRow, endCol);
		if (ch == '|') return new Symbol(Token.IOR, "|", startRow, startCol, endRow, endCol);
		if (ch == '!') return new Symbol(Token.NOT, "!", startRow, startCol, endRow, endCol);

		if (ch == '(') return new Symbol(Token.LPARENT, "(", startRow, startCol, endRow, endCol);
		if (ch == ')') return new Symbol(Token.RPARENT, ")", startRow, startCol, endRow, endCol);
		if (ch == '{') return new Symbol(Token.LBRACE, "{", startRow, startCol, endRow, endCol);
		if (ch == '}') return new Symbol(Token.RBRACE, "}", startRow, startCol, endRow, endCol);
		if (ch == '[') return new Symbol(Token.LBRACKET, "[", startRow, startCol, endRow, endCol);
		if (ch == ']') return new Symbol(Token.RBRACKET, "]", startRow, startCol, endRow, endCol);

		if (ch == '<') return new Symbol(Token.LTH, "<", startRow, startCol, endRow, endCol);
		if (ch == '>') return new Symbol(Token.GTH, ">", startRow, startCol, endRow, endCol);
		if (ch == '=') return new Symbol(Token.ASSIGN, "=", startRow, startCol, endRow, endCol);

		if (ch == '.') return new Symbol(Token.DOT, ",", startRow, startCol, endRow, endCol);
		if (ch == ':') return new Symbol(Token.COLON, ":", startRow, startCol, endRow, endCol);
		if (ch == ';') return new Symbol(Token.SEMIC, ";", startRow, startCol, endRow, endCol);
		if (ch == ',') return new Symbol(Token.COMMA, ",", startRow, startCol, endRow, endCol);

		return null;
	}
	
	/**
	 * Check for double-character operators.
	 */
	private Symbol isOperator2(int ch1, int ch2) {
		if (ch1 == '=' && ch2 == '=') 
			return new Symbol(Token.EQU, "EQU", startRow, startCol, endRow, endCol);
		if (ch1 == '!' && ch2 == '=')
			return new Symbol(Token.NEQ, "NEQ", startRow, startCol, endRow, endCol);
		if (ch1 == '>' && ch2 == '=')
			return new Symbol(Token.GEQ, "GEQ", startRow, startCol, endRow, endCol);
		if (ch1 == '<' && ch2 == '=')
			return new Symbol(Token.LEQ, "LEQ", startRow, startCol, endRow, endCol);
		return null;
	}
	
	private boolean isNumeric(int ch) {
		return (ch >= '0' && ch <= '9');
	}

	private void reset() {
		numeric = false;
		string = false;
		logical = false;
		stringClosed = false;
		word = new StringBuilder();
	}

	/**
	 * @return Symbol based on current state.
	 */
	private Symbol returnSymbol() {
		if (word.length() == 0) return null;
		
		if (numeric)
			return new Symbol(Token.INT_CONST, word.toString(), startRow, startCol, endRow, endCol);
		if (string)
			return new Symbol(Token.STR_CONST, word.toString(), startRow, startCol, endRow, endCol);
		if (logical)
			return new Symbol(Token.LOG_CONST, word.toString(), startRow, startCol, endRow, endCol);
		
		/**
		 * If in identifier state, check if word is
		 * - keyword or
		 * - logical literal ("true", "false")
		 */
		if (identifier) {
			if (keywordsMap.containsKey(word.toString()))
				return new Symbol(keywordsMap.get(word.toString()), 
						word.toString(), startRow, startCol, endRow, endCol);
			else if (word.toString().equals("true") || word.toString().equals("false"))
				return new Symbol(Token.LOG_CONST, 
						word.toString(), startRow, startCol, endRow, endCol);
			return new Symbol(Token.IDENTIFIER, 
					word.toString(), startRow, startCol, endRow, endCol);
		}
		return null;
	}

	/**
	 * Izpise simbol v datoteko z vmesnimi rezultati.
	 * 
	 * @param symb
	 *            Simbol, ki naj bo izpisan.
	 */
	private void dump(Symbol symb) {
		if (!dump)
			return;
		if (Report.dumpFile() == null)
			return;
		if (symb.token == Token.EOF)
			Report.dumpFile().println(symb.toString());
		else
			Report.dumpFile().println(
					"[" + symb.position.toString() + "] " + symb.toString());
	}

}
