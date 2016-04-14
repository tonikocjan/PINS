package compiler.seman.type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import compiler.Report;

public class SemStructType extends SemType {

	private LinkedHashMap<String, SemType> members = new LinkedHashMap<>();

	/**
	 * Ustvari nov opis strukture.
	 * 
	 * @param type
	 *            Tip elementa tabele.
	 * @param size
	 *            Velikost tabele.
	 */
	public SemStructType(ArrayList<String> names, ArrayList<SemType> types) {
		if (names.size() != types.size())
			Report.error("Internal error :: compiler.seman.type.SemStructType: names size not equal types size");

		for (int i = 0; i < names.size(); i++)
			members.put(names.get(i), types.get(i));
	}

	@Override
	public boolean sameStructureAs(SemType type) {
		if (!(type instanceof SemStructType))
			return false;
		
		return members.equals(((SemStructType)type).members);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("STRUCT(");
		for (Map.Entry<String, SemType> entry : members.entrySet())
			sb.append(entry.getKey() + ":" + entry.getValue().toString() + ";");
		return sb.toString();
	}

}
