package compiler;

import compiler.lincode.CodeGenerator;
import compiler.lexan.*;
import compiler.synan.*;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.seman.*;
import compiler.frames.*;
import compiler.imcode.*;
import compiler.interpreter.Interpreter;

/**
 * Osnovni razred prevajalnika, ki vodi izvajanje celotnega procesa prevajanja.
 * 
 * @author sliva
 */
public class Main {

	/** Ime izvorne datoteke. */
	private static String sourceFileName;

	/** Seznam vseh faz prevajalnika. */
	private static String allPhases = "(lexan|synan|ast|seman|frames|imcode|interpret)";

	/** Doloca zadnjo fazo prevajanja, ki se bo se izvedla. */
	private static String execPhase = "interpret";

	/** Doloca faze, v katerih se bodo izpisali vmesni rezultati. */
	private static String dumpPhases = "interpret";

	/**
	 * Metoda, ki izvede celotni proces prevajanja.
	 * 
	 * @param args
	 *            Parametri ukazne vrstice.
	 */
	public static void main(String[] args) {
		System.out.printf("This is PREV compiler, v0.1:\n");

		// Pregled ukazne vrstice.
		for (int argc = 0; argc < args.length; argc++) {
			if (args[argc].startsWith("--")) {
				// Stikalo v ukazni vrstici.
				if (args[argc].startsWith("--phase=")) {
					String phase = args[argc].substring("--phase=".length());
					if (phase.matches(allPhases))
						execPhase = phase;
					else
						Report.warning("Unknown exec phase '" + phase + "' ignored.");
					continue;
				}
				if (args[argc].startsWith("--dump=")) {
					String phases = args[argc].substring("--dump=".length());
					if (phases.matches(allPhases + "(," + allPhases + ")*"))
						dumpPhases = phases;
					else
						Report.warning("Illegal dump phases '" + phases + "' ignored.");
					continue;
				}
				if (args[argc].startsWith("--debug=")) {
					String debug = args[argc].substring("--debug=".length());
					Interpreter.debug = debug.equals("true"); 
					continue;
				}
				if (args[argc].startsWith("--stack_size=")) {
					String size = args[argc].substring("--stack_size=".length());
					try {
						Interpreter.STACK_SIZE = Integer.parseInt(size);
					}
					catch(Exception e) {
						Report.warning("Invalid stack_size parameter");
					}
					continue;
				}
				// Neznano stikalo.
				Report.warning("Unrecognized switch in the command line.");
			} else {
				// Ime izvorne datoteke.
				if (sourceFileName == null)
					sourceFileName = args[argc];
				else
					Report.warning("Source file name '" + sourceFileName + "' ignored.");
			}
		}
		if (sourceFileName == null)
			Report.error("Source file name not specified.");

		// Odpiranje datoteke z vmesnimi rezultati.
		if (dumpPhases != null) Report.openDumpFile(sourceFileName);
		
		Report.fileName = sourceFileName;

		// Izvajanje faz prevajanja.
		while (true) {
			// Leksikalna analiza.
			LexAn lexAn = new LexAn(sourceFileName, dumpPhases.contains("lexan"));
			if (execPhase.equals("lexan")) {
				while (lexAn.lexAn().token != Token.EOF) {
				}
				break;
			}
			// Sintaksna analiza.
			SynAn synAn = new SynAn(lexAn, dumpPhases.contains("synan"));
			AbsTree source = synAn.parse();
			if (execPhase.equals("synan")) break;
			// Abstraktna sintaksa.
			Abstr ast = new Abstr(dumpPhases.contains("ast"));
			ast.dump(source);
			if (execPhase.equals("ast")) break;
			// Semanticna analiza.
			SemAn semAn = new SemAn(dumpPhases.contains("seman"));
			NameChecker nc = new NameChecker();
			source.accept(nc);
			source.accept(new TypeChecker());
			AbsFunDef mainFunction = nc.getMain();
			semAn.dump(source);
			if (execPhase.equals("seman")) break;
			// Klicni zapisi.
			Frames frames = new Frames(dumpPhases.contains("frames"));
			source.accept(new FrmEvaluator());
			frames.dump(source);
			if (execPhase.equals("frames")) break;
			// Vmesna koda.
			ImCode imcode = new ImCode(dumpPhases.contains("imcode"));
			ImcCodeGen imcodegen = new ImcCodeGen();
			source.accept(imcodegen);
			imcode.dump(imcodegen.chunks);
			if (execPhase.equals("imcode")) break;
			// TODO premakni v CodeGenerator??
			// Izvajanje linearizirane vmesne kode
			ImcCodeChunk mainFrame = null;
			
			int offset = 0;
			for (ImcChunk chnk : imcodegen.chunks) {
				if (chnk instanceof ImcCodeChunk) {
					ImcCodeChunk fn = (ImcCodeChunk) chnk;
					fn.lincode = fn.imcode.linear();
					if (fn.frame.label.name().equals("_main")) {
						mainFrame = fn;
						
						if (mainFunction.numPars() > 1 || !mainFunction.par(0).name.equals("i"))
							Report.error(mainFunction.position, "Undefined reference to _main(i:integer)");
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
			if (mainFrame == null)
				Report.error("Undefined reference to _main(i:integer)");

			System.out.printf(":-) Done.\n");
			System.out.printf("----- Executing file %s -----\n", sourceFileName);
			
			imcode = new ImCode(dumpPhases.contains("interpret"));
			imcode.dump(imcodegen.chunks);
			
			Interpreter.stM(Interpreter.getFP() + 4, 0);
			new Interpreter(mainFrame.frame, mainFrame.imcode.linear());
			
			if (execPhase.equals("interpret")) break;
			
			// Neznana faza prevajanja.
			if (! execPhase.equals(""))
				Report.warning("Unknown compiler phase specified.");
		}

		// Zapiranje datoteke z vmesnimi rezultati.
		if (dumpPhases != null) Report.closeDumpFile();

		System.exit(0);
	}
}
