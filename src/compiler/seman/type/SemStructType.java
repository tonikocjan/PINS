package compiler.seman.type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import compiler.Report;

public class SemStructType extends SemType {

	private final LinkedHashMap<String, SemType> members = new LinkedHashMap<>();
	private final String name;
	private final int size;

	/**
	 * Ustvari nov opis strukture.
	 * 
	 * @param name name of structure
	 * @param names List of names for each definition
	 * @param types List of types for each definition
	 */
	public SemStructType(String name, ArrayList<String> names, ArrayList<SemType> types) {
		if (names.size() != types.size())
			Report.error("Internal error :: compiler.seman.type.SemStructType: names size not equal types size");

		int size = 0;
		for (int i = 0; i < names.size(); i++) {
			members.put(names.get(i), types.get(i));
			size += types.get(i).size();
		}
		this.size = size;
		this.name = name;
	}
	
	public LinkedHashMap<String, SemType> getMembers() {
		return members;
	}
	
	public int offsetOf(String name) {
		int offset = 0;
		
		for (Map.Entry<String, SemType> entry : members.entrySet()) {
			if (name.equals(entry.getKey())) break;
			offset += entry.getValue().size();
		}
		
		return offset;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public boolean sameStructureAs(SemType type) {
		if (!(type instanceof SemStructType))
			return false;
		
		SemStructType type_ = (SemStructType) type;
		
		if (members.size() != type_.members.size())
			return false;
		
		List<SemType> this_ = new ArrayList<>(members.values());
		List<SemType> o_ = new ArrayList<>(type_.members.values());
		
		for (int i = 0; i < this_.size(); i++) {
			if (!this_.get(i).sameStructureAs(o_.get(i)))
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		sb.append("STRUCT(");
		for (Map.Entry<String, SemType> entry : members.entrySet()) {
			sb.append(entry.getKey() + ":" + entry.getValue().toString());
			if (++i < members.size()) sb.append(";");
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public int size() {
		return size;
	}

}
