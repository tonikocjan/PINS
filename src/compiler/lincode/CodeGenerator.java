package compiler.lincode;

import java.util.HashMap;
import java.util.LinkedList;

import compiler.frames.FrmFrame;
import compiler.frames.FrmLabel;
import compiler.imcode.ImcChunk;
import compiler.imcode.ImcCode;
import compiler.imcode.ImcCodeChunk;
import compiler.imcode.ImcDataChunk;
import compiler.interpreter.Interpreter;

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
	
	public static FrmFrame generateLinearCode() {
		return null;
	}

	public static ImcCodeChunk linearize(LinkedList<ImcChunk> chunks) {
		ImcCodeChunk mainFrame = null;
		int offset = 0;
		for (ImcChunk chnk : chunks) {
			if (chnk instanceof ImcCodeChunk) {
				ImcCodeChunk fn = (ImcCodeChunk) chnk;
				fn.lincode = fn.imcode.linear();
				if (fn.frame.label.name().equals("_main")) {
					mainFrame = fn;
					
				}
				CodeGenerator.insertCode(fn.frame.label, fn);
			}
			else {
				ImcDataChunk data = (ImcDataChunk) chnk;
				Interpreter.locations.put(data.label, offset);
				if (data.data != null)
					Interpreter.stM(offset, data.data);
				else
					Interpreter.stM(offset, 0);
					
				offset += data.size;
			}
		}
		return mainFrame;
	}
	
}
