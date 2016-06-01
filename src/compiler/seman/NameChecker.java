package compiler.seman;

import java.util.Vector;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.frames.FrmDesc;
import compiler.frames.FrmFrame;
import compiler.frames.FrmLabel;
import compiler.imcode.ImcDesc;
import compiler.interpreter.Interpreter;
import compiler.seman.type.SemAtomType;
import compiler.seman.type.SemFunType;
import compiler.seman.type.SemPtrType;
import compiler.seman.type.SemType;

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
	private AbsFunDef main = null;
	
	public AbsFunDef getMain() {
		return main;
	}

	public NameChecker() {
		try {
			{
				Vector<AbsPar> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				parTypes.add(new SemAtomType(SemAtomType.INT));
				
				pars.add(new AbsPar(null, "x", new AbsAtomType(null,
						AbsAtomType.INT)));
				
				AbsFunDef putInt = new AbsFunDef(null, "putInt", pars,
						new AbsAtomType(null, AbsAtomType.INT), new AbsExpr(
								null) {
							@Override
							public void accept(Visitor visitor) {
							}
						});
				SymbTable.ins("putInt", putInt);
				SymbDesc.setType(putInt, new SemFunType(parTypes, new SemAtomType(SemAtomType.INT)));
				
				FrmFrame frame = new FrmFrame(putInt, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel("putInt");
				FrmDesc.setFrame(putInt, frame);
			}
			{
				Vector<AbsPar> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				parTypes.add(new SemPtrType(new SemAtomType(SemAtomType.INT)));
				pars.add(new AbsPar(null, "x", new AbsAtomType(null,
						AbsAtomType.INT)));
				AbsFunDef putInt = new AbsFunDef(null, "getInt", pars,
						new AbsAtomType(null, AbsAtomType.INT), new AbsExpr(
								null) {
							@Override
							public void accept(Visitor visitor) {
							}
						});
				SymbTable.ins("getInt", putInt);
				SymbDesc.setType(putInt, new SemFunType(parTypes, new SemAtomType(SemAtomType.INT)));
				
				FrmFrame frame = new FrmFrame(putInt, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel("getInt");
				FrmDesc.setFrame(putInt, frame);
			}
			{
				Vector<AbsPar> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				parTypes.add(new SemAtomType(SemAtomType.STR));
				pars.add(new AbsPar(null, "x", new AbsAtomType(null,
						AbsAtomType.STR)));
				AbsFunDef putInt = new AbsFunDef(null, "putString", pars,
						new AbsAtomType(null, AbsAtomType.INT), new AbsExpr(
								null) {
							@Override
							public void accept(Visitor visitor) {
							}
						});
				SymbTable.ins("putString", putInt);
				SymbDesc.setType(putInt, new SemFunType(parTypes, new SemAtomType(SemAtomType.INT)));
				
				FrmFrame frame = new FrmFrame(putInt, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel("putString");
				FrmDesc.setFrame(putInt, frame);
			}
			{
				Vector<AbsPar> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				parTypes.add(new SemPtrType(new SemAtomType(SemAtomType.STR)));
				pars.add(new AbsPar(null, "x", new AbsAtomType(null,
						AbsAtomType.STR)));
				AbsFunDef putInt = new AbsFunDef(null, "getString", pars,
						new AbsAtomType(null, AbsAtomType.STR), new AbsExpr(
								null) {
							@Override
							public void accept(Visitor visitor) {
							}
						});
				SymbTable.ins("getString", putInt);
				SymbDesc.setType(putInt, new SemFunType(parTypes, new SemAtomType(SemAtomType.STR)));
				
				FrmFrame frame = new FrmFrame(putInt, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel("getString");
				FrmDesc.setFrame(putInt, frame);
			}
		} catch (Exception e) {
		}
	}

	@Override
	public void visit(AbsArrType acceptor) {
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsPtrType acceptor) {
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsStructType acceptor) {
		TraversalState tmp = currentState;
		SymbTable.newScope();
		acceptor.getDefinitions().accept(this);
		SymbTable.oldScope();
		currentState = tmp;
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

		if (acceptor.oper != AbsBinExpr.DOT)
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
					+ acceptor.name + "\" is undefined");

		SymbDesc.setNameDef(acceptor, definition);

		for (int arg = 0; arg < acceptor.numArgs(); arg++)
			acceptor.arg(arg).accept(this);
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		if (acceptor.name.equals("main")) main = acceptor;
		
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
			Report.error(acceptor.position, "Type \"" + acceptor.name
					+ "\" is undefined");

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

	@Override
	public void visit(AbsImportDef acceptor) {
		// TODO Auto-generated method stub
		
	}

}
