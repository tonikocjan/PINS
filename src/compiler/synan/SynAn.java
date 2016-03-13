package compiler.synan;

import compiler.Report;
import compiler.lexan.*;

/**
 * Sintaksni analizator.
 * 
 * @author sliva
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
							parseExpressions();
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
	
	private void parseExpressions() {
		parseExpression();
		skipSymbol();
		parseExpressions_();
	}
	
	private void parseExpressions_() {
		if (symbol.token == Token.COMMA) {
			parseExpression();
		}
		//error(previous, "Error on token \"" + previous.lexeme + "\", expected \",\" after this token");
	}
	
	private void parseExpression() {
		parseLogicalIORExpression();
		skipSymbol();
		parseExpression_();
	}
	
	private void parseExpression_() {
		if (symbol.token == Token.LBRACE) {
			if (next().token == Token.KW_WHERE) {
				skipSymbol();
				parseDefinitions();
			}
			error("Error on token \"" + symbol.token + "\", expected keyword \"WHERE\"");
		}
	}
	
	private void parseLogicalIORExpression() {
		parseLogicalANDExpression();
		skipSymbol();
		parseLogicalIORExpression_();
	}

	private void parseLogicalIORExpression_() {
		if (symbol.token == Token.IOR) {
			skipSymbol();
			parseLogicalIORExpression();
		}
	}
	
	private void parseLogicalANDExpression() {
		parseCmpExpression();
		skipSymbol();
		parseLogicalANDExpression_();
	}
	
	private void parseLogicalANDExpression_() {
		if (symbol.token == Token.AND) {
			skipSymbol();
			parseLogicalANDExpression();
		}
	}
	
	private void parseCmpExpression() {
		parseAddExpression();
		skipSymbol();
		switch (symbol.token) {
		case Token.EQU:
		case Token.NEQ:
		case Token.LEQ:
		case Token.GEQ:
		case Token.LTH:
		case Token.GTH:
			skipSymbol();
			parseAddExpression();
		}
	}
	
	private void parseAddExpression() {
		parseMulExpression();
		skipSymbol();
		parseAddExpression_();
	}

	private void parseAddExpression_() {
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
			skipSymbol();
			parseAddExpression();
		}
	}
	
	private void parseMulExpression() {
		parsePrefixExpression();
		skipSymbol();
		parseMulExpression_();
	}
	
	private void parseMulExpression_() {
		switch (symbol.token) {
		case Token.MUL:
		case Token.DIV:
		case Token.MOD:
			skipSymbol();
			parseMulExpression();
		}
	}
	
	private void parsePrefixExpression() {
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
			skipSymbol();
			parsePostfixExpression();
		default:
			parsePostfixExpression();
		}
	}
	
	private void parsePostfixExpression() {
		parseAtomExpression();
		skipSymbol();
		parsePostfixExpression_();
	}
	
	private void parsePostfixExpression_() {
		if (symbol.token == Token.LBRACKET) {
			skipSymbol();
			parseExpression();
			if (next().token == Token.RBRACKET) {
				skipSymbol();
				parsePostfixExpression();
			}
			error(previous, "Error on token \"" + previous.lexeme + "\", expected \"]\" after this token");
		}
	}
	
	private void parseAtomExpression() {
		switch (symbol.token) {
		case Token.INT_CONST:
		case Token.LOG_CONST:
		case Token.STR_CONST:
			return;
		case Token.IDENTIFIER:
			if (next().token == Token.LPARENT) {
				skipSymbol();
				parseExpressions();
				if (next().token == Token.RPARENT) return;
				error(previous, "Error on token \"" + previous.lexeme + "\", expected \")\" after this token");
			}
			return;
		case Token.LBRACE:
			if (next().token == Token.KW_IF) {
				skipSymbol();
				parseExpression();
				if (symbol.token == Token.KW_THEN) {
					skipSymbol();
					parseExpression();
					if (next().token == Token.RBRACE) return;
					else if (symbol.token == Token.KW_ELSE) {
						skipSymbol();
						parseExpression();
						if (next().token == Token.RBRACE) return;
						error(previous, "Error on token \"" + previous.lexeme + "\", expected \"}\" after this token");
						return;
					}
					error("Error on token \"" + symbol.lexeme + "\", delete this token");
					return;
				}
				error("Error on token \"" + symbol.lexeme + "\"");
				return;
			}
			if (symbol.token == Token.KW_WHILE) {
				skipSymbol();
				parseExpression();
				if (next().token == Token.COLON) {
					skipSymbol();
					parseExpression();
					if (next().token == Token.RBRACE) return;
					error(previous, "Error on token \"" + previous.lexeme + "\", expected \"}\" after this token");
					return;
				}
				error(previous, "Error on token \"" + previous.lexeme + "\", expected \":\" after this token");
				return;
			}
			if (symbol.token == Token.KW_FOR) {
				if (next().token == Token.IDENTIFIER) {
					if (next().token == Token.ASSIGN) {
						skipSymbol();
						parseExpression();
						if (next().token == Token.COMMA) {
							skipSymbol();
							parseExpression();
							if (next().token == Token.COMMA) {
								skipSymbol();
								parseExpression();
								if (next().token == Token.COLON) {
									skipSymbol();
									parseExpression();
									if (next().token == Token.RBRACE) return;
									error(previous, "Error on token \"" + previous.lexeme + "\", expected \"}\" to end for-loop definition");
									return;
								}
								error(previous, "Error on token \"" + previous.lexeme + "\", expected \":\" after this token");
								return;
							}
							error(previous, "Error on token \"" + previous.lexeme + "\", expected \",\" after this token");
							return;
						}
						error(previous, "Error on token \"" + previous.lexeme + "\", expected \",\" after this token");
						return;
					}
					error(previous, "Error on token \"" + previous.lexeme + "\", expected \"=\" after this token");
					return;
				}
				error("Error on token \"" + symbol.lexeme + "\", expected identifier");
				return;
			}
			skipSymbol();
			parseExpression();
			if (next().token == Token.ASSIGN) {
				skipSymbol();
				parseExpression();
				if (next().token == Token.RBRACE) return;
				error(previous, "Error on token \"" + previous.lexeme + "\", expected \"}\" after this token");
				return;
			}
		default:
			error("Error on token \"" + symbol.lexeme + "\", expected expression-definition after this token");
		}
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
