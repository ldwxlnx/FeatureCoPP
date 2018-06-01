package de.ovgu.spldev.featurecopp.io;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import de.ovgu.spldev.featurecopp.config.Configuration;

public class NLPreservBufferedReader extends BufferedReader {
	public NLPreservBufferedReader(Reader in, int sz) {
		super(in, sz);
	}

	public NLPreservBufferedReader(Reader in) {
		super(in);
	}

	public String readLine() throws IOException {
		// first line already processed and hence line separator detected?
		if(lineSeparatorDetected) {
			String currLine = super.readLine();
			return currLine == null ? currLine : currLine + lineSeparator;
		}
		// analyze first line
		else {
			StringBuilder buffer = new StringBuilder();
			while (super.read(character, 0, 1) > 0) {
				buffer.append(character[0]);
				if (character[0] == '\r') {
					lineSeparatorDetected = true;
					// bookmark first read byte
					super.mark(1);
					// EOF? -> old mac os
					if (super.read(character, 1, 1) < 0) {
						lineSeparator = "\r";
						zeroizeCharacters();
						return buffer.toString();
					}

					// win?
					if (character[1] == '\n') {
						buffer.append(character[1]);
						lineSeparator = "\r\n";
					}
					// old mac os
					else {
						lineSeparator = "\r";
						// first char of new line already consumed, hence reverse lookup
						super.reset();
					}
					zeroizeCharacters();
					return buffer.toString();

				}
				// *nix (incl. modern apple)
				else if (character[0] == '\n') {
					lineSeparator = "\n";
					lineSeparatorDetected = true;
					return buffer.toString();
				}
			}
			return null;
		}
	}

	private void zeroizeCharacters() {
		character[0] = 0;
		character[1] = 0;
	}

	private char[] character = new char[2];
	private String lineSeparator = Configuration.LINE_SEPARATOR;
	private boolean lineSeparatorDetected;
}
