package renderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Scanner;

public class DOT {

	Scanner file = null;
	int count = 0;
	
	HashMap<Integer, Pair> tree = new HashMap<>();
	int id = 0;

	public DOT(String file, PrintStream outStream) throws FileNotFoundException {
		this.file = new Scanner(new File(file));
		this.generateDOT(outStream);
	}

	private void generateDOT(PrintStream outStream) {
		int intend;
		
		/**
		 * Step one:
		 *  - save all tree nodes into map
		 */
		while (file.hasNextLine()) {		
			String line = file.nextLine();
			
			intend = getIntend(line);
			line = line.substring(intend);
			String tmp = line;
			line = line.substring(0, line.indexOf(' ') + 1);
			tmp = tmp.substring(tmp.indexOf(':') + 1, tmp.length());
			tmp = tmp.substring(tmp.indexOf(':') + 1, tmp.length());
			tmp = tmp.substring(tmp.indexOf(':') + 1, tmp.length());
			line = line + "\n" + tmp;
			
			Pair p = new Pair(line, intend);
			tree.put(id, p);
			
			id++;
		}
		
		/**
		 * Step two:
		 * 	 - create directed graph
		 */
		outStream.println("digraph abstrSynTree {");
		
		/**
		 * Step three:
		 *   - for each node print it's ID and create label
		 */
		for (java.util.Map.Entry<Integer, Pair> p : tree.entrySet()) {
			outStream.println("\t" + p.getKey() + "[label=\"" + p.getValue().name + "\"]");
		}

		/**
		 * Step four:
		 *   - for each node n:
		 *     iterate map (m) starting from current node,
		 *     if m.intend - n.intend == 2 then m is child of n
		 *     if m.intend <= n.intend then there are no children of n left
		 */
		int skip = 1;
		for (java.util.Map.Entry<Integer, Pair> p : tree.entrySet()) {
			outStream.print("\t" + p.getKey() + " -> {");
			
			intend = p.getValue().intend;
			
			int skiped = 0;
			for (java.util.Map.Entry<Integer, Pair> j : tree.entrySet()) {
				if (skiped++ < skip) continue;
				
				if (j.getValue().intend - intend == 2)
					outStream.print(" " + j.getKey());
				else if (j.getValue().intend <= intend) break;
			}
			
			skip++;
			outStream.println("};");
		}
		
		/**
		 * Step five:
		 *   - complete the graph
		 */
		outStream.println("}");
	}
	
	private int getIntend(String line) {
		for (int i = 0; i < line.length(); i++)
			if (line.charAt(i) != ' ') return i;
		return 0;
	}
	
	private class Pair {
		public String name;
		public int intend;
		public Pair(String name, int intend) {
			this.name = name;
			this.intend = intend;
		}
	}
}
