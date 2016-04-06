package renderer;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class Main {

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: java -jar DOTgenerator.jar inputFile outputFile");
			System.exit(1);
		}
		
		try {
			new DOT(args[0], new PrintStream(args[1]));
		} catch (FileNotFoundException e) {
			System.err.println("Exception! File not found");
		}
	}

}
