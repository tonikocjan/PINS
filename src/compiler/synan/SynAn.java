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
		dump("source -> definitions");
		parseDefinitions();

		if (symbol.token != Token.EOF)
			Report.error(symbol.position, "Syntax error!");
	}
	
	private void parseDefinitions() {
		switch (symbol.token) {
		case Token.KW_TYP:
			dump("definitions -> definition");
			dump("definition -> type_definition");
			skipSymbol();
			parseTypeDefinition();
			break;
		case Token.KW_FUN:
			dump("definitions -> definition");
			dump("definition -> fun_definition");
			skipSymbol();
			parseFunDefinition();
			break;
		case Token.KW_VAR:
			dump("definitions -> definition");
			dump("definition -> var_definition");
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
//		skipSymbol();
		parseDefinitions_();
	}

	private void parseDefinitions_() {
		switch (symbol.token) {
		case Token.EOF:
			dump("definitions -> $");
			break;
		case Token.RBRACE:
			skipSymbol();
			break;
		case Token.SEMIC:
			skipSymbol();
			parseDefinitions();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
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
				if (symbol.token == Token.COLON) {
					skipSymbol();
					parseType();
					if (symbol.token == Token.ASSIGN) {
						skipSymbol();
						parseExpression();
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
		}
		Report.error(previous.position, "Syntax error on token \""+ previous.lexeme + "\", expected identifier after this token");
	}
	
	private void parseType() {
		switch (symbol.token) {
		case Token.IDENTIFIER:
			dump("type -> identifier");
		case Token.LOGICAL:
			dump("type -> logical");
		case Token.INTEGER:
			dump("type -> integer");
		case Token.STRING:
			dump("type -> string");
			skipSymbol();
			break;
		case Token.KW_ARR:
			dump("type -> arr [ int_const ] type");
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
		dump("parameters -> parameter");
		if (symbol.token == Token.IDENTIFIER) {
			if (next().token == Token.COLON) {
				skipSymbol();
				parseType();
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
			dump("parameters -> parameter");
			skipSymbol();
			parseParameters();
			return;
		}
		else if (symbol.token != Token.RPARENT)
			Report.error(symbol.position, 
					"Syntax error, insert \")\" to complete function declaration");
		skipSymbol();
	}
	
	private void parseExpressions() {
		dump("expressions -> expression");
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			parseExpression();
			parseExpressions_();
			break;
		default:
			Report.error(symbol.position,
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseExpressions_() {
		switch (symbol.token) {
		case Token.COMMA:
			dump("expressions -> expression");
			skipSymbol();
			parseExpressions();
			break;
		case Token.RPARENT:
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error expressions_");
		}
	}
	
	private void parseExpression() {
		dump("expression -> logical_ior_expression");
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			parseIorExpression();
			parseExpression_();
			break;
		default:
			Report.error(symbol.position,
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseExpression_() {
		dump("expression -> logical_ior_expression { WHERE definitions}");
		switch (symbol.token) {
		case Token.LBRACE:
			if (next().token == Token.KW_WHERE) {
				skipSymbol();
				parseDefinitions();
				return;
			}
			Report.error(symbol.position,
					"Syntax error on token \"" + previous.lexeme + "\", expected keyword \"where\" after this token");
			break;
		case Token.SEMIC:
		case Token.COLON:
		case Token.RPARENT:
		case Token.ASSIGN:
		case Token.RBRACE:
		case Token.RBRACKET:
		case Token.KW_THEN:
		case Token.KW_ELSE:
		case Token.COMMA:
		case Token.EOF:
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseIorExpression() {
		dump("logical_ior_expression -> logical_and_expression");
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			parseAndExpression();
			parseIorExpression_();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseIorExpression_() {
		switch (symbol.token) {
		case Token.IOR:
			skipSymbol();
			parseIorExpression();
			break;
		case Token.SEMIC:
		case Token.COLON:
		case Token.RPARENT:
		case Token.ASSIGN:
		case Token.RBRACE:
		case Token.RBRACKET:
		case Token.KW_THEN:
		case Token.KW_ELSE:
		case Token.COMMA:
		case Token.EOF:
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseAndExpression() {
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			dump("logical_and_expression -> logical_cmp_expression");
			parseCmpExpression();
			parseAndExpression_();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseAndExpression_() {
		switch (symbol.token) {
		case Token.AND:
			skipSymbol();
			parseAndExpression();
			break;
		case Token.IOR:
		case Token.SEMIC:
		case Token.COLON:
		case Token.RPARENT:
		case Token.ASSIGN:
		case Token.RBRACE:
		case Token.RBRACKET:
		case Token.KW_THEN:
		case Token.KW_ELSE:
		case Token.COMMA:
		case Token.EOF:
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}

	private void parseCmpExpression() {
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			parseAddExpression();
			parseCmpExpression_();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseCmpExpression_() {
		switch (symbol.token) {
		case Token.AND:
		case Token.IOR:
		case Token.SEMIC:
		case Token.COLON:
		case Token.RPARENT:
		case Token.ASSIGN:
		case Token.RBRACE:
		case Token.RBRACKET:
		case Token.KW_THEN:
		case Token.KW_ELSE:
		case Token.COMMA:
		case Token.EOF:
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}

	private void parseAddExpression() {
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			parseMulExpression();
			parseAddExpression_();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseAddExpression_() {
		switch (symbol.token) {
		case Token.AND:
		case Token.IOR:
		case Token.SEMIC:
		case Token.COLON:
		case Token.RPARENT:
		case Token.ASSIGN:
		case Token.RBRACE:
		case Token.RBRACKET:
		case Token.KW_THEN:
		case Token.KW_ELSE:
		case Token.COMMA:
		case Token.EOF:
			break;
		case Token.EQU:
		case Token.NEQ:
		case Token.GTH:
		case Token.LTH:
		case Token.GEQ:
		case Token.LEQ:
			skipSymbol();
			/**
			 *  TODO al je add al cmp expression
			 */
			parseCmpExpression();
			break;
		case Token.ADD:
		case Token.SUB:
			skipSymbol();
			parseAddExpression();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on parseAddExpression_");
		}
	}
	
	private void parseMulExpression() {
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			parsePrefixExpression();
			parseMulExpression_();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}

	private void parseMulExpression_() {
		switch (symbol.token) {
		case Token.AND:
		case Token.IOR:
		case Token.SEMIC:
		case Token.COLON:
		case Token.RPARENT:
		case Token.ASSIGN:
		case Token.RBRACE:
		case Token.RBRACKET:
		case Token.KW_THEN:
		case Token.KW_ELSE:
		case Token.COMMA:
		case Token.EOF:
			break;
		case Token.EQU:
		case Token.NEQ:
		case Token.GTH:
		case Token.LTH:
		case Token.GEQ:
		case Token.LEQ:
			skipSymbol();
			/**
			 *  TODO al je add al cmp expression
			 */
			parseCmpExpression();
			break;
		case Token.ADD:
		case Token.SUB:
			skipSymbol();
			parseAddExpression();
			break;
		case Token.MUL:
		case Token.DIV:
		case Token.MOD:
			skipSymbol();
			parseMulExpression();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parsePrefixExpression() {
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
			skipSymbol();
			parsePostfixExpression();
			break;
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			parsePostfixExpression();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parsePostfixExpression() {
		switch (symbol.token) {
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			parseAtomExpression();
			parsePostfixExpression_();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parsePostfixExpression_() {
		switch (symbol.token) {
		case Token.AND:
		case Token.IOR:
		case Token.SEMIC:
		case Token.COLON:
		case Token.RPARENT:
		case Token.ASSIGN:
		case Token.RBRACE:
		case Token.RBRACKET:
		case Token.KW_THEN:
		case Token.KW_ELSE:
		case Token.COMMA:
		case Token.EOF:
			break;
		case Token.EQU:
		case Token.NEQ:
		case Token.GTH:
		case Token.LTH:
		case Token.GEQ:
		case Token.LEQ:
			skipSymbol();
			/**
			 *  TODO al je add al cmp expression
			 */
			parseCmpExpression();
			break;
		case Token.ADD:
		case Token.SUB:
			skipSymbol();
			parseAddExpression();
			break;
		case Token.MUL:
		case Token.DIV:
		case Token.MOD:
			skipSymbol();
			parseMulExpression();
			break;
		case Token.LBRACKET:
			skipSymbol();
			parseExpression();
			if (symbol.token != Token.RBRACKET)
				Report.report(previous.position, 
						"Syntax error, insert \"]\"");
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseAtomExpression() {
		switch (symbol.token) {
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
			skipSymbol();
			break;
		case Token.LBRACE:
			skipSymbol();
			parseAtomExprBrace();
			break;
		case Token.LPARENT:
			skipSymbol();
			parseExpressions();
			if (symbol.token != Token.RPARENT)
				Report.report(symbol.position, 
						"Syntax error on token \"" + previous.lexeme + "\", expected \"]\"");
			break;
		case Token.IDENTIFIER:
			skipSymbol();
			if (symbol.token == Token.LPARENT) {
				parseExpressions();
				if (symbol.token != Token.RPARENT)
					Report.report(symbol.position, 
							"Syntax error on token \"" + previous.lexeme + "\", expected \"]\"");
				skipSymbol();
			}
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseAtomExprBrace() {
		if (symbol.token == Token.KW_IF) parseIf();
		else if (symbol.token == Token.KW_WHILE) parseWhileLoop();
		else if (symbol.token == Token.KW_FOR) parseForLoop();
		else {
			// else
			parseExpression();
			if (symbol.token == Token.ASSIGN) {
				skipSymbol();
				parseExpression();
			}
			else {
				Report.error(symbol.position, 
						"Syntax error on token \"" + previous.lexeme + "\", expected \"=\" after this token");
			}
		}
		if (symbol.token != Token.RBRACE) {
			Report.error(symbol.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected \"}\" after this token");
		}
		skipSymbol();
	}
	
	private void parseForLoop() {
		if (symbol.token == Token.KW_FOR) {
			if (next().token == Token.IDENTIFIER) {
				if (next().token == Token.ASSIGN) {
					skipSymbol();
					parseExpression();
					if (symbol.token == Token.COMMA) {
						skipSymbol();
						parseExpression();
						if (symbol.token == Token.COMMA) {
							skipSymbol();
							parseExpression();
							if (symbol.token == Token.COLON) {
								skipSymbol();
								parseExpression();
								return;
							}
							Report.error(symbol.position, 
									"Syntax error on token \"" + previous.lexeme + "\", expected \":\" after this token");
						}
						Report.error(symbol.position, 
								"Syntax error on token \"" + previous.lexeme + "\", expected \",\" after this token");
					}
					Report.error(symbol.position, 
							"Syntax error on token \"" + previous.lexeme + "\", expected \",\" after this token");
				}
				Report.error(symbol.position, 
						"Syntax error on token \"" + previous.lexeme + "\", expected \"=\" after this token");
			}
			Report.error(symbol.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected identifier after this token");
		}
		Report.error(symbol.position, 
				"Syntax error, expected keyword \"for\"");
	}
	
	private void parseWhileLoop() {
		if (symbol.token == Token.KW_WHILE) {
			skipSymbol();
			parseExpression();
			if (symbol.token == Token.COLON) {
				skipSymbol();
				parseExpression();
				return;
			}
			Report.error(symbol.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected \":\" after this token");
		}
		Report.report(symbol.position, "Syntax error, expected keyword \"while\"");
	}
	
	private void parseIf() {
		if (symbol.token == Token.KW_IF) {
			skipSymbol();
			parseExpression();
			if (symbol.token == Token.KW_THEN) {
				skipSymbol();
				parseExpression();
				if (symbol.token == Token.KW_ELSE) {
					skipSymbol();
					parseExpression();
				}
				if (symbol.token == Token.RBRACE) return;

				Report.error(symbol.position, 
						"Syntax error on token \"" + symbol.lexeme + "\", expected \"}\"");
			}
			Report.error(symbol.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected keyword \"then\" after this token");
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
