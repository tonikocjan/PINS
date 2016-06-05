package compiler.imcode;

import java.util.HashMap;

import compiler.abstr.tree.AbsTree;

public class ImcDesc {
	/**
	 * 
	 */
	public static HashMap<AbsTree, ImcCode> imcMap = new HashMap<>();
	
	/**
	 * 
	 * @param node
	 * @param imc
	 */
	public static void setImcCode(AbsTree node, ImcCode imc) {
		imcMap.put(node, imc);
	}
	
	/**
	 * 
	 * @param node
	 * @return
	 */
	public static ImcCode getImcCode(AbsTree node) {
		return imcMap.get(node);
	}
}
