package compiler.abstr.tree;

import compiler.Position;
import compiler.abstr.Visitor;

public class AbsStructType extends AbsType {
	
	private final AbsDefs definitions;
	private final String name;
	
	public AbsDefs getDefinitions() { return definitions; }
	
	public AbsStructType(String name, Position pos, AbsDefs definitions) {
		super(pos);
		this.definitions = definitions;
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	@Override public void accept(Visitor visitor) { visitor.visit(this); }

}
