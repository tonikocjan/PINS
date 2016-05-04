package compiler.seman.type;

public class SemPtrType extends SemType {
	/** Tip kamor kaže. */
	public final SemType type;
	
	public SemPtrType(SemType type) {
		this.type = type;
	}

	@Override
	public boolean sameStructureAs(SemType type) {
		if (!(type instanceof SemPtrType))
			return false;
		if (((SemPtrType)type).type.sameStructureAs(this.type))
			return true;
		return false;
	}
	
	@Override
	public String toString() {
		return "PTR(" + type.toString() + ")";
	}

	@Override
	public int size() {
		return 4;
	}

}
