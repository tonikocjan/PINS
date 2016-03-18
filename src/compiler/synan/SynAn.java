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
			Report.report(symbol.position, "Syntax error!");
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
				error("Syntax error on token \"" + symbol.lexeme + "\", delete this token");
			else
				error(previous, "Syntax error on token \"" + previous.lexeme + "\", delete this token");
			return;
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
			error(previous, "Syntax error on token \"" + previous.lexeme + "\", expected \";\" after this token");
		}
	}

	private void parseTypeDefinition() {
		if (symbol.token == Token.IDENTIFIER) {
			if (next().token == Token.COLON) {
				skipSymbol();
				parseType();
				return;
			}
			error(previous, "Syntax error on token \"" + previous.lexeme + "\", expected \":\" after this token");
			return;
		}
		error(previous, "Syntax error on token \"" + previous.lexeme + "\"; identifier expected");
		return;
	}

	private void parseFunDefinition() {
		if (symbol.token == Token.IDENTIFIER) {
			if (next().token == Token.LPARENT) {
				skipSymbol();
				parseParameters();
				if (symbol.token != Token.EOF) {
					if (next().token == Token.COLON) {
						skipSymbol();
						parseType();
						if (symbol.token == Token.EOF) return;
						
						if (next().token == Token.ASSIGN) {
							skipSymbol();
//							parseExpressions();
							return;
						}
						error(previous, "Syntax error on token \"" + previous.lexeme + "\", exptected \"=\" after this token");
						return;
					}
					error(previous, "Syntax error on token \"" + previous.lexeme + "\", \":\" exptected after this token");
					return;
				}
				error(previous, "Syntax error, insert \")\" to complete function declaration");
				return;
			}
			error(previous, "Syntax error on token \"" + previous.lexeme + "\", exptected \"(\" after this token");
			return;
		}
		error(previous, "Syntax error on token \"" + previous.lexeme + "\", expected identifier after this token");
	}

	private void parseVarDefinition() {
		if (symbol.token == Token.IDENTIFIER) {
			if (next().token == Token.COLON) {
				skipSymbol();
				parseType();
				return;
			}
			error(previous, "Syntax error on token \""+ previous.lexeme + "\", expected \":\" after this token");
			return;
		}
		error(previous, "Syntax error on token \""+ previous.lexeme + "\", expected identifier after this token");
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
					error("Syntax error, insert \"]\" to complete Dimensions");
					break;
				}
				error("Variable must provide array dimension expression");
				break;
			}
			else error("Syntax error, insert \"[\"");
			break;
		default:
			error("Syntax error on token \"" + symbol.lexeme + "\", expected \"variable type\"");
		}
	}
	
	private void parseParameters() {
		switch (symbol.token) {
		case Token.IDENTIFIER:
			if (next().token == Token.COLON) {
				skipSymbol();
				parseType();
				return;
			}
			error("Synax error on token \"" + previous.lexeme + "\", expected \":\" after this token");
			break;
		}
//		skipSymbol();
		parseParameters_();
	}
	
	private void parseParameters_() {
		switch (symbol.token) {
		case Token.COMMA:
			parseParameters();
			break;
		case Token.RPARENT:
			return;
		}
		error("Syntax error on token \"" + previous.lexeme + "\"");
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
	
	private void error(String err) {
		error(symbol, err);
	}
	
	private void error(Symbol s, String err) {
		Report.report(s.position, err);
		symbol = new Symbol(Token.EOF, "$", s.position);
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
