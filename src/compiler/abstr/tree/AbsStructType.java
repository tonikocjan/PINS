package compiler.abstr.tree;

import compiler.Position;
import compiler.abstr.Visitor;

public class AbsStructType extends AbsType {
	
	private AbsDefs definitions = null;
	
	public AbsDefs getDefinitions() { return definitions; }
	
	public AbsStructType(Position pos, AbsDefs definitions) {
		super(pos);
		this.definitions = definitions;
	}

	@Override public void accept(Visitor visitor) { visitor.visit(this); }

}
