package compiler.imcode;

import java.util.*;

import compiler.abstr.*;
import compiler.abstr.tree.AbsArrType;
import compiler.abstr.tree.AbsAtomConst;
import compiler.abstr.tree.AbsAtomType;
import compiler.abstr.tree.AbsBinExpr;
import compiler.abstr.tree.AbsDef;
import compiler.abstr.tree.AbsDefs;
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
import compiler.frames.FrmVarAccess;
import compiler.seman.SymbDesc;
import compiler.seman.type.SemArrType;
import compiler.seman.type.SemType;

public class ImcCodeGen implements Visitor {

	public LinkedList<ImcChunk> chunks;

	private ImcSEQ statements = null;
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
			statements.stmts.add(new ImcEXP(new ImcCONST(Integer
					.parseInt(acceptor.value))));
	}

	@Override
	public void visit(AbsAtomType acceptor) {

	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		acceptor.expr1.accept(this);
		acceptor.expr2.accept(this);

		ImcExpr e2 = ((ImcEXP) statements.stmts.pollLast()).expr;
		ImcExpr e1 = ((ImcEXP) statements.stmts.pollLast()).expr;
		ImcExpr expr = null;

		if (acceptor.oper >= 8 && acceptor.oper <= 12)
			expr = new ImcBINOP(acceptor.oper, e1, e2);
		else if (acceptor.oper >= 0 && acceptor.oper <= 7)
			expr = new ImcBINOP(acceptor.oper, e1, e2);
		else if (acceptor.oper == AbsBinExpr.ASSIGN) {
			statements.stmts.add(new ImcMOVE(e1, e2));
			return;
		} else if (acceptor.oper == AbsBinExpr.ARR) {
			int size = ((SemArrType) SymbDesc.getType(SymbDesc
					.getNameDef(acceptor.expr1))).type.size();
			statements.stmts.add(new ImcEXP(new ImcMEM(new ImcBINOP(
					ImcBINOP.ADD, e1, new ImcBINOP(ImcBINOP.MUL, e2,
							new ImcCONST(size))))));
			return;
		}

		statements.stmts.add(new ImcEXP(expr));
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (int i = 0; i < acceptor.numDefs(); i++)
			acceptor.def(i).accept(this);
	}

	@Override
	public void visit(AbsExprs acceptor) {
		for (int i = 0; i < acceptor.numExprs(); i++)
			acceptor.expr(i).accept(this);
	}

	@Override
	public void visit(AbsFor acceptor) {
		ImcSEQ tmp = statements;
		statements = new ImcSEQ();

		acceptor.count.accept(this);
		acceptor.lo.accept(this);
		acceptor.hi.accept(this);
		acceptor.step.accept(this);
		acceptor.body.accept(this);

		ImcStmt body = statements.stmts.pollLast();
		ImcExpr step = ((ImcEXP) (statements.stmts.pollLast())).expr;
		ImcExpr hi = ((ImcEXP) (statements.stmts.pollLast())).expr;
		ImcExpr lo = ((ImcEXP) (statements.stmts.pollLast())).expr;
		ImcExpr count = ((ImcEXP) (statements.stmts.pollLast())).expr;
		
		FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel(), l3 = FrmLabel
				.newLabel();
		
		statements.stmts.add(new ImcMOVE(count, lo));
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(new ImcCJUMP(new ImcBINOP(ImcBINOP.LTH, count, hi), l2, l3));
		statements.stmts.add(new ImcLABEL(l2));
		statements.stmts.add(body);
		statements.stmts.add(new ImcMOVE(count, new ImcBINOP(ImcBINOP.ADD, count, step)));
		statements.stmts.add(new ImcJUMP(l1));
		statements.stmts.add(new ImcLABEL(l3));

		tmp.stmts.add(statements);
		statements = tmp;
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		ImcSEQ tmp = statements;
		statements = new ImcSEQ();

		for (int arg = 0; arg < acceptor.numArgs(); arg++)
			acceptor.arg(arg).accept(this);

		FrmFrame frame = FrmDesc.getFrame(SymbDesc.getNameDef(acceptor));
		ImcCALL fnCall = new ImcCALL(frame.label);

		fnCall.args.add(new ImcCONST(frame.level - currentFrame.level));
		for (int i = 0; i < statements.stmts.size(); i++) {
			ImcExpr e = ((ImcEXP) (statements.stmts.get(i))).expr;
			fnCall.args.add(e);
		}

		statements = tmp;
		statements.stmts.add(new ImcEXP(fnCall));
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		FrmFrame frame = FrmDesc.getFrame(acceptor);

		for (int par = 0; par < acceptor.numPars(); par++)
			acceptor.par(par).accept(this);

		ImcSEQ tmp = statements;
		FrmFrame tmpFr = currentFrame;

		currentFrame = FrmDesc.getFrame(acceptor);
		statements = new ImcSEQ();

		acceptor.expr.accept(this);

		chunks.add(new ImcCodeChunk(frame, statements));

		statements = tmp;
		currentFrame = tmpFr;
	}

	@Override
	public void visit(AbsIfThen acceptor) {
		ImcSEQ tmp = statements;
		statements = new ImcSEQ();

		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);

		ImcStmt expr = statements.stmts.pollLast();
		ImcExpr cond = ((ImcEXP) (statements.stmts.pollLast())).expr;
		FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel();

		statements.stmts.add(new ImcCJUMP(cond, l1, l2));
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(expr);
		statements.stmts.add(new ImcLABEL(l2));

		tmp.stmts.add(statements);
		statements = tmp;
	}

	@Override
	public void visit(AbsIfThenElse acceptor) {
		ImcSEQ tmp = statements;
		statements = new ImcSEQ();

		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);
		acceptor.elseBody.accept(this);

		ImcStmt expr2 = statements.stmts.pollLast();
		ImcStmt expr1 = statements.stmts.pollLast();
		ImcExpr cond = ((ImcEXP) (statements.stmts.pollLast())).expr;
		FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel(), l3 = FrmLabel
				.newLabel();

		statements.stmts.add(new ImcCJUMP(cond, l1, l2));
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(expr1);
		statements.stmts.add(new ImcJUMP(l3));
		statements.stmts.add(new ImcLABEL(l2));
		statements.stmts.add(expr2);
		statements.stmts.add(new ImcLABEL(l3));

		tmp.stmts.add(statements);
		statements = tmp;
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
			expr = new ImcNAME(((FrmVarAccess) access).label);
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

		statements.stmts.add(new ImcEXP(expr));
		// expressions.add(expr);
	}

	@Override
	public void visit(AbsWhere acceptor) {
		acceptor.defs.accept(this);
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsWhile acceptor) {
		ImcSEQ tmp = statements;
		statements = new ImcSEQ();

		acceptor.cond.accept(this);
		acceptor.body.accept(this);

		ImcStmt expr = statements.stmts.pollLast();
		ImcExpr cond = ((ImcEXP) (statements.stmts.pollLast())).expr;
		FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel(), l3 = FrmLabel
				.newLabel();
		
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(new ImcCJUMP(cond, l2, l3));
		statements.stmts.add(new ImcLABEL(l2));
		statements.stmts.add(expr);
		statements.stmts.add(new ImcJUMP(l1));
		statements.stmts.add(new ImcLABEL(l3));

		tmp.stmts.add(statements);
		statements = tmp;
	}

}
