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
	private int previous = -1;
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
	private boolean hexadecimal = false;
	private boolean octal = false;

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

			if (s == null) s = new Symbol(Token.EOF, "", null);
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
				// if previous character was EOF, return EOF token
				if (previous == -1)
					return new Symbol(Token.EOF, "EOF", new Position(endRow, endCol));
				
				/**
				 *  If previous character was an operator,
				 *  also check next character ('==', '!=' ...)
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
				
				/**
				 * If in string state and string not properly closed,
				 * return lexal-error.
				 */
				if (string && !stringClosed) {
					Report.report(new Position(startRow, startCol, endRow, endCol), 
							"String literal is not properly closed by a single-quote.");
					break;
				}
				
				return returnSymbol();
			}
			
			/**
			 * Skip whitespaces (if not in string state).
			 */
			else if (nxtCh == 32 || nxtCh == 9 || nxtCh == 10 || nxtCh == 13) {
				Symbol s = returnSymbol();
				
				if (nxtCh == 10) {
					// exit comment state
					comment = false;
					
					/**
					 * If in string state and string not properly closed,
					 * return lexal-error.
					 */
					if (string && !stringClosed) {
						Report.report(new Position(startRow, startCol, endRow, endCol), 
								"String literal is not properly closed by a single-quote.");
						break;
					}
					
					/**
					 * Update counters.
					 */
					startRow++;
					endRow++;
					startCol = 1;
					endCol = 0;
					if (s != null) return s;
				}
				
				/**
				 * Update counters.
				 */
				if (word.length() == 0) {
					if (nxtCh == 32) startCol++;
					if (nxtCh == 9) startCol += 4;
				}
				else {
					if (nxtCh == 32) endCol++;
					if (nxtCh == 9) endCol += 4;
				}
				
				if (s != null && !string) return s;
				if (!string) continue;
			}
			
			/**
			 * If character is '#' and not in string state, enter comment state.
			 */
			else if (nxtCh == '#' && !string) {
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
			 * Entering state-machine. --------------------------------------------------
			 */
			
			/** 
			 * If not in string state, 
			 * first check if character is an operator (',', ';' .... )
			 */
			if (!string) {
				Symbol operator = isOperator(nxtCh);
				// if this is operator, return currently processed word and save the character
				if (operator != null) {
					dontRead = true;
					previous = nxtCh;
					
					Symbol s = returnSymbol();
					if (s != null) return s;
					
					continue;
				}
			}
			// append current character
			word.append((char)nxtCh);
			
			/**
			 * If this is the first character, we can determine whether
			 * it will be a number, string or identifier.
			 * If current character is a single quote, it is a string. 
			 * If current character is numeric, it is a number.
			 * Otherwise an identifier.
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
					 * If current character is single-quote, exit numeric and enter
					 * string state.
					 */
					if (nxtCh == '\'') {
						dontRead = true;
						
						Symbol s = returnSymbol();
						if (s != null) return s;
						
						continue;
					}
					
					/**
					 * If this is second character and previous character was '0',
					 * this must either be 'x' or char in range '0' - '7'.
					 * Otherwise return lexal-error.
					 */
					if (previous == '0' && word.length() == 2) {
						/**
						 * Enter hexadecimal state.
						 */
						if (nxtCh == 'x' || nxtCh == 'X') {
							hexadecimal = true;
							continue;
						}
						/**
						 * Enter octal state.
						 */
						else if (isOctal(nxtCh))
							octal = true;
						/**
						 * Return lexal-error ilegal char.
						 */
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
				 * If in string state, read characters until a single-quote.
				 * When found, also check the next character, because double single-quote 
				 * is a single-quote part of the string.
				 */
				else if (string) {
					if (nxtCh >= 32 && nxtCh < 126) {
						if (nxtCh == '\'') {
							previous = '\'';
							stringClosed = true;

							nxtCh = file.read();
							endCol++;
							
							if (nxtCh != '\'') {
								dontRead = true;
								return returnSymbol();
							}
							else 
								stringClosed = false;
						}
					}
					else {
						Report.report(new Position(startRow, startCol, endRow, endCol),
								"Invalid character in string constant!");
						break;
					}
				}
				/**
				 * Allow numbers, letters and '_' to be in identifier.
				 */
				else if (identifier) {
					if (!isLegal(nxtCh)) {
						Report.report(new Position(startRow, startCol, endRow, endCol),
								"Ilegal character ['" + (char)nxtCh + "'] in identifier!");
						break;
					}
				}
			}
		}
		
		return null;
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
	
	private boolean isLegal(int ch) {
		return (isNumeric(nxtCh) || (ch >= 'a' && ch <= 'z') ||
				(ch >= 'A' && ch <= 'Z') || ch == '_');
	}

	private boolean isNumeric(int ch) {
		return (ch >= '0' && ch <= '9');
	}

	private boolean isHexadecimal(int ch) {
		return isNumeric(ch) || (ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F');
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
