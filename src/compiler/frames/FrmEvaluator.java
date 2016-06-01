package compiler.frames;

import compiler.abstr.Visitor;
import compiler.abstr.tree.*;
import compiler.seman.SymbDesc;

public class FrmEvaluator implements Visitor {

	private int currentLevel = 1;
	private FrmFrame currentFrame = null;

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

	}

	@Override
	public void visit(AbsAtomType acceptor) {

	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		acceptor.expr1.accept(this);
		acceptor.expr2.accept(this);
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (int def = 0; def < acceptor.numDefs(); def++)
			acceptor.def(def).accept(this);
	}

	@Override
	public void visit(AbsExprs acceptor) {
		for (int i = 0; i < acceptor.numExprs(); i++)
			acceptor.expr(i).accept(this);
	}

	@Override
	public void visit(AbsFor acceptor) {
		acceptor.lo.accept(this);
		acceptor.hi.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		int parSize = 4;
		for (int i = 0; i < acceptor.numArgs(); i++)
			parSize += SymbDesc.getType(acceptor.arg(i)).size();

		currentFrame.sizeArgs = Math.max(currentFrame.sizeArgs, parSize);
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		FrmFrame frame = new FrmFrame(acceptor, currentLevel);
		FrmDesc.setFrame(acceptor, frame);

		FrmFrame tmp = currentFrame;
		currentFrame = frame;

		for (int i = 0; i < acceptor.numPars(); i++)
			acceptor.par(i).accept(this);

		currentLevel++;

		acceptor.expr.accept(this);

		currentFrame = tmp;
		currentLevel--;
	}

	@Override
	public void visit(AbsIfThen acceptor) {
		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);
	}

	@Override
	public void visit(AbsIfThenElse acceptor) {
		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);
		acceptor.elseBody.accept(this);
	}

	@Override
	public void visit(AbsPar acceptor) {
		FrmDesc.setAccess(acceptor, new FrmParAccess(acceptor, currentFrame));
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
		if (currentFrame == null)
			FrmDesc.setAccess(acceptor, new FrmVarAccess(acceptor));
		else
			FrmDesc.setAccess(acceptor,
					new FrmLocAccess(acceptor, currentFrame));
	}

	@Override
	public void visit(AbsVarName acceptor) {

	}

	@Override
	public void visit(AbsWhere acceptor) {
		acceptor.defs.accept(this);
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsWhile acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsImportDef acceptor) {
		// TODO Auto-generated method stub
		
	}

}
