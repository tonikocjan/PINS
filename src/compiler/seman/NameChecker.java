package compiler.seman;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.*;

/**
 * Preverjanje in razresevanje imen (razen imen komponent).
 * 
 * @author sliva
 * @implementation Toni Kocjan
 */
public class NameChecker implements Visitor {

	private enum TraversalState {
		ETS_types, ETS_prototypes, ETS_functions
	}

	private TraversalState currentState;

	@Override
	public void visit(AbsArrType acceptor) {
		acceptor.type.accept(this);
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
		for (TraversalState state : TraversalState.values()) {
			currentState = state;
			for (int def = 0; def < acceptor.numDefs(); def++)
				acceptor.def(def).accept(this);
		}
	}

	@Override
	public void visit(AbsExprs acceptor) {
		for (int expr = 0; expr < acceptor.numExprs(); expr++)
			acceptor.expr(expr).accept(this);
	}

	@Override
	public void visit(AbsFor acceptor) {
		acceptor.count.accept(this);
		acceptor.lo.accept(this);
		acceptor.hi.accept(this);
		acceptor.step.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		if (currentState != TraversalState.ETS_functions)
			return;

		AbsDef definition = SymbTable.fnd(acceptor.name);

		if (definition == null)
			Report.error(acceptor.position, "Error, function \""
					+ acceptor.name + "\" undefined");

		SymbDesc.setNameDef(acceptor, definition);

		for (int arg = 0; arg < acceptor.numArgs(); arg++)
			acceptor.arg(arg).accept(this);
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		if (currentState == TraversalState.ETS_prototypes) {
			try {
				SymbTable.ins(acceptor.name, acceptor);
			} catch (SemIllegalInsertException e) {
				Report.error(acceptor.position, "Duplicate method \""
						+ acceptor.name + "\"");
			}
		}

		else if (currentState == TraversalState.ETS_functions) {
			SymbTable.newScope();

			for (int par = 0; par < acceptor.numPars(); par++)
				acceptor.par(par).accept(this);
			acceptor.type.accept(this);
			acceptor.expr.accept(this);

			SymbTable.oldScope();
		}
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
		try {
			SymbTable.ins(acceptor.name, acceptor);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Duplicate parameter \""
					+ acceptor.name + "\"");
		}
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsTypeDef acceptor) {
		if (currentState == TraversalState.ETS_types) {
			try {
				SymbTable.ins(acceptor.name, acceptor);
			} catch (SemIllegalInsertException e) {
				Report.error(acceptor.position, "Type definition \""
						+ acceptor.name + "\" already exists");
			}
		} else if (currentState == TraversalState.ETS_prototypes) {
			acceptor.type.accept(this);
		}
	}

	@Override
	public void visit(AbsTypeName acceptor) {
		AbsDef definition = SymbTable.fnd(acceptor.name);
		
		if (definition == null)
			Report.error(acceptor.position, 
					"Type \"" + acceptor.name + "\" is undefined");

		SymbDesc.setNameDef(acceptor, definition);
	}

	@Override
	public void visit(AbsUnExpr acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsVarDef acceptor) {
		if (currentState == TraversalState.ETS_prototypes) {
			try {
				SymbTable.ins(acceptor.name, acceptor);
				acceptor.type.accept(this);
			} catch (SemIllegalInsertException e) {
				Report.error(acceptor.position, "Duplicate variable \""
						+ acceptor.name + "\"");
			}
		}
	}

	@Override
	public void visit(AbsVarName acceptor) {
		if (currentState != TraversalState.ETS_functions)
			return;

		AbsDef definition = SymbTable.fnd(acceptor.name);
		if (definition == null)
			Report.error(acceptor.position, "Error, variable \""
					+ acceptor.name + "\" undefined");

		SymbDesc.setNameDef(acceptor, definition);
	}

	@Override
	public void visit(AbsWhere acceptor) {
		SymbTable.newScope();

		acceptor.defs.accept(this);
		acceptor.expr.accept(this);

		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsWhile acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);
	}

}
