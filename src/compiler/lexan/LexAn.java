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
			Symbol s = parseSymbol();

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
			startCol += word.length();
			word = new StringBuilder(); 
			
			if (!dontRead) nxtCh = file.read();
			else dontRead = false;
			
			if (nxtCh == -1) return new Symbol(Token.EOF, "$", 
					startRow, startCol, startRow, startCol);
			
			if (nxtCh == '#') {
				startRow++;
				while (nxtCh != -1 && nxtCh != 10) {
					nxtCh = file.read();
				}
			}
			
			if (nxtCh == '\'') {
				word.append('\'');
				while (true) {
					nxtCh = file.read();
					if (nxtCh < 32 || nxtCh > 126) {
						if (isWhiteSpace(nxtCh) || nxtCh == -1) break;
						Report.report("Invalid token in string constant");
						return null;
					}
					
					word.append((char)nxtCh);
					
					if (nxtCh == '\'') {
						nxtCh = file.read();
						if (nxtCh == '\'') word.append((char)nxtCh);
						else {
							dontRead = true;
							break;
						}
					}
				}
				// if last character of the word is't single-quote, report error
				if (word.charAt(word.length() - 1) != '\'') {
					Report.report("String not properly closed with single-quote");
				}
				
				return new Symbol(Token.STR_CONST, word.toString(), 
						startRow, startCol, startRow, startCol + word.length());
			}
			
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
					
					if (isOperator(nxtCh) != null || isWhiteSpace(nxtCh) || nxtCh == -1) {
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
					if (!isLegalId(nxtCh)) {
						Report.report("Invalid token \"" + (char)nxtCh + "\" in identifier");
						return null;
					}
				}
			}
			
			Symbol op = isOperator(nxtCh);
			if (op != null) {
				int tmpCh = file.read();
				Symbol op2 = isOperator2(nxtCh, tmpCh);
				if (op2 != null) return op2;
				dontRead = true;
				nxtCh = tmpCh;
				return op;
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
	
	private boolean isWhiteSpace(int ch) {
		return (ch == 32 || ch == 9 || ch == 13 || ch == 10);
	}
	
	private boolean isLegalId(int ch) {
		return isNumeric(nxtCh) || 
				(nxtCh >= 'a' && nxtCh <= 'z') ||
				(nxtCh >= 'A' && nxtCh <= 'Z') ||
				 nxtCh == '_';
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
