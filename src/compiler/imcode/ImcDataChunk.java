package compiler.imcode;

import compiler.*;
import compiler.frames.*;

/**
 * Fragment podatkov.
 * 
 * @author sliva
 */
public class ImcDataChunk extends ImcChunk {

	/** Naslov spremenljivke v pomnilniku.  */
	public FrmLabel label;

	/** Velikost spremenljivke v pomnilniku.  */
	public int size;
	
	/**  Inicializacija spremenljivke v labeli */
	public String data = null;

	/**
	 * Ustvari novfragment podatkov.
	 * 
	 * @param label Labela podatka.
	 * @param size Velikost podatka.
	 */
	public ImcDataChunk(FrmLabel label, int size) {
		this.label = label;
		this.size = size;
	}

	@Override
	public void dump() {
		String tmp =  data == null ? "" : " .BYTE: " + data;
		Report.dump(0, "DATA CHUNK: label=" + label.name() + " size=" + size + tmp);
	}

}
