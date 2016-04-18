package compiler.synan;

import java.util.Vector;

import compiler.Position;
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
		if (symbol == null)
			Report.error("Error accessing LexAn");

		return parseSource();
	}

	/**
	 * Parse functions.
	 */
	private AbsTree parseSource() {
		dump("source -> definitions");
		AbsTree abstrTree = parseDefinitions();

		if (symbol.token != Token.EOF)
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\"");

		return abstrTree;
	}

	private AbsDefs parseDefinitions() {
		dump("definitions -> definition definitions'");
		AbsDef definition = parseDefinition();

		Vector<AbsDef> absDefs = parseDefinitions_();
		absDefs.add(0, definition);
		return new AbsDefs(new Position(absDefs.firstElement().position,
				absDefs.lastElement().position), absDefs);
	}

	private Vector<AbsDef> parseDefinitions_() {
		switch (symbol.token) {
		case Token.EOF:
			dump("definitions' -> $");

			return new Vector<>();
		case Token.RBRACE:
			dump("definitions' -> e");
			skip();

			return new Vector<>();
		case Token.SEMIC:
			dump("definitions' -> ; definitions");
			skip();

			AbsDef definition = parseDefinition();
			Vector<AbsDef> absDefs = parseDefinitions_();
			absDefs.add(0, definition);
			return absDefs;
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme
					+ "\", expected \";\" or \"}\" after this token");
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
				Report.error(symbol.position, "Syntax error on token \""
						+ symbol.lexeme + "\", delete this token");
			else
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme + "\", delete this token");
		}

		return definition;
	}

	private AbsTypeDef parseTypeDefinition() {
		Position startPos = symbol.position;
		if (symbol.token == Token.KW_TYP) {
			skip(new Symbol(Token.IDENTIFIER, "identifier", null));

			Symbol id = symbol;
			skip(new Symbol(Token.COLON, ":", null));
			skip();
			
			AbsType type = null;
			
			if (symbol.token == Token.KW_STRUCT) {
				dump("type_definition -> typ identifier : struct { definitions }");
				skip(new Symbol(Token.LBRACE, "{", null));
				skip();
				
				AbsDefs definitions = parseDefinitions();
				
				type = new AbsStructType(
						id.lexeme,
						new Position(startPos, definitions.position), 
						definitions);
			}
			else {
				dump("type_definition -> typ identifier : type");
				type = parseType();
			}
			return new AbsTypeDef(new Position(startPos, type.position),
					id.lexeme, type);
		}

		Report.error(previous.position, "Syntax error on token \""
				+ previous.lexeme + "\", expected keyword \"typ\"");
		return null;
	}

	private AbsFunDef parseFunDefinition() {
		Position startPos = symbol.position;
		if (symbol.token == Token.KW_FUN) {
			Symbol id = skip(new Symbol(Token.IDENTIFIER, "identifier", null));

			skip(new Symbol(Token.LPARENT, "(", null));
			skip();
			dump("function_definition -> fun identifier ( parameters ) : type = expression");

			Vector<AbsPar> params = parseParameters();
			skip();

			AbsType type = null;

			if (symbol.token == Token.KW_PTR) {
				Position pos = symbol.position;
				skip();

				AbsType t = parseType();
				type = new AbsPtrType(new Position(pos, t.position), t);
			} else
				type = parseType();

			if (symbol.token != Token.ASSIGN)
				Report.error(symbol.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected \"=\" after this token");
			skip();

			AbsExpr expr = parseExpression();
			return new AbsFunDef(new Position(startPos, expr.position),
					id.lexeme, params, type, expr);
		}
		Report.error(previous.position, "Syntax error on token \""
				+ previous.lexeme + "\", expected keyword \"fun\"");

		return null;
	}

	private AbsVarDef parseVarDefinition() {
		Position startPos = symbol.position;
		if (symbol.token == Token.KW_VAR) {
			Symbol id = skip(new Symbol(Token.IDENTIFIER, "identifier", null));

			skip(new Symbol(Token.COLON, ":", null));
			skip();

			if (symbol.token == Token.KW_PTR) {
				dump("var_definition -> var identifier : ptr ptr'");
				Position pos = symbol.position;

				skip();
				AbsType type = parsePointer();
				return new AbsVarDef(new Position(startPos, type.position),
						id.lexeme, new AbsPtrType(new Position(pos,
								type.position), type));
			} else {
				dump("var_definition -> var identifier : type");

				AbsType type = parseType();
				return new AbsVarDef(new Position(startPos, type.position),
						id.lexeme, type);
			}
		}
		Report.error(previous.position, "Syntax error on token \""
				+ previous.lexeme + "\", expected keyword \"var\"");

		return null;
	}
	
	private AbsType parsePointer() {
		if (symbol.token == Token.KW_PTR) {
			Position pos = symbol.position;
			skip();
			return new AbsPtrType(pos, parsePointer());
		}
		return parseType();
	}

	private AbsType parseType() {
		Symbol s = symbol;

		switch (symbol.token) {
		case Token.IDENTIFIER:
			dump("type -> identifier");
			skip();

			return new AbsTypeName(s.position, s.lexeme);
		case Token.LOGICAL:
			dump("type -> logical");
			skip();

			return new AbsAtomType(s.position, AbsAtomType.LOG);
		case Token.INTEGER:
			dump("type -> integer");
			skip();

			return new AbsAtomType(s.position, AbsAtomType.INT);
		case Token.STRING:
			dump("type -> string");
			skip();

			return new AbsAtomType(s.position, AbsAtomType.STR);
		case Token.KW_ARR:
			dump("type -> arr [ int_const ] type");
			if (skip().token == Token.LBRACKET) {
				if (skip().token == Token.INT_CONST) {
					int len = Integer.parseInt(symbol.lexeme);
					if (skip().token == Token.RBRACKET) {
						skip();
						AbsType type = parseType();
						return new AbsArrType(new Position(s.position,
								type.position), len, type);
					}
					Report.error(symbol.position,
							"Syntax error, insert \"]\" to complete Dimensions");
				}
				Report.error(symbol.position,
						"Syntax error, variable must provide array dimension expression");
			}
			Report.error(symbol.position, "Syntax error, insert \"[\"");
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", expected \"variable type\"");
		}

		return null;
	}

	private Vector<AbsPar> parseParameters() {
		dump("parameters -> parameter parameters'");

		AbsPar paramater = parseParameter();
		Vector<AbsPar> params = new Vector<>();
		params.add(paramater);
		params.addAll(parseParameters_());

		return params;
	}

	private Vector<AbsPar> parseParameters_() {
		if (symbol.token == Token.COMMA) {
			dump("parameters' -> parameters");
			skip();

			AbsPar parameter = parseParameter();
			Vector<AbsPar> params = new Vector<>();
			params.add(parameter);
			params.addAll(parseParameters_());
			return params;
		} else if (symbol.token != Token.RPARENT)
			Report.error(symbol.position,
					"Syntax error, insert \")\" to complete function declaration");

		dump("parameters' -> e");
		skip(new Symbol(Token.COLON, ":", null));

		return new Vector<>();
	}

	private AbsPar parseParameter() {
		if (symbol.token == Token.IDENTIFIER) {
			Symbol id = symbol;

			skip(new Symbol(Token.COLON, ":", null));
			skip();

			if (symbol.token == Token.KW_PTR) {
				dump("parameter -> identifier : ptr type");

				skip();
				Position pos = symbol.position;

				AbsType type = parsePointer();
				return new AbsPar(new Position(id.position, type.position),
						id.lexeme, new AbsPtrType(new Position(pos,
								type.position), type));
			}

			dump("parameter -> identifier : type");

			AbsType type = parseType();
			return new AbsPar(new Position(id.position, type.position),
					id.lexeme, type);
		}
		Report.error(symbol.position,
				"Syntax error, expected paramater definition");

		return null;
	}

	private Vector<AbsExpr> parseExpressions() {
		AbsExpr e = null;

		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
		case Token.AND:
		case Token.MUL:
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

			return expressions;
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private Vector<AbsExpr> parseExpressions_() {
		switch (symbol.token) {
		case Token.COMMA:
			dump("expressions' -> , expression expression'");
			skip();

			AbsExpr e = parseExpression();

			Vector<AbsExpr> expressions = new Vector<>();
			expressions.add(e);
			expressions.addAll(parseExpressions_());

			return expressions;
		case Token.RPARENT:
			dump("expressions' -> e");
			skip();
			break;
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme
					+ "\", expected \",\" or \")\" to end expression");
		}
		return new Vector<>();
	}

	private AbsExpr parseExpression() {
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
			// AND and MUL are for pointers
		case Token.AND:
		case Token.MUL:
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			dump("expression -> logical_ior_expression");
			return parseExpression_(parseIorExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseExpression_(AbsExpr e) {
		switch (symbol.token) {
		case Token.LBRACE:
			skip(new Symbol(Token.KW_WHERE, "where", null));
			dump("expression' ->  { WHERE definitions }");
			skip();

			return new AbsWhere(e.position, e, parseDefinitions());
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
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseIorExpression() {
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
		// AND and MUL are for pointers
		case Token.AND:
		case Token.MUL:
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			dump("logical_ior_expression -> logical_and_expression logical_ior_expression'");

			return parseIorExpression_(parseAndExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseIorExpression_(AbsExpr e) {
		switch (symbol.token) {
		case Token.IOR:
			dump("logical_ior_expression' -> | log_ior_expression");
			skip();
			AbsExpr expr = parseAndExpression();
			return parseIorExpression_(new AbsBinExpr(new Position(e.position,
					expr.position), AbsBinExpr.IOR, e, expr));
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
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseAndExpression() {
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
		// AND and MUL are for pointers
		case Token.AND:
		case Token.MUL:
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			dump("logical_and_expression -> logical_compare_expression logical_and_expression'");

			return parseAndExpression_(parseCmpExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseAndExpression_(AbsExpr e) {
		switch (symbol.token) {
		case Token.AND:
			dump("logical_and_expression' -> & logical_and_expression");
			skip();

			AbsExpr expr = parseCmpExpression();
			return parseAndExpression_(new AbsBinExpr(new Position(e.position,
					expr.position), AbsBinExpr.AND, e, expr));
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
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseCmpExpression() {
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
		// AND and MUL are for pointers
		case Token.AND:
		case Token.MUL:
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			dump("compare_expression -> add_expression compare_expression'");

			return parseCmpExpression_(parseAddExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseCmpExpression_(AbsExpr e) {
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
			dump("compare_expression' -> e");
			return e;
		case Token.EQU:
			dump("compare_expression' -> == compare_expression");
			skip();

			expr = parseAddExpression();
			oper = AbsBinExpr.EQU;
			break;
		case Token.NEQ:
			dump("compare_expression' -> != compare_expression");
			skip();

			expr = parseAddExpression();
			oper = AbsBinExpr.NEQ;
			break;
		case Token.GTH:
			dump("compare_expression' -> > compare_expression");
			skip();

			expr = parseAddExpression();
			oper = AbsBinExpr.GTH;
			break;
		case Token.LTH:
			dump("compare_expression' -> < compare_expression");
			skip();

			expr = parseAddExpression();
			oper = AbsBinExpr.LTH;
			break;
		case Token.GEQ:
			dump("compare_expression' -> >= compare_expression");
			skip();

			expr = parseAddExpression();
			oper = AbsBinExpr.GEQ;
			break;
		case Token.LEQ:
			dump("compare_expression' -> <= compare_expression");
			skip();

			expr = parseAddExpression();
			oper = AbsBinExpr.LEQ;
			break;
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return new AbsBinExpr(new Position(e.position, expr.position), oper, e,
				expr);
	}

	private AbsExpr parseAddExpression() {
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
		// AND and MUL are for pointers
		case Token.AND:
		case Token.MUL:
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			dump("add_expression -> multiplicative_expression add_expression'");

			return parseAddExpression_(parseMulExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
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
		case Token.EQU:
		case Token.NEQ:
		case Token.GTH:
		case Token.LTH:
		case Token.GEQ:
		case Token.LEQ:
			dump("add_expression' -> e");
			return e;
		case Token.ADD:
			dump("add_expression' -> multiplicative_expression add_expresison'");
			skip();

			expr = parseMulExpression();
			return parseAddExpression_(new AbsBinExpr(new Position(e.position,
					expr.position), AbsBinExpr.ADD, e, expr));
		case Token.SUB:
			dump("add_expression' -> - add_expression");
			skip();

			expr = parseMulExpression();
			return parseAddExpression_(new AbsBinExpr(e.position,
					AbsBinExpr.ADD, e, expr));
		default:
			Report.error(symbol.position, "Syntax error on parseAddExpression_");
		}

		return null;
	}

	private AbsExpr parseMulExpression() {
		switch (symbol.token) {
		case Token.ADD:
		case Token.SUB:
		case Token.NOT:
		// AND and MUL are for pointers
		case Token.AND:
		case Token.MUL:
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			dump("multiplicative_expression -> prefix_expression multiplicative_expression'");

			return parseMulExpression_(parsePrefixExpression());
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected prefix expression");
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
		case Token.EQU:
		case Token.NEQ:
		case Token.GTH:
		case Token.LTH:
		case Token.GEQ:
		case Token.LEQ:
		case Token.ADD:
		case Token.SUB:
			dump("multiplicative_expression' -> e");
			return e;
		case Token.MUL:
			oper = AbsBinExpr.MUL;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			skip();
			expr = parsePrefixExpression();
			break;
		case Token.DIV:
			oper = AbsBinExpr.DIV;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			skip();
			expr = parsePrefixExpression();
			break;
		case Token.MOD:
			oper = AbsBinExpr.MOD;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			skip();
			expr = parsePrefixExpression();
			break;
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return parseMulExpression_(new AbsBinExpr(new Position(e.position,
				expr.position), oper, e, expr));
	}

	private AbsExpr parsePrefixExpression() {
		AbsExpr e = null;
		Symbol op = symbol;

		switch (symbol.token) {
		case Token.ADD:
			dump("prefix_expression -> + prefix_expression");
			skip();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.position, e.position),
					AbsUnExpr.ADD, e);
		case Token.SUB:
			dump("prefix_expression -> - prefix_expression");
			skip();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.position, e.position),
					AbsUnExpr.SUB, e);
		case Token.NOT:
			dump("prefix_expression -> ! prefix_expression");
			skip();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.position, e.position),
					AbsUnExpr.NOT, e);
		case Token.AND:
			dump("prefix_expression -> & prefix_expression");
			skip();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.position, e.position),
					AbsUnExpr.MEM, e);
		case Token.MUL:
			dump("prefix_expression -> * prefix_expression");
			skip();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.position, e.position),
					AbsUnExpr.VAL, e);
		case Token.LOG_CONST:
		case Token.INT_CONST:
		case Token.STR_CONST:
		case Token.LBRACE:
		case Token.LPARENT:
		case Token.IDENTIFIER:
			dump("prefix_expression -> postfix_expression");
			return parsePostfixExpression();
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
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
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
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
		case Token.EQU:
		case Token.NEQ:
		case Token.GTH:
		case Token.LTH:
		case Token.GEQ:
		case Token.LEQ:
		case Token.ADD:
		case Token.SUB:
		case Token.MUL:
		case Token.DIV:
		case Token.MOD:
			dump("postfix_expression' -> e");
			return e;
		case Token.LBRACKET:
			dump("postfix_expression' -> [ expression ] postfix_expression'");
			skip();
			AbsExpr expr = parseExpression();
			if (symbol.token != Token.RBRACKET)
				Report.error(previous.position,
						"Syntax error, insert \"]\" to complete expression");
			skip();
			return parsePostfixExpression_(new AbsBinExpr(new Position(
					e.position, expr.position), AbsBinExpr.ARR, e, expr));
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseAtomExpression() {
		Symbol current = symbol;

		switch (symbol.token) {
		case Token.LOG_CONST:
			dump("atom_expression -> log_const");
			skip();

			return new AbsAtomConst(current.position, AbsAtomConst.LOG,
					current.lexeme);
		case Token.INT_CONST:
			dump("atom_expression -> int_const");
			skip();

			return new AbsAtomConst(current.position, AbsAtomConst.INT,
					current.lexeme);
		case Token.STR_CONST:
			dump("atom_expression -> str_const");
			skip();

			return new AbsAtomConst(current.position, AbsAtomConst.STR,
					current.lexeme);
		case Token.LBRACE:
			skip();

			return parseAtomExprBrace();
		case Token.LPARENT:
			dump("atom_expression -> ( expressions )");
			skip();

			Vector<AbsExpr> exprs = parseExpressions();
			return new AbsExprs(new Position(exprs.firstElement().position,
					exprs.lastElement().position), exprs);
		case Token.IDENTIFIER:
			skip();
			if (symbol.token == Token.LPARENT) {
				dump("atom_expression -> identifier ( expressions )");
				skip();

				Vector<AbsExpr> absExprs = parseExpressions();
				return new AbsFunCall(new Position(current.position,
						absExprs.lastElement().position), current.lexeme,
						absExprs);
			} else if (symbol.token == Token.DOT) {
				dump("atom_expression -> identifier.identifier");
				
				AbsVarName expr1 = new AbsVarName(current.position, current.lexeme);
				skip(new Symbol(Token.IDENTIFIER, "identifier", null));
				
				AbsVarName expr2 = new AbsVarName(symbol.position, symbol.lexeme);
				skip();
				
				return new AbsBinExpr(
						new Position(expr1.position, expr2.position),
						AbsBinExpr.DOT,
						expr1,
						expr2);
			} else {
				dump("atom_expression -> identifier");
				return new AbsVarName(current.position, current.lexeme);
			}
		default:
			Report.error(symbol.position, "Syntax error on token \""
					+ symbol.lexeme + "\", delete this token");
		}

		return null;
	}

	private AbsExpr parseAtomExprBrace() {
		AbsExpr expr = null;

		if (symbol.token == Token.KW_IF) {
			dump("atom_expression -> if_expression if_expression'");

			expr = parseIf();
		} else if (symbol.token == Token.KW_WHILE) {
			dump("atom_expression -> { while expression : expression }");

			expr = parseWhileLoop();
		} else if (symbol.token == Token.KW_FOR) {
			dump("atom_expression -> { for identifier = expression, expression, expression : expression }");

			expr = parseForLoop();
		} else {
			dump("atom_expression -> { expression = expression }");

			AbsExpr e1 = parseExpression();
			if (symbol.token == Token.ASSIGN) {
				skip();
				AbsExpr e2 = parseExpression();

				expr = new AbsBinExpr(e1.position, AbsBinExpr.ASSIGN, e1, e2);
			} else {
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected \"=\" after this token");
			}
		}
		if (symbol.token != Token.RBRACE) {
			Report.error(previous.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected \"}\" after this token");
		}
		skip();
		return expr;
	}

	private AbsExpr parseForLoop() {
		if (symbol.token == Token.KW_FOR) {
			Position start = symbol.position;
			Symbol count = skip(new Symbol(Token.IDENTIFIER, "identifier", null));
			skip(new Symbol(Token.ASSIGN, "=", null));
			skip();
			AbsExpr e1 = parseExpression();
			if (symbol.token == Token.COMMA) {
				skip();
				AbsExpr e2 = parseExpression();
				if (symbol.token == Token.COMMA) {
					skip();
					AbsExpr e3 = parseExpression();
					if (symbol.token == Token.COLON) {
						skip();
						AbsExpr e4 = parseExpression();

						return new AbsFor(new Position(start, e4.position),
								new AbsVarName(count.position, count.lexeme),
								e1, e2, e3, e4);
					}
					Report.error(previous.position, "Syntax error on token \""
							+ previous.lexeme
							+ "\", expected \":\" after this token");
				}
				Report.error(previous.position, "Syntax error on token \""
						+ previous.lexeme
						+ "\", expected \",\" after this token");
			}
			Report.error(previous.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected \",\" after this token");
		}
		Report.error(symbol.position, "Syntax error, expected keyword \"for\"");

		return null;
	}

	private AbsExpr parseWhileLoop() {
		if (symbol.token == Token.KW_WHILE) {
			Position start = symbol.position;
			skip();
			AbsExpr e1 = parseExpression();
			if (symbol.token == Token.COLON) {
				skip();
				AbsExpr e2 = parseExpression();

				return new AbsWhile(new Position(start, e2.position), e1, e2);
			}
			Report.error(previous.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected \":\" after this token");
		}
		Report.error(previous.position,
				"Syntax error, expected keyword \"while\"");

		return null;
	}

	private AbsExpr parseIf() {
		if (symbol.token == Token.KW_IF) {
			dump("if_expression -> if epression then expression");

			Position start = symbol.position;
			skip();
			AbsExpr e1 = parseExpression();
			if (symbol.token == Token.KW_THEN) {
				skip();
				AbsExpr e2 = parseExpression();
				return parseIf_(new Position(start, e2.position), e1, e2);
			}
			Report.error(previous.position, "Syntax error on token \""
					+ previous.lexeme
					+ "\", expected keyword \"then\" after this token");
		}
		return null;
	}

	private AbsExpr parseIf_(Position start, AbsExpr e1, AbsExpr e2) {
		if (symbol.token == Token.KW_ELSE) {
			dump("if_expression' -> else expression }");
			skip();
			AbsExpr e3 = parseExpression();

			return new AbsIfThenElse(new Position(start, e3.position), e1, e2,
					e3);
		}

		if (symbol.token == Token.RBRACE) {
			dump("if_expression' -> }");
			return new AbsIfThen(new Position(start, e2.position), e1, e2);
		}

		Report.error(symbol.position, "Syntax error on token \""
				+ symbol.lexeme + "\", expected \"}\"");

		return null;
	}

	/**
	 * Get next symbol from lexan.
	 */
	private Symbol skip() {
		previous = symbol;
		symbol = lexAn.lexAn();
		return symbol;
	}

	/**
	 * Get next symbol from lexan.
	 * 
	 * @param expected
	 *            symbol which we are expecting
	 * @return next symbol
	 */
	private Symbol skip(Symbol expected) {
		if (skip().token != expected.token)
			Report.error(previous.position, "Syntax error on token \""
					+ previous.lexeme + "\", expected \"" + expected.lexeme
					+ "\" after this token");
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
