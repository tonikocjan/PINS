package compiler.synan;

import compiler.Report;
import compiler.lexan.*;

/**
 * Sintaksni analizator.
 * 
 * @author sliva
 * @implementation Toni Kocjan
 */
public class SynAn {

	/** Leksikalni analizator. */
	private LexAn lexAn;

	/** Ali se izpisujejo vmesni rezultati. */
	private boolean dump;
	
	/** Current & previous symbol */
	private Symbol symbol = null;
	private Symbol previous = null;

	/**
	 * Ustvari nov sintaksni analizator.
	 * 
	 * @param lexAn
	 *            Leksikalni analizator.
	 * @param dump
	 *            Ali se izpisujejo vmesni rezultati.
	 */
	public SynAn(LexAn lexAn, boolean dump) {
		this.lexAn = lexAn;
		this.dump = dump;
		
		this.symbol = this.lexAn.lexAn();
		this.previous = this.symbol;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	public void parse() {
		parseSource();
	}

	/**
	 * Parse functions.
	 */
	private void parseSource() {
		parseDefinitions();

		if (symbol.token != Token.EOF)
			Report.error(symbol.position, "Syntax error!");
	}
	
	private void parseDefinitions() {
		switch (symbol.token) {
		case Token.KW_TYP:
			skipSymbol();
			parseTypeDefinition();
			break;
		case Token.KW_FUN:
			skipSymbol();
			parseFunDefinition();
			break;
		case Token.KW_VAR:
			skipSymbol();
			parseVarDefinition();
			break;
		default:
			if (symbol.token != Token.EOF)
				Report.error(symbol.position, 
						"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
			else
				Report.error(previous.position, 
						"Syntax error on token \"" + previous.lexeme + "\", delete this token");
		}
		skipSymbol();
		parseDefinitions_();
	}

	private void parseDefinitions_() {
		switch (symbol.token) {
		case Token.EOF:
			break;
		case Token.SEMIC:
			skipSymbol();
			parseDefinitions();
			break;
		default:
			Report.error(previous.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected \";\" after this token");
		}
	}

	private void parseTypeDefinition() {
		if (symbol.token == Token.IDENTIFIER) {
			if (next().token == Token.COLON) {
				skipSymbol();
				parseType();
				return;
			}
			Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", expected \":\" after this token");
		}
		Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\"; identifier expected");
	}

	private void parseFunDefinition() {
		if (symbol.token == Token.IDENTIFIER) {
			if (next().token == Token.LPARENT) {
				skipSymbol();
				parseParameters();
				if (next().token == Token.COLON) {
					skipSymbol();
					parseType();
					if (next().token == Token.ASSIGN) {
						skipSymbol();
//						parseExpressions();
						return;
					}
					Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", exptected \"=\" after this token");
				}
				Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", \":\" exptected after this token");
			}
			Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", exptected \"(\" after this token");
		}
		Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", expected identifier after this token");
	}

	private void parseVarDefinition() {
		if (symbol.token == Token.IDENTIFIER) {
			if (next().token == Token.COLON) {
				skipSymbol();
				parseType();
				return;
			}
			Report.error(previous.position, "Syntax error on token \""+ previous.lexeme + "\", expected \":\" after this token");
			return;
		}
		Report.error(previous.position, "Syntax error on token \""+ previous.lexeme + "\", expected identifier after this token");
	}
	
	private void parseType() {
		switch (symbol.token) {
		case Token.IDENTIFIER:
		case Token.LOGICAL:
		case Token.INTEGER:
		case Token.STRING:
			break;
		case Token.KW_ARR:
			if (next().token == Token.LBRACKET) {
				if (next().token == Token.INT_CONST) {
					if (next().token == Token.RBRACKET) {
						skipSymbol();
						parseType();
						return;
					}
					Report.error(symbol.position, "Syntax error, insert \"]\" to complete Dimensions");
				}
				Report.error(symbol.position, "Variable must provide array dimension expression");
			}
			Report.error(symbol.position, "Syntax error, insert \"[\"");
		default:
			Report.error(symbol.position, "Syntax error on token \"" + symbol.lexeme + "\", expected \"variable type\"");
		}
	}
	
	private void parseParameters() {
		if (symbol.token == Token.IDENTIFIER) {
			if (next().token == Token.COLON) {
				skipSymbol();
				parseType();
				skipSymbol();
				parseParameters_();
				return;
			}
			Report.error(symbol.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected \":\" after this token");
		}
		Report.error(symbol.position, "Syntax error, expected paramater definition");
	}
	
	private void parseParameters_() {
		if (symbol.token == Token.COMMA) {
			skipSymbol();
			parseParameters();
		}
		else if (symbol.token != Token.RPARENT)
			Report.error(symbol.position, 
					"Syntax error, insert \")\" to complete function declaration");
	}
	
	/**
	 * 
	 */
	private void skipSymbol() {
		previous = symbol;
		symbol = lexAn.lexAn();
	}
	
	private Symbol next() {
		skipSymbol();
		return symbol;
	}

	/**
	 * Izpise produkcijo v datoteko z vmesnimi rezultati.
	 * 
	 * @param production
	 *            Produkcija, ki naj bo izpisana.
	 */
	private void dump(String production) {
		if (!dump)
			return;
		if (Report.dumpFile() == null)
			return;
		Report.dumpFile().println(production);
	}

}
