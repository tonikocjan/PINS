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
	 * Current caracter.
	 */
	private int nxtCh = -1;

	/**
	 * Position.
	 */
	private int startCol = 1, startRow = 1;

	/**
	 * 
	 */
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
			Report.error("File [ " + sourceFileName + " ] does not exist! Exiting.");
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
			Symbol s = parseSymbol();
			if (s == null) 
				s = new Symbol(Token.EOF, "$", startRow, startCol, startRow, startCol + 1);

			dump(s);
			
			return s;

		} catch (IOException e) {
			Report.error("Error while parsing input file! Exiting ...");
		}

		return null;
	}

	/**
	 * Parse next symbol in file. If symbol is not lexically correct, report error.
	 * @return next symbol in file or null, if error detected
	 * @throws IOException
	 */
	private Symbol parseSymbol() throws IOException {
		while (true) {
			startCol += word.length();
			word = new StringBuilder(); 
			
			if (!dontRead) nxtCh = file.read();
			else dontRead = false;
			
			/**
			 * Skip characters after '#'.
			 */
			if (nxtCh == '#') 
				while (nxtCh != -1 && nxtCh != 10) {
					nxtCh = file.read();
					if ((nxtCh < 32 || nxtCh > 126) && !isWhiteSpace(nxtCh) && nxtCh != -1)
						Report.error(new Position(startRow, startCol), "Invalid token in comment");
				}
		
			/**
			 * Handle EOF.
			 */
			if (nxtCh == -1) 
				return new Symbol(Token.EOF, "$", startRow, startCol, startRow, startCol);
			
			/**
			 * Handle whitespaces.
			 */
			if (isWhiteSpace(nxtCh)) {
				// update counters
				if (nxtCh == 32 || nxtCh == 9) startCol += (nxtCh == 32) ? 1 : 4;
				else if (nxtCh == 10) {
					startCol = 1;
					startRow++;
				}
				continue;
			}
			
			/**
			 * Parse string.
			 */
			if (nxtCh == '\'') {
				word.append('\'');
				boolean strClosed = false;
				while (true) {
					nxtCh = file.read();
					if (nxtCh < 32 || nxtCh > 126) {
						if (isWhiteSpace(nxtCh) || nxtCh == -1) break;
						Report.error(new Position(startRow, startCol, startRow, startCol + word.length() + 1), 
								"Invalid token in string constant");
					}
					
					word.append((char)nxtCh);
					
					if (nxtCh == '\'') {
						nxtCh = file.read();
						if (nxtCh == '\'') word.append((char)nxtCh);
						else {
							dontRead = true;
							strClosed = true;
							break;
						}
					}
				}
				// if last character of the word isn't single-quote, report error
				if (!strClosed) {
					Report.error(new Position(startRow, startCol, startRow, startCol + word.length()),
							"String literal not properly closed");
				}
				
				return new Symbol(Token.STR_CONST, word.toString(), 
						startRow, startCol, startRow, startCol + word.length());
			}
			
			/**
			 * Parse int const.
			 */
			if (isNumeric(nxtCh)) {
				while (isNumeric(nxtCh)) {
					word.append((char)nxtCh);
					nxtCh = file.read();
				}
				dontRead = true;

				return new Symbol(Token.INT_CONST, word.toString(), 
						startRow, startCol, startRow, startCol + word.length());
			}
			
			/**
			 * Parse identifier.
			 */
			if (isLegalId(nxtCh)) {
				while (true) {
					word.append((char)nxtCh);
					nxtCh = file.read();
					
					/**
					 * Delemiters for identifier are:
					 * - whitespaces
					 * - EOF
					 * - operator
					 * - single-quote
					 */
					if (isOperator(nxtCh) != null || isWhiteSpace(nxtCh) || 
							nxtCh == -1 || nxtCh == '\'') {
						dontRead = true;
						int token = Token.IDENTIFIER;
						
						// Check if word is keyword
						if (keywordsMap.containsKey(word.toString())) 
							token = keywordsMap.get(word.toString());
						// Check if word is log const
						if (word.toString().equals("true") || word.toString().equals("false"))
							token = Token.LOG_CONST;
						
						return new Symbol(token, word.toString(), 
								startRow, startCol, startRow, startCol + word.length());
					}
					/**
					 * If this is not legal identifier character, report error.
					 */
					if (!isLegalId(nxtCh))
						Report.error(new Position(startRow, startCol, startRow, startCol + word.length() + 1), 
								"Invalid token \"" + (char)nxtCh + "\" in identifier");
				}
			}
			
			/**
			 * Parse operator.
			 */
			Symbol op = isOperator(nxtCh);
			if (op != null) {
				/**
				 * Also check if this character + next character is an operator.
				 */
				int tmpCh = file.read();
				Symbol op2 = isOperator2(nxtCh, tmpCh);
				if (op2 != null) {
					startCol += 2;
					return op2;
				}
				
				dontRead = true;
				nxtCh = tmpCh;
				startCol++;
				return op;
			}
			
			/**
			 * Unknown character. Report error.
			 */
			Report.error(new Position(startRow, startCol, startRow, startCol + word.length() + 1),
					"Unknown token \"" + (char)nxtCh + "\", delete this token");
		}
	}
	
	/**
	 * Check if character is an operator.
	 * @param ch
	 * @return detected operator or null, if no operator is detected
	 */
	private Symbol isOperator(int ch) {
		if (ch == '+') return new Symbol(Token.ADD, "+", startRow, startCol, startRow, startCol + 1);
		if (ch == '-') return new Symbol(Token.SUB, "-", startRow, startCol, startRow, startCol + 1);
		if (ch == '*') return new Symbol(Token.MUL, "*", startRow, startCol, startRow, startCol + 1);
		if (ch == '/') return new Symbol(Token.DIV, "/", startRow, startCol, startRow, startCol + 1);
		if (ch == '%') return new Symbol(Token.MOD, "%", startRow, startCol, startRow, startCol + 1);

		if (ch == '&') return new Symbol(Token.AND, "&", startRow, startCol, startRow, startCol + 1);
		if (ch == '|') return new Symbol(Token.IOR, "|", startRow, startCol, startRow, startCol + 1);
		if (ch == '!') return new Symbol(Token.NOT, "!", startRow, startCol, startRow, startCol + 1);

		if (ch == '(') return new Symbol(Token.LPARENT, "(", startRow, startCol, startRow, startCol + 1);
		if (ch == ')') return new Symbol(Token.RPARENT, ")", startRow, startCol, startRow, startCol + 1);
		if (ch == '{') return new Symbol(Token.LBRACE, "{", startRow, startCol, startRow, startCol + 1);
		if (ch == '}') return new Symbol(Token.RBRACE, "}", startRow, startCol, startRow, startCol + 1);
		if (ch == '[') return new Symbol(Token.LBRACKET, "[", startRow, startCol, startRow, startCol + 1);
		if (ch == ']') return new Symbol(Token.RBRACKET, "]", startRow, startCol, startRow, startCol + 1);

		if (ch == '<') return new Symbol(Token.LTH, "<", startRow, startCol, startRow, startCol + 1);
		if (ch == '>') return new Symbol(Token.GTH, ">", startRow, startCol, startRow, startCol + 1);
		if (ch == '=') return new Symbol(Token.ASSIGN, "=", startRow, startCol, startRow, startCol + 1);

		if (ch == '.') return new Symbol(Token.DOT, ",", startRow, startCol, startRow, startCol + 1);
		if (ch == ':') return new Symbol(Token.COLON, ":", startRow, startCol, startRow, startCol + 1);
		if (ch == ';') return new Symbol(Token.SEMIC, ";", startRow, startCol, startRow, startCol + 1);
		if (ch == ',') return new Symbol(Token.COMMA, ",", startRow, startCol, startRow, startCol + 1);

		return null;
	}
	
	/**
	 * Check if this two characters are an operator.
	 * @param ch1
	 * @param ch2
	 * @return detected operator or null, if no operator is detected 
	 */
	private Symbol isOperator2(int ch1, int ch2) {
		if (ch1 == '=' && ch2 == '=') 
			return new Symbol(Token.EQU, "==", startRow, startCol, startRow, startCol + 2);
		if (ch1 == '!' && ch2 == '=')
			return new Symbol(Token.NEQ, "!=", startRow, startCol, startRow, startCol + 2);
		if (ch1 == '>' && ch2 == '=')
			return new Symbol(Token.GEQ, ">=", startRow, startCol, startRow, startCol + 2);
		if (ch1 == '<' && ch2 == '=')
			return new Symbol(Token.LEQ, "<=", startRow, startCol, startRow, startCol + 2);
		return null;
	}
	
	/**
	 * @param ch character to be checked
	 * @return true if character is a number; false otherwise
	 */
	private boolean isNumeric(int ch) {
		return (ch >= '0' && ch <= '9');
	}
	
	/**
	 * @param ch character to be checked
	 * @return true if character is whitespace; false otherwise
	 */
	private boolean isWhiteSpace(int ch) {
		return (ch == 32 || ch == 9 || ch == 13 || ch == 10);
	}
	
	/**
	 * @param ch character to be checked
	 * @return true if character is legal identifier character; false otherwise
	 */
	private boolean isLegalId(int ch) {
		return isNumeric(nxtCh) || nxtCh == '_' ||
				(nxtCh >= 'a' && nxtCh <= 'z') ||
				(nxtCh >= 'A' && nxtCh <= 'Z');
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
