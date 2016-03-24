package compiler.synan;

import java.util.Arrays;
import java.util.Vector;

import compiler.Report;
import compiler.abstr.tree.*;
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
	public AbsTree parse() {
		if (symbol == null) Report.error("Error accessing LexAn");
		
		return parseSource();
	}

	/**
	 * Parse functions.
	 */
	private AbsTree parseSource() {
		dump("source -> definitions");
		AbsTree abstrTree = parseDefinitions();

		if (symbol.token != Token.EOF)
			Report.error(symbol.position, "Syntax error on token \"" + previous.lexeme + "\"");
		
		return abstrTree;
	}
	
	private AbsDefs parseDefinitions() {
		dump("definitions -> definition definitions'");
		AbsDef definition = parseDefinition();
		
		Vector<AbsDef> absDefs = parseDefinitions_();
		absDefs.add(0, definition);
		return new AbsDefs(absDefs.firstElement().position, absDefs);
	}

	private Vector<AbsDef> parseDefinitions_() {
		switch (symbol.token) {
		case Token.EOF:
			dump("definitions' -> $");
			
			return new Vector<>();
		case Token.RBRACE:
			dump("definitions' -> e");
			skipSymbol();
			
			return new Vector<>();
		case Token.SEMIC:
			dump("definitions' -> ; definitions");
			skipSymbol();
			
			AbsDef definition = parseDefinition();
			Vector<AbsDef> absDefs = parseDefinitions_();
			absDefs.add(0, definition);
			return absDefs;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected \";\" or \"}\" after this token");
		}
		return null;
	}
	
	private AbsDef parseDefinition() {
		AbsDef definition = null;
		
		switch (symbol.token) {
		case Token.KW_TYP:
			dump("definition -> type_definition");
			definition = parseTypeDefinition();
			break;
		case Token.KW_FUN:
			dump("definition -> function_definition");
			definition = parseFunDefinition();
			break;
		case Token.KW_VAR:
			dump("definition -> var_definition");
			definition = parseVarDefinition();
			break;
		default:
			if (symbol.token != Token.EOF)
				Report.error(symbol.position, 
						"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
			else
				Report.error(previous.position, 
						"Syntax error on token \"" + previous.lexeme + "\", delete this token");
		}
		
		return definition;
	}

	private AbsTypeDef parseTypeDefinition() {
		if (symbol.token == Token.KW_TYP) {
			if (next().token == Token.IDENTIFIER) {
				Symbol id = symbol;
				if (next().token == Token.COLON) {
					dump("type_definition -> typ identifier : type");
					skipSymbol();
					AbsType type = parseType();
					return new AbsTypeDef(id.position, id.lexeme, type);
				}
				Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", expected \":\" after this token");
			}
			Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", identifier expected");
		}
		Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", expected keyword \"type\"");
		
		return null;
	}

	private AbsFunDef parseFunDefinition() {
		if (symbol.token == Token.KW_TYP) {
			if (next().token == Token.IDENTIFIER) {
				Symbol id = symbol;
				if (next().token == Token.LPARENT) {
					dump("function_definition -> fun identifier ( parameters ) : type = expression");
					skipSymbol();
					Vector<AbsPar> params = parseParameters();
					if (symbol.token == Token.COLON) {
						skipSymbol();
						AbsType type = parseType();
						if (symbol.token == Token.ASSIGN) {
							skipSymbol();
							AbsExpr expr = parseExpression();
							return new AbsFunDef(id.position, id.lexeme, params, type, expr);
						}
						Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", exptected \"=\" after this token");
					}
					Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", \":\" exptected after this token");
				}
				Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", exptected \"(\" after this token");
			}
			Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", expected identifier after this token");
		}
		Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", expected keyword \"fun\"");
		
		return null;
	}

	private AbsVarDef parseVarDefinition() {
		if (symbol.token == Token.KW_VAR) {
			if (next().token == Token.IDENTIFIER) {
				Symbol id = symbol;
				if (next().token == Token.COLON) {
					dump("var_definition -> var identifier : type");
					skipSymbol();
					
					return new AbsVarDef(id.position, id.lexeme, parseType());
				}
				Report.error(previous.position, "Syntax error on token \""+ previous.lexeme + "\", expected \":\" after this token");
			}
			Report.error(previous.position, "Syntax error on token \""+ previous.lexeme + "\", expected identifier after this token");
		}
		Report.error(previous.position, "Syntax error on token \"" + previous.lexeme + "\", expected keyword \"var\"");
		
		return null;
	}
	
	private AbsType parseType() {
		Symbol s = symbol;
		
		switch (symbol.token) {
		case Token.IDENTIFIER:
			dump("type -> identifier");
			skipSymbol();
			
			return new AbsTypeName(s.position, s.lexeme);
		case Token.LOGICAL:
			dump("type -> logical");
			skipSymbol();
			
			return new AbsAtomType(s.position, AbsAtomType.LOG);
		case Token.INTEGER:
			dump("type -> integer");
			skipSymbol();
			
			return new AbsAtomType(s.position, AbsAtomType.INT);
		case Token.STRING:
			dump("type -> string");
			skipSymbol();

			return new AbsAtomType(s.position, AbsAtomType.STR);
		case Token.KW_ARR:
			dump("type -> arr [ int_const ] type");
			if (next().token == Token.LBRACKET) {
				if (next().token == Token.INT_CONST) {
					int len = Integer.parseInt(symbol.lexeme);
					if (next().token == Token.RBRACKET) {
						skipSymbol();
						AbsType type = parseType();
						return new AbsArrType(s.position, len, type);
					}
					Report.error(symbol.position, "Syntax error, insert \"]\" to complete Dimensions");
				}
				Report.error(symbol.position, "Syntax error, variable must provide array dimension expression");
			}
			Report.error(symbol.position, "Syntax error, insert \"[\"");
		default:
			Report.error(symbol.position, "Syntax error on token \"" + symbol.lexeme + "\", expected \"variable type\"");
		}
		
		return null;
	}
	
	private Vector<AbsPar> parseParameters() {
		dump("parameters -> parameter parameters'");
		
		AbsPar paramater = parseParameter();
		Vector<AbsPar> params = new Vector<>();
		params.add(paramater);
		params.addAll(parseParameters_());
		
		return null;
	}
	
	private Vector<AbsPar> parseParameters_() {
		if (symbol.token == Token.COMMA) {
			dump("parameters' -> parameters");
			skipSymbol();
			
			AbsPar parameter = parseParameter();
			Vector<AbsPar> params = new Vector<>();
			params.add(parameter);
			params.addAll(parseParameters_());
			return params;
		}
		else if (symbol.token != Token.RPARENT)
			Report.error(symbol.position, 
					"Syntax error, insert \")\" to complete function declaration");
		
		dump("parameters' -> e");
		skipSymbol();
		
		return new Vector<>();
	}
	
	private AbsPar parseParameter() {
		if (symbol.token == Token.IDENTIFIER) {
			Symbol id = symbol;
			if (next().token == Token.COLON) {
				dump("parameter -> identifier : type");
				skipSymbol();
				
				return new AbsPar(id.position, id.lexeme, parseType());
			}
			Report.error(symbol.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected \":\" after this token");
		}
		Report.error(symbol.position, "Syntax error, expected paramater definition");
		
		return null;
	}
	
	private AbsExpr parseExpressions() {
		AbsExpr e = null; 
		Symbol s = symbol;
		
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
			e = parseExpression();
			
			Vector<AbsExpr> expressions = new Vector<>();
			expressions.add(e);
			expressions.addAll(parseExpressions_());
			
			return new AbsExprs(s.position, expressions);
		default:
			Report.error(symbol.position,
					"Syntax error on token \"" + previous.lexeme + "\", delete this token");
		}
		
		return null;
	}
	
	private Vector<AbsExpr> parseExpressions_() {
		switch (symbol.token) {
		case Token.COMMA:
			dump("expressions' -> , expression expression'");
			skipSymbol();
			
			AbsExpr e = parseExpression();
			
			Vector<AbsExpr> expressions = new Vector<>();
			expressions.add(e);
			expressions.addAll(parseExpressions_());
			
			return expressions;
		case Token.RPARENT:
			dump("expressions' -> e");
			skipSymbol();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected \",\" or \")\" to end expression");
		}
		return new Vector<>();
	}
	
	private AbsExpr parseExpression() {
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
			return parseExpression_(parseIorExpression());
		default:
			Report.error(symbol.position,
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		
		return null;
	}
	
	private AbsExpr parseExpression_(AbsExpr e) {
		switch (symbol.token) {
		case Token.LBRACE:
			if (next().token == Token.KW_WHERE) {
				dump("expression' ->  { WHERE definitions }");
				skipSymbol();
				
				return new AbsWhere(e.position, e, parseDefinitions());
			}
			Report.error(symbol.position,
					"Syntax error on token \"" + previous.lexeme + "\", expected keyword \"where\" after this token");
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
			return e;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		
		return null;
	}
	
	private AbsExpr parseIorExpression() {
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
			
			return parseIorExpression_(parseAndExpression());
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		
		return null;
	}
	
	private AbsExpr parseIorExpression_(AbsExpr e) {
		switch (symbol.token) {
		case Token.IOR:
			dump("logical_ior_expression' -> | log_ior_expression");
			skipSymbol();
			AbsExpr expr = parseAndExpression();
			return parseIorExpression_(new AbsBinExpr(e.position, AbsBinExpr.IOR, e, expr));
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
			return e;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		
		return null;
	}
	
	private AbsExpr parseAndExpression() {
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
			
			return parseAndExpression_(parseCmpExpression());
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		
		return null;
	}
	
	private AbsExpr parseAndExpression_(AbsExpr e) {
		switch (symbol.token) {
		case Token.AND:
			dump("logical_and_expression' -> & logical_and_expression");
			skipSymbol();
			
			AbsExpr expr = parseCmpExpression();
			return parseAndExpression_(new AbsBinExpr(e.position, AbsBinExpr.AND, e, expr));
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
			return e;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		
		return null;
	}

	private AbsExpr parseCmpExpression() {
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
			
			return parseCmpExpression_(parseAddExpression());
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		
		return null;
	}
	
	private AbsExpr parseCmpExpression_(AbsExpr e) {
		AbsExpr expr = null;
		
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
			return e;
		case Token.EQU:
			dump("compare_expression' -> == compare_expression");
			skipSymbol();
			
			expr = parseAddExpression();
			return parseCmpExpression_(new AbsBinExpr(e.position, AbsBinExpr.EQU, e, expr));
		case Token.NEQ:
			dump("compare_expression' -> != compare_expression");
			skipSymbol();

			expr = parseAddExpression();
			return parseCmpExpression_(new AbsBinExpr(e.position, AbsBinExpr.NEQ, e, expr));
		case Token.GTH:
			dump("compare_expression' -> > compare_expression");
			skipSymbol();
			
			expr = parseAddExpression();
			return parseCmpExpression_(new AbsBinExpr(e.position, AbsBinExpr.GTH, e, expr));
		case Token.LTH:
			dump("compare_expression' -> < compare_expression");
			skipSymbol();
			
			expr = parseAddExpression();
			return parseCmpExpression_(new AbsBinExpr(e.position, AbsBinExpr.LTH, e, expr));
		case Token.GEQ:
			dump("compare_expression' -> >= compare_expression");
			skipSymbol();
			
			expr = parseAddExpression();
			return parseCmpExpression_(new AbsBinExpr(e.position, AbsBinExpr.GEQ, e, expr));
		case Token.LEQ:
			dump("compare_expression' -> <= compare_expression");
			skipSymbol();
			
			expr = parseAddExpression();
			return parseCmpExpression_(new AbsBinExpr(e.position, AbsBinExpr.LEQ, e, expr));
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		
		return null;
	}

	private AbsExpr parseAddExpression() {
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
			
			return parseAddExpression_(parseMulExpression());
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		
		return null;
	}
	
	private AbsExpr parseAddExpression_(AbsExpr e) {
		AbsExpr expr = null;
		
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
			return e;
		case Token.EQU:
		case Token.NEQ:
		case Token.GTH:
		case Token.LTH:
		case Token.GEQ:
		case Token.LEQ:
//			skipSymbol();
//			parseAddExpression();
			// TODO
			return e;
		case Token.ADD:
			dump("add_expression' -> + add_expression");
			skipSymbol();
			
			expr = parseMulExpression();
			return parseAddExpression_(new AbsBinExpr(e.position, AbsBinExpr.ADD, e, expr));
		case Token.SUB:
			dump("add_expression' -> - add_expression");
			skipSymbol();
			
			expr = parseMulExpression();
			return parseAddExpression_(new AbsBinExpr(e.position, AbsBinExpr.ADD, e, expr));
		default:
			Report.error(symbol.position, 
					"Syntax error on parseAddExpression_");
		}
		
		return null;
	}
	
	private AbsExpr parseMulExpression() {
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
			
			return parseMulExpression_(parsePrefixExpression());
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + previous.lexeme + "\", expected prefix expression");
		}
		
		return null;
	}

	private AbsExpr parseMulExpression_(AbsExpr e) {
		AbsExpr expr = null;
		int oper = -1;
		
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
			return e;
		case Token.EQU:
		case Token.NEQ:
		case Token.GTH:
		case Token.LTH:
		case Token.GEQ:
		case Token.LEQ:
			return e;
		case Token.ADD:
		case Token.SUB:
			// TODO
			dump("nevem todo");
			skipSymbol();
			parseAddExpression();
			break;
		case Token.MUL:
			oper = AbsBinExpr.MUL;
		case Token.DIV:
			oper = AbsBinExpr.DIV;
		case Token.MOD:
			oper = AbsBinExpr.MOD;
			dump("multiplicative_expression' -> multiplicative_expression");
			skipSymbol();
			
			expr = parsePrefixExpression();
			return parseMulExpression_(new AbsBinExpr(e.position, oper, e, expr));
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		
		return null;
	}
	
	private AbsExpr parsePrefixExpression() {
		AbsExpr e = null;
		
		switch (symbol.token) {
		case Token.ADD:
			dump("prefix_expression -> + prefix_expression");
			skipSymbol();
			
			e = parsePrefixExpression();
			return new AbsUnExpr(e.position, AbsUnExpr.ADD, e);
		case Token.SUB:
			dump("prefix_expression -> - prefix_expression");
			skipSymbol();
			
			e = parsePrefixExpression();
			return new AbsUnExpr(e.position, AbsUnExpr.SUB, e);
		case Token.NOT:
			dump("prefix_expression -> ! prefix_expression");
			skipSymbol();

			e = parsePrefixExpression();
			return new AbsUnExpr(e.position, AbsUnExpr.NOT, e);
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			dump("prefix_expression -> postfix_expression");
			return parsePostfixExpression();
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		
		return null;
	}
	
	private AbsExpr parsePostfixExpression() {
		switch (symbol.token) {
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			dump("postfix_expression -> atom_expression postfix_expression'");
			
			return parsePostfixExpression_(parseAtomExpression());
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		
		return null;
	}
	
	private AbsExpr parsePostfixExpression_(AbsExpr e) {
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
			return e;
		case Token.EQU:
		case Token.NEQ:
		case Token.GTH:
		case Token.LTH:
		case Token.GEQ:
		case Token.LEQ:
			return e;
			break;
		case Token.ADD:
		case Token.SUB:
			// TODO
			skipSymbol();
			parseAddExpression();
			break;
		case Token.MUL:
		case Token.DIV:
		case Token.MOD:
			// TODO
			skipSymbol();
			parseMulExpression();
			break;
		case Token.LBRACKET:
			dump("postfix_expression' -> [ expression ] postfix_expression'");
			skipSymbol();
			parseExpression();
			if (symbol.token != Token.RBRACKET)
				Report.error(previous.position, "Syntax error, insert \"]\" to complete expression");
			skipSymbol();
			parsePostfixExpression_();
			break;
		default:
			Report.error(symbol.position, 
					"Syntax error on token \"" + symbol.lexeme + "\", delete this token");
		}
		
		return null;
	}
	
	private AbsExpr parseAtomExpression() {
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
	
	private AbsExpr parseAtomExprBrace() {
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
	
	private AbsExpr parseForLoop() {
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
	
	private AbsExpr parseWhileLoop() {
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
	
	private AbsExpr parseIf() {
		if (symbol.token == Token.KW_IF) {
			dump("atom_expression -> if_expression if_expression'");
			dump("if_expression -> if epression then expression");
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
	
	private AbsExpr parseIf_() {
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
