package compiler.imcode;

import java.util.*;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.AbsArrType;
import compiler.abstr.tree.AbsAtomConst;
import compiler.abstr.tree.AbsAtomType;
import compiler.abstr.tree.AbsBinExpr;
import compiler.abstr.tree.AbsDef;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.AbsExpr;
import compiler.abstr.tree.AbsExprs;
import compiler.abstr.tree.AbsFor;
import compiler.abstr.tree.AbsFunCall;
import compiler.abstr.tree.AbsFunDef;
import compiler.abstr.tree.AbsIfThen;
import compiler.abstr.tree.AbsIfThenElse;
import compiler.abstr.tree.AbsPar;
import compiler.abstr.tree.AbsPtrType;
import compiler.abstr.tree.AbsStructType;
import compiler.abstr.tree.AbsTypeDef;
import compiler.abstr.tree.AbsTypeName;
import compiler.abstr.tree.AbsUnExpr;
import compiler.abstr.tree.AbsVarDef;
import compiler.abstr.tree.AbsVarName;
import compiler.abstr.tree.AbsWhere;
import compiler.abstr.tree.AbsWhile;
import compiler.frames.FrmAccess;
import compiler.frames.FrmDesc;
import compiler.frames.FrmFrame;
import compiler.frames.FrmLabel;
import compiler.frames.FrmLocAccess;
import compiler.frames.FrmParAccess;
import compiler.frames.FrmTemp;
import compiler.frames.FrmVarAccess;
import compiler.seman.SymbDesc;
import compiler.seman.type.SemArrType;
import compiler.seman.type.SemStructType;
import compiler.seman.type.SemType;

public class ImcCodeGen implements Visitor {

	public LinkedList<ImcChunk> chunks;
	private FrmFrame currentFrame = null;

	public ImcCodeGen() {
		chunks = new LinkedList<ImcChunk>();
	}

	@Override
	public void visit(AbsArrType acceptor) {

	}

	@Override
	public void visit(AbsPtrType acceptor) {

	}

	@Override
	public void visit(AbsStructType acceptor) {

	}

	@Override
	public void visit(AbsAtomConst acceptor) {
		if (acceptor.type == AbsAtomConst.INT)
			ImcDesc.setImcCode(acceptor,
					new ImcCONST(Integer.parseInt(acceptor.value)));
		else if (acceptor.type == AbsAtomConst.LOG)
			ImcDesc.setImcCode(acceptor,
					new ImcCONST(acceptor.value.equals("true") ? 1 : 0));
		else {
			FrmLabel l = FrmLabel.newLabel();
			ImcDataChunk str = new ImcDataChunk(l, 4);
			str.data = new String(acceptor.value + "\0");
			chunks.add(str);
			ImcDesc.setImcCode(acceptor, new ImcNAME(l));
		}
	}

	@Override
	public void visit(AbsAtomType acceptor) {

	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		acceptor.expr1.accept(this);
		acceptor.expr2.accept(this);

		ImcCode c1 = ImcDesc.getImcCode(acceptor.expr1);
		ImcCode c2 = ImcDesc.getImcCode(acceptor.expr2);

		ImcExpr e1 = null;
		ImcExpr e2 = null;

		if (c1 instanceof ImcEXP)
			e1 = ((ImcEXP) c1).expr;
		else
			e1 = (ImcExpr) c1;

		if (c2 instanceof ImcEXP)
			e2 = ((ImcEXP) c2).expr;
		else
			e2 = (ImcExpr) c2;

		ImcCode code = null;

		if (acceptor.oper >= 0 && acceptor.oper <= 11)
			code = new ImcBINOP(acceptor.oper, e1, e2);
		else if (acceptor.oper == AbsBinExpr.ASSIGN) {
			code = new ImcMOVE(e1, e2);
		} else if (acceptor.oper == AbsBinExpr.ARR) {
			// TODO zrihtej da bo zaznalo velikost spremenljivke
			// trenutno je hardcodano not 4
			int size = 4;
			e1 = ((ImcMEM) e1).expr;
			code = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, e1, new ImcBINOP(
					ImcBINOP.MUL, e2, new ImcCONST(size))));
		} else if (acceptor.oper == ImcBINOP.MOD) {
			ImcBINOP div = new ImcBINOP(ImcBINOP.DIV, e1, e2);
			ImcBINOP mul = new ImcBINOP(ImcBINOP.MUL, div, e2);
			ImcBINOP sub = new ImcBINOP(ImcBINOP.SUB, e1, mul);
			code = sub;
		} else if (acceptor.oper == AbsBinExpr.DOT) {
			SemType t = SymbDesc.getType(acceptor.expr1).actualType();
			SemStructType type = (SemStructType) t;
			String var = ((AbsVarName) acceptor.expr2).name;
			int offset = type.offsetOf(var);
			
			code = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, ((ImcMEM)e1).expr, new ImcCONST(offset)));
		}

		ImcDesc.setImcCode(acceptor, code);
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (int i = 0; i < acceptor.numDefs(); i++)
			acceptor.def(i).accept(this);
	}

	@Override
	public void visit(AbsExprs acceptor) {
		if (acceptor.numExprs() == 1) {
			acceptor.expr(0).accept(this);
			ImcDesc.setImcCode(acceptor, ImcDesc.getImcCode(acceptor.expr(0)));
			return;
		}

		ImcSEQ statements = new ImcSEQ();

		for (int i = 0; i < acceptor.numExprs() - 1; i++) {
			acceptor.expr(i).accept(this);
			ImcCode tmp = ImcDesc.getImcCode(acceptor.expr(i));

			if (tmp instanceof ImcStmt)
				statements.stmts.add((ImcStmt) tmp);
			else
				statements.stmts.add(new ImcEXP((ImcExpr) tmp));
		}
		acceptor.expr(acceptor.numExprs() - 1).accept(this);
		ImcCode last = ImcDesc
				.getImcCode(acceptor.expr(acceptor.numExprs() - 1));

		if (last instanceof ImcExpr)
			ImcDesc.setImcCode(acceptor,
					new ImcESEQ(statements, (ImcExpr) last));
		else {
			statements.stmts.add((ImcStmt) last);
			ImcDesc.setImcCode(acceptor, statements);
		}
	}

	@Override
	public void visit(AbsFor acceptor) {
		acceptor.count.accept(this);
		acceptor.lo.accept(this);
		acceptor.hi.accept(this);
		acceptor.step.accept(this);
		acceptor.body.accept(this);

		ImcCode bodyCode = ImcDesc.getImcCode(acceptor.body);
		ImcStmt body = null;

		if (bodyCode instanceof ImcStmt)
			body = (ImcStmt) bodyCode;
		else
			body = new ImcEXP((ImcExpr) bodyCode);

		ImcExpr step = (ImcExpr) ImcDesc.getImcCode(acceptor.step);
		ImcExpr hi = (ImcExpr) ImcDesc.getImcCode(acceptor.hi);
		ImcExpr lo = (ImcExpr) ImcDesc.getImcCode(acceptor.lo);
		ImcExpr count = (ImcExpr) ImcDesc.getImcCode(acceptor.count);
		FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel(), l3 = FrmLabel
				.newLabel();

		ImcSEQ statements = new ImcSEQ();
		statements.stmts.add(new ImcMOVE(count, lo));
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(new ImcCJUMP(
				new ImcBINOP(ImcBINOP.LTH, count, hi), l2, l3));
		statements.stmts.add(new ImcLABEL(l2));
		statements.stmts.add(body);
		statements.stmts.add(new ImcMOVE(count, new ImcBINOP(ImcBINOP.ADD,
				count, step)));
		statements.stmts.add(new ImcJUMP(l1));
		statements.stmts.add(new ImcLABEL(l3));

		ImcDesc.setImcCode(acceptor, statements);
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		for (int arg = 0; arg < acceptor.numArgs(); arg++)
			acceptor.arg(arg).accept(this);

		FrmFrame frame = FrmDesc.getFrame(SymbDesc.getNameDef(acceptor));
		ImcCALL fnCall = new ImcCALL(frame.label);

		int diff = currentFrame.level - frame.level;
		if (diff == 0)
			fnCall.args.add(new ImcCONST(0xFFAAFF));
		else {
			ImcExpr SL = new ImcTEMP(currentFrame.FP);
			if (diff > 0)
				for (int i = 0; i < diff + 1; i++)
					SL = new ImcMEM(SL);

			fnCall.args.add(SL);
		}
		for (int i = 0; i < acceptor.numArgs(); i++) {
			ImcExpr e = (ImcExpr) ImcDesc.getImcCode(acceptor.arg(i));
			fnCall.args.add(e);
		}

		ImcDesc.setImcCode(acceptor, fnCall);
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		FrmFrame frame = FrmDesc.getFrame(acceptor);
		FrmFrame tmpFr = currentFrame;
		currentFrame = frame;

		acceptor.expr.accept(this);
		ImcCode code = ImcDesc.getImcCode(acceptor.expr);

		ImcExpr rv = new ImcTEMP(new FrmTemp());
		ImcStmt fnCode = null;

		if (code instanceof ImcESEQ) {
			fnCode = ((ImcESEQ) code).stmt;
			code = ((ImcESEQ) code).expr;
		}

		ImcSEQ seq = new ImcSEQ();
		// add function code
		if (fnCode != null)
			seq.stmts.add(fnCode);
		// save result into temp
		seq.stmts.add(new ImcMOVE(rv, (ImcExpr) code));
		// move result from temp into RV
		seq.stmts.add(new ImcMOVE(new ImcTEMP(currentFrame.RV), rv));

		chunks.add(new ImcCodeChunk(frame, seq));
		currentFrame = tmpFr;
	}

	@Override
	public void visit(AbsIfThen acceptor) {
		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);

		ImcCode thenBody = ImcDesc.getImcCode(acceptor.thenBody);
		ImcStmt expr = null;

		if (thenBody instanceof ImcExpr)
			expr = new ImcEXP((ImcExpr) thenBody);
		else
			expr = (ImcStmt) thenBody;

		ImcExpr cond = (ImcExpr) ImcDesc.getImcCode(acceptor.cond);
		FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel();

		ImcSEQ statements = new ImcSEQ();
		statements.stmts.add(new ImcCJUMP(cond, l1, l2));
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(expr);
		statements.stmts.add(new ImcLABEL(l2));

		ImcDesc.setImcCode(acceptor, statements);
	}

	@Override
	public void visit(AbsIfThenElse acceptor) {
		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);
		acceptor.elseBody.accept(this);

		ImcCode e1 = ImcDesc.getImcCode(acceptor.thenBody);
		ImcCode e2 = ImcDesc.getImcCode(acceptor.elseBody);

		ImcExpr cond = (ImcExpr) ImcDesc.getImcCode(acceptor.cond);
		ImcStmt expr1 = (e1 instanceof ImcStmt) ? (ImcStmt) e1 : new ImcEXP(
				(ImcExpr) e1);
		ImcStmt expr2 = (e2 instanceof ImcStmt) ? (ImcStmt) e2 : new ImcEXP(
				(ImcExpr) e2);

		FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel(), l3 = FrmLabel
				.newLabel();

		ImcSEQ statements = new ImcSEQ();
		statements.stmts.add(new ImcCJUMP(cond, l1, l2));
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(expr1);
		statements.stmts.add(new ImcJUMP(l3));
		statements.stmts.add(new ImcLABEL(l2));
		statements.stmts.add(expr2);
		statements.stmts.add(new ImcLABEL(l3));

		ImcDesc.setImcCode(acceptor, statements);
	}

	@Override
	public void visit(AbsPar acceptor) {

	}

	@Override
	public void visit(AbsTypeDef acceptor) {

	}

	@Override
	public void visit(AbsTypeName acceptor) {

	}

	@Override
	public void visit(AbsUnExpr acceptor) {
		acceptor.expr.accept(this);

		ImcCode expr = ImcDesc.getImcCode(acceptor.expr);

		if (acceptor.oper == AbsUnExpr.SUB) {
			ImcDesc.setImcCode(acceptor, new ImcBINOP(ImcBINOP.SUB,
					new ImcCONST(0), (ImcExpr) expr));
		} 
		else if (acceptor.oper == AbsUnExpr.NOT) {
			AbsExpr e2 = new AbsAtomConst(null, AbsAtomConst.INT, "0");
			AbsExpr cond = new AbsBinExpr(null, AbsBinExpr.EQU, acceptor.expr,
					e2);
			AbsExpr thenBody = new AbsAtomConst(null, AbsAtomConst.LOG, "true");
			AbsExpr elseBody = new AbsAtomConst(null, AbsAtomConst.LOG, "false");
			AbsIfThenElse not = new AbsIfThenElse(null, cond, thenBody,
					elseBody);
			not.accept(this);

			ImcDesc.setImcCode(acceptor, ImcDesc.getImcCode(not));
		}
		else if (acceptor.oper == AbsUnExpr.MEM) {
			if (expr instanceof ImcStmt) Report.error(acceptor.position, "Error");
			ImcDesc.setImcCode(acceptor, ((ImcMEM) expr).expr);
		}
		else if (acceptor.oper == AbsUnExpr.VAL) {
			if (expr instanceof ImcStmt) Report.error(acceptor.position, "Error");
			ImcDesc.setImcCode(acceptor, new ImcMEM((ImcExpr) expr));
		}
		else
			ImcDesc.setImcCode(acceptor, expr);
	}

	@Override
	public void visit(AbsVarDef acceptor) {
		FrmAccess x = FrmDesc.getAccess(acceptor);
		SemType y = SymbDesc.getType(acceptor);

		if (x instanceof FrmVarAccess)
			chunks.add(new ImcDataChunk(((FrmVarAccess) x).label, y.size()));
	}

	@Override
	public void visit(AbsVarName acceptor) {
		FrmAccess access = FrmDesc.getAccess(SymbDesc.getNameDef(acceptor));
		ImcExpr expr = null;

		if (access instanceof FrmVarAccess)
			expr = new ImcMEM(new ImcNAME(((FrmVarAccess) access).label));
		else if (access instanceof FrmLocAccess) {
			FrmLocAccess loc = (FrmLocAccess) access;
			int diff = currentFrame.level - loc.frame.level;

			ImcExpr fp = new ImcTEMP(currentFrame.FP);
			for (int i = 0; i < diff; i++)
				fp = new ImcMEM(fp);

			expr = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, fp, new ImcCONST(
					loc.offset)));
		} else if (access instanceof FrmParAccess) {
			FrmParAccess loc = (FrmParAccess) access;
			int diff = currentFrame.level - loc.frame.level;

			ImcExpr fp = new ImcTEMP(currentFrame.FP);
			for (int i = 0; i < diff; i++)
				fp = new ImcMEM(fp);

			expr = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, fp, new ImcCONST(
					loc.offset)));
		}

		ImcDesc.setImcCode(acceptor, expr);
	}

	@Override
	public void visit(AbsWhere acceptor) {
		acceptor.defs.accept(this);
		acceptor.expr.accept(this);

		ImcDesc.setImcCode(acceptor, ImcDesc.getImcCode(acceptor.expr));
	}

	@Override
	public void visit(AbsWhile acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);

		FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel(), l3 = FrmLabel
				.newLabel();

		ImcSEQ statements = new ImcSEQ();
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(new ImcCJUMP((ImcExpr) ImcDesc
				.getImcCode(acceptor.cond), l2, l3));
		statements.stmts.add(new ImcLABEL(l2));
		statements.stmts.add((ImcStmt) ImcDesc.getImcCode(acceptor.body));
		statements.stmts.add(new ImcJUMP(l1));
		statements.stmts.add(new ImcLABEL(l3));

		ImcDesc.setImcCode(acceptor, statements);
	}

}
