package compiler.lexan;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
	private static final String[] keywords = new String[]{"logical", "integer", "string","arr", "else", "for", "fun", "if", "then", "typ", "var", "where", "while"};
	private static Map<String, Integer> keywordsMap = null;

	/**
	 * Previous, current caracter.
	 */
	private int previous = -1;
	private int nxtCh = -1;

	/**
	 * Position.
	 */
	private int startCol = 1, startRow = 1;
	private int endCol = 1, endRow = 1;

	/**
	 * State-machine flags.
	 * 
	 * PINS contains three constants: 
	 * - logical: "true" or "false" 
	 * - numeric: decimal, hexadecimal and octal
	 * - string: sequence of characters inside single-quotes
	 */
	private boolean numeric = false;
	private boolean hexadecimal = false;
	private boolean octal = false;

	private boolean string = false;
	private boolean logical = false;
	private boolean identifier = false;

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
	 * na datoteko z vmesnimi rezultati.
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

			return s;

		} catch (IOException e) {
			Report.report("Error while parsing input file! Exiting.");
		}

		return null;
	}

	private Symbol parseSymbol() throws IOException {
		for (; true; ) {
			if (dontRead) {
				dontRead = false;
				// if previous character was EOF, return EOF token
				if (previous == -1)
					return new Symbol(Token.EOF, "EOF", new Position(endRow, endCol));
				/**
				 *  If previous character was an operator,
				 *  we also need to check next character ('==', '!=' ...)
				 */
				Symbol op = isOperator(previous);
				if (op != null) {
					// read next character
					nxtCh = file.read();
					Symbol op2 = isOperator2(previous, nxtCh);
					previous = '\0';
					if (op2 != null) return op2;
					previous = nxtCh;
					dontRead = true;
					return op;
				}
			}
			else {
				previous = nxtCh;
				nxtCh = file.read();
				endCol++;
			}
			
			// if EOF
			if (nxtCh == -1) {
				dontRead = true;
				previous = -1;
				return returnSymbol();
			}
			
			/**
			 * Skip whitespaces.
			 */
			if (nxtCh == 32 || nxtCh == 9 || nxtCh == 10 || nxtCh == 13) {
				Symbol s = returnSymbol();
				
				/**
				 * If we are in string state and stumble upon newline, 
				 * check if last character of the word is single quote. 
				 * If it isn't, return lexal-error.
				 */
				if (nxtCh == 10 || nxtCh == 13) {
					if (string && word.charAt(word.length() - 1) != '\'') {
						
						Report.report(new Position(startRow, startCol, endRow, endCol), 
								"String literal is not properly closed by a single-quote.");
						break;
					}
					startRow++;
					endRow++;
					startCol = 1;
					endCol = 1;
				}
				
				if (s != null) return s;
				continue;
			}
			
			/**
			 * Entering state-machine.
			 * 
			 * If we are not in 'string' state, 
			 * we first check if character is an operator (',', ';' .... )
			 */
			if (!string) {
				Symbol operator = isOperator(nxtCh);
				// if this is operator, return currently processed word and save character
				if (operator != null) {
					dontRead = true;
					previous = nxtCh;
					if (word.length() > 0)
						return returnSymbol();
					continue;
				}
			}
			
			// append current character
			word.append((char)nxtCh);

			/**
			 * If this is first character, we can determine whether
			 * this will be a number, string or identifier.
			 * If current charcter is single quote, this is a string. 
			 * If current character is '0' it might be either a hexadecimal or an octal number.
			 * Otherwise it is an identifier.
			 */
			if (word.length() == 1) {
				if (isNumeric(nxtCh))
					numeric = true;
				else if (nxtCh == '\'')
					string = true;
				else
					identifier = true;
			}
			else {
				/**
				 * If state is numeric.
				 */
				if (numeric) {
					/**
					 * If this is second character and previous character was '0',
					 * this must either be 'x' or char in range '0' - '7'.
					 * Otherwise return lexal-error.
					 */
					if (previous == '0' && word.length() == 2) {
						if (nxtCh == 'x' || nxtCh == 'X') {
							hexadecimal = true;
							continue;
						}
						else if (isOctal(nxtCh))
							octal = true;
						else {
							Position pos = new Position(startRow, startCol, endRow, endCol);
							Report.report(pos, "Invalid token ['" + (char)nxtCh + "']. Expected 'x' or 0-7!");
							break;
						}
					}					
					/**
					 * If octal.
					 */
					if (octal && !isOctal(nxtCh)) {
						Position pos = new Position(startRow, startCol, endRow, endCol);
						Report.report(pos, "Invalid token ['" + (char)nxtCh + "']. Expected 0-7!");
						break;
					}
					/**
					 * If decimal.
					 */
					if (!hexadecimal && !isNumeric(nxtCh)) {
						Position pos = new Position(startRow, startCol, endRow, endCol);
						Report.report(pos, "Invalid token ['" + (char)nxtCh + "']. Expected 0-9!");
						break;
					}
					/**
					 * If hexadecimal.
					 */
					if (hexadecimal && !isHexadecimal(nxtCh)) {
						Position pos = new Position(startRow, startCol, endRow, endCol);
						Report.report(pos, "Invalid token ['" + (char)nxtCh + "']. Expected 0-9 or A-F!");
						break;
					}
				}
				/**
				 * If state is string, we read characters until we find single quote.
				 * When we find it, we also need to check next character, because
				 * double single quote is part of string.
				 */
				else if (string) {
					if (nxtCh == '\'') {
						previous = '\'';
						nxtCh = file.read();
						if (nxtCh == '\'')
							word.append((char)nxtCh);
						// string ended
						else {
							dontRead = true;
							return returnSymbol();
						}
					}
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Check if character is single-character operator.
	 */
	private Symbol isOperator(int ch) {
		if (ch == '+')
			return new Symbol(Token.ADD, "ADD", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == '-')
			return new Symbol(Token.SUB, "SUB", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == '*')
			return new Symbol(Token.MUL, "MUL", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == '/')
			return new Symbol(Token.DIV, "DIV", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == '%')
			return new Symbol(Token.MOD, "MOD", new Position(startRow,
					startCol, endRow, endCol));

		if (ch == '&')
			return new Symbol(Token.AND, "AND", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == '|')
			return new Symbol(Token.IOR, "IOR", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == '!')
			return new Symbol(Token.NOT, "NOT", new Position(startRow,
					startCol, endRow, endCol));

		if (ch == '(')
			return new Symbol(Token.LPARENT, "LPARENT", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == ')')
			return new Symbol(Token.RPARENT, "RPARENT", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == '{')
			return new Symbol(Token.LBRACE, "LBRACE", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == '}')
			return new Symbol(Token.RBRACE, "RBRACE", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == '[')
			return new Symbol(Token.LBRACKET, "LBRACKET", new Position(
					startRow, startCol, endRow, endCol));
		if (ch == ']')
			return new Symbol(Token.RBRACKET, "RBRACKET", new Position(
					startRow, startCol, endRow, endCol));

		if (ch == '<')
			return new Symbol(Token.LTH, "LTH", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == '>')
			return new Symbol(Token.GTH, "GTH", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == '=')
			return new Symbol(Token.ASSIGN, "ASSIGN", new Position(startRow,
					startCol, endRow, endCol));

		if (ch == '.')
			return new Symbol(Token.DOT, "DOT", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == ':')
			return new Symbol(Token.COLON, "COLON", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == ';')
			return new Symbol(Token.SEMIC, "SEMIC", new Position(startRow,
					startCol, endRow, endCol));
		if (ch == ',')
			return new Symbol(Token.COMMA, "COMMA", new Position(startRow,
					startCol, endRow, endCol));

		return null;
	}
	/**
	 * Check for double-character operators.
	 */
	private Symbol isOperator2(int ch1, int ch2) {
		if (ch1 == '=' && ch2 == '=')
			return new Symbol(Token.EQU, "EQU", new Position(startRow,
					startCol, endRow, endCol));
		if (ch1 == '!' && ch2 == '=')
			return new Symbol(Token.NEQ, "NEQ", new Position(startRow,
					startCol, endRow, endCol));
		if (ch1 == '>' && ch2 == '=')
			return new Symbol(Token.GEQ, "GEQ", new Position(startRow,
					startCol, endRow, endCol));
		if (ch1 == '<' && ch2 == '=')
			return new Symbol(Token.LEQ, "GEQ", new Position(startRow,
					startCol, endRow, endCol));
		return null;
	}

	private boolean isAlphabetical(int ch) {
		return (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z');
	}

	private boolean isNumeric(int ch) {
		return (ch >= '0' && ch <= '9');
	}

	private boolean isHexadecimal(int ch) {
		return isNumeric(ch)
				|| (ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F');
	}

	private boolean isOctal(int ch) {
		return (ch >= '0' && ch <= '7');
	}

	private void reset() {
		numeric = false;
		hexadecimal = false;
		octal = false;
		string = false;
		logical = false;
		word = new StringBuilder();
	}

	/**
	 * @return Symbol based on current state.
	 */
	private Symbol returnSymbol() {
		if (word.length() == 0) return null;
		
		Position pos = new Position(startRow, startCol, endRow, endCol);
		if (numeric)
			return new Symbol(Token.INT_CONST, word.toString(), pos);
		if (string)
			return new Symbol(Token.STR_CONST, word.toString(), pos);
		if (logical)
			return new Symbol(Token.LOG_CONST, word.toString(), pos);
		
		/**
		 * If returning IDENTIFIER, check if word is
		 * - keyword,
		 * - logical literal ("true", "false")
		 */
		if (identifier) {
			if (keywordsMap.containsKey(word.toString()))
				return new Symbol(keywordsMap.get(word.toString()), word.toString(), pos);
			return new Symbol(Token.IDENTIFIER, word.toString(), pos);
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
