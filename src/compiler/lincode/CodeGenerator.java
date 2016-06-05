package compiler.lincode;

import java.util.HashMap;

import compiler.frames.FrmFrame;
import compiler.frames.FrmLabel;
import compiler.imcode.ImcCode;
import compiler.imcode.ImcCodeChunk;

public class CodeGenerator {
	
	private static HashMap<FrmLabel, ImcCodeChunk> dict = new HashMap<>();

	public static FrmFrame framesByFrmLabel(FrmLabel label) {
		return dict.get(label).frame;
	}
	
	public static ImcCode codesByFrmLabel(FrmLabel label) {
		return dict.get(label).lincode;
	}
	
	public static void insertCode(FrmLabel label, ImcCodeChunk code) {
		dict.put(label, code);
	}
	
}
