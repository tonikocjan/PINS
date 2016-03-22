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
		if (symbol == null) Report.error("Error accessing LexAn");
		
		parseSource();
	}

	/**
	 * Parse functions.
	 */
	private void parseSource() {
		dump("source -> definitions");
		parseDefinitions();

		if (symbol.token != Token.EOF)
			Report.error(symbol.position, "Syntax error on token \"" + previous.lexeme + "\"");
	}
	
	private void parseDefinitions() {
		switch (symbol.token) {
		case Token.KW_TYP:
			dump("definitions -> definition definition'");
			dump("definition -> type_definition");
			skipSymbol();
			parseTypeDefinition();
			break;
		case Token.KW_FUN:
			dump("definitions -> definition definition'");
			dump("definition -> function_definition");
			skipSymbol();
			parseFunDefinition();
			break;
		case Token.KW_VAR:
			dump("definitions -> definition definition'");
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
			dump("definitions' -> $");
			break;
		case Token.RBRACE:
			dump("definitions' -> e");
			skipSymbol();
			break;
		case Token.SEMIC:
			dump("definitions' -> ; definitions");
			skipSymbol();
			parseDefinitions();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected \";\" or \"}\" after this token");
		}
	}

	private void parseTypeDefinition() {
		if (symbol.token == Token.IDENTIFIER) {
			if (next().token == Token.COLON) {
				dump("type_definition -> typ identifier : type");
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
				dump("function_definition -> fun identifier ( parameters ) : type = expression");
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
				dump("var_definition -> var identifier : type");
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
			skipSymbol();
			break;
		case Token.LOGICAL:
			dump("type -> logical");
			skipSymbol();
			break;
		case Token.INTEGER:
			dump("type -> integer");
			skipSymbol();
			break;
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
		dump("parameters -> parameter parameters'");
		if (symbol.token == Token.IDENTIFIER) {
			if (next().token == Token.COLON) {
				dump("parameter -> identifier : type");
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
			dump("parameters' -> parameters");
			skipSymbol();
			parseParameters();
			return;
		}
		else if (symbol.token != Token.RPARENT)
			Report.error(symbol.position, 
					"Syntax error, insert \")\" to complete function declaration");
		dump("parameters' -> e");
		skipSymbol();
	}
	
	private void parseExpressions() {
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
			dump("expressions -> expression expression'");
			parseExpression();
			parseExpressions_();
			break;
		default:
			Report.error(symbol.position,
					"Syntax error on token \"" + previous.lexeme + "\", delete this token");
		}
	}
	
	private void parseExpressions_() {
		switch (symbol.token) {
		case Token.COMMA:
			dump("expressions' -> , expression expression'");
			skipSymbol();
			parseExpressions();
			break;
		case Token.RPARENT:
			dump("expressions' -> e");
			skipSymbol();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected \",\" or \")\" to end expression");
		}
	}
	
	private void parseExpression() {
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
			dump("expression -> logical_ior_expression");
			parseIorExpression();
			parseExpression_();
			break;
		default:
			Report.error(symbol.position,
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseExpression_() {
		switch (symbol.token) {
		case Token.LBRACE:
			if (next().token == Token.KW_WHERE) {
				dump("expression' ->  { WHERE definitions }");
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
			dump("expression' -> e");
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseIorExpression() {
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
			dump("logical_ior_expression -> logical_and_expression logical_ior_expression'");
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
			dump("logical_ior_expression' -> | log_ior_expression");
			skipSymbol();
			parseIorExpression();
			break;
		case Token.SEMIC:
		case Token.COLON:
		case Token.RPARENT:
		case Token.ASSIGN:
		case Token.RBRACE:
		case Token.LBRACE:
		case Token.RBRACKET:
		case Token.KW_THEN:
		case Token.KW_ELSE:
		case Token.COMMA:
		case Token.EOF:
			dump("logical_ior_expression' -> e");
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
			dump("logical_and_expression -> logical_compare_expression logical_and_expression'");
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
			dump("logical_and_expression' -> & logical_and_expression");
			skipSymbol();
			parseAndExpression();
			break;
		case Token.IOR:
		case Token.SEMIC:
		case Token.COLON:
		case Token.RPARENT:
		case Token.ASSIGN:
		case Token.RBRACE:
		case Token.LBRACE:
		case Token.RBRACKET:
		case Token.KW_THEN:
		case Token.KW_ELSE:
		case Token.COMMA:
		case Token.EOF:
			dump("logical_and_expression' -> e");
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
			dump("compare_expression -> add_expression compare_expression'");
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
		case Token.LBRACE:
		case Token.RBRACKET:
		case Token.KW_THEN:
		case Token.KW_ELSE:
		case Token.COMMA:
		case Token.EOF:
			dump("compare_expression' -> e");
			break;		
		case Token.EQU:
			dump("compare_expression' -> == compare_expression");
			skipSymbol();
			parseCmpExpression();
			break;
		case Token.NEQ:
			dump("compare_expression' -> != compare_expression");
			skipSymbol();
			parseCmpExpression();
			break;
		case Token.GTH:
			dump("compare_expression' -> > compare_expression");
			skipSymbol();
			parseCmpExpression();
			break;
		case Token.LTH:
			dump("compare_expression' -> < compare_expression");
			skipSymbol();
			parseCmpExpression();
			break;
		case Token.GEQ:
			dump("compare_expression' -> >= compare_expression");
			skipSymbol();
			parseCmpExpression();
			break;
		case Token.LEQ:
			dump("compare_expression' -> <= compare_expression");
			skipSymbol();
			parseCmpExpression();
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
			dump("add_expression -> multiplicative_expression add_expression'");
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
		case Token.LBRACE:
		case Token.RBRACKET:
		case Token.KW_THEN:
		case Token.KW_ELSE:
		case Token.COMMA:
		case Token.EOF:
			dump("add_expression' -> e");
			break;
		case Token.EQU:
		case Token.NEQ:
		case Token.GTH:
		case Token.LTH:
		case Token.GEQ:
		case Token.LEQ:
//			skipSymbol();
//			parseAddExpression();
			break;
		case Token.ADD:
			dump("add_expression' -> + add_expression'");
			skipSymbol();
			parseAddExpression();
			break;
		case Token.SUB:
			dump("add_expression' -> - add_expression'");
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
			dump("multiplicative_expression -> prefix_expression multiplicative_expression'");
			parsePrefixExpression();
			parseMulExpression_();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected prefix expression");
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
		case Token.LBRACE:
		case Token.RBRACKET:
		case Token.KW_THEN:
		case Token.KW_ELSE:
		case Token.COMMA:
		case Token.EOF:
			dump("multiplicative_expression' -> e");
			break;
		case Token.EQU:
		case Token.NEQ:
		case Token.GTH:
		case Token.LTH:
		case Token.GEQ:
		case Token.LEQ:
			break;
		case Token.ADD:
		case Token.SUB:
			dump("nevem todo");
			skipSymbol();
			parseAddExpression();
			break;
		case Token.MUL:
		case Token.DIV:
		case Token.MOD:
			dump("multiplicative_expression' -> multiplicative_expression");
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
			dump("prefix_expression -> + prefix_expression");
			skipSymbol();
			parsePrefixExpression();
			break;
		case Token.SUB:
			dump("prefix_expression -> - prefix_expression");
			skipSymbol();
			parsePrefixExpression();
			break;
		case Token.NOT:
			dump("prefix_expression -> ! prefix_expression");
			skipSymbol();
			parsePrefixExpression();
			break;
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			dump("prefix_expression -> postfix_expression");
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
			dump("postfix_expression -> atom_expression postfix_expression'");
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
		case Token.LBRACE:
		case Token.RBRACKET:
		case Token.KW_THEN:
		case Token.KW_ELSE:
		case Token.COMMA:
		case Token.EOF:
			dump("postfix_expression' -> e");
			break;
		case Token.EQU:
		case Token.NEQ:
		case Token.GTH:
		case Token.LTH:
		case Token.GEQ:
		case Token.LEQ:
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
			dump("postfix_expression' -> [ expression ]");
			skipSymbol();
			parseExpression();
			if (symbol.token != Token.RBRACKET)
				Report.error(previous.position, 
						"Syntax error, insert \"]\"");
			skipSymbol();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseAtomExpression() {
		switch (symbol.token) {
		case Token.LOG_CONST:
			dump("atom_expression -> log_const");
			skipSymbol();
			break;
		case Token.INT_CONST:
			dump("atom_expression -> int_const");
			skipSymbol();
			break;
		case Token.STR_CONST:
			dump("atom_expression -> str_const");
			skipSymbol();
			break;
		case Token.LBRACE:
			skipSymbol();
			parseAtomExprBrace();
			break;
		case Token.LPARENT:
			dump("atom_expression -> ( expressions )");
			skipSymbol();
			parseExpressions();
			break;
		case Token.IDENTIFIER:
			skipSymbol();
			if (symbol.token == Token.LPARENT) {
				dump("atom_expression -> identifier ( expressions )");
				skipSymbol();
				parseExpressions();
			}
			else
				dump("atom_expression -> identifier");
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
	}
	
	private void parseAtomExprBrace() {
		if (symbol.token == Token.KW_IF) {
			parseIf();
		}
		else if (symbol.token == Token.KW_WHILE) {			
			dump("atom_expression -> { while expression : expression }");
			parseWhileLoop();
		}
		else if (symbol.token == Token.KW_FOR) {
			dump("atom_expression -> { for identifier = expression, expression, expression : expression }");
			parseForLoop();
		}
		else {
			dump("atom_expression -> { expression = expression }");
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
		Report.error(symbol.position, "Syntax error, expected keyword \"while\"");
	}
	
	private void parseIf() {
		// TODO
		if (symbol.token == Token.KW_IF) {
			dump("atom_expression -> if_expression if_expression'");
			skipSymbol();
			parseExpression();
			if (symbol.token == Token.KW_THEN) {
				skipSymbol();
				parseExpression();
				parseIf_();
				return;
			}
			Report.error(symbol.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected keyword \"then\" after this token");
		}
	}
	
	private void parseIf_() {
		if (symbol.token == Token.KW_ELSE) {
			dump("if_expression' -> else expression }");
			skipSymbol();
			parseExpression();
		}
		
		if (symbol.token == Token.RBRACE) {
			dump("if_expression' -> }");
			return;
		}

		Report.error(symbol.position, 
				"Syntax error on token \"" + symbol.lexeme + "\", expected \"}\"");
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
