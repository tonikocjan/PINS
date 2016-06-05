package compiler.abstr.tree;

import java.util.HashSet;

import compiler.Position;
import compiler.abstr.Visitor;

public class AbsImportDef extends AbsDef {
	
	/** Ime vključujoče datoteke. */
	public final String fileName;
	
	/** Definicije znotraj importa  */
	public AbsDefs imports;
	
	/** Katere definicije želimo importirat (empty => importaj vse) */
	public final HashSet<String> definitions = new HashSet<>();

	public AbsImportDef(Position pos, String fn) {
		super(pos);
		fileName = fn;
		imports = null;
	}

	@Override
	public void accept(Visitor visitor) { visitor.visit(this); }
}
