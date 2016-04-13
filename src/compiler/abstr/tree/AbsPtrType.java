package compiler.abstr.tree;

import compiler.Position;
import compiler.abstr.Visitor;


/**
 * Opis kazalca.
 * 
 * @author Toni
 */
public class AbsPtrType extends AbsType {
	
	/** Tip kazalca. */
	public final AbsType type;

	/**
	 * Ustvari nov opis tabele.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param length
	 *            Dolzina tabele.
	 * @param type
	 *            Tip elementa tabele.
	 */
	public AbsPtrType(Position pos, AbsType pointerToType) {
		super(pos);
		this.type = pointerToType;
	}
	
	@Override public void accept(Visitor visitor) { visitor.visit(this); }
	
}

