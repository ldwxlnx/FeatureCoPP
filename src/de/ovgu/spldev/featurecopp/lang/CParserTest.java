package de.ovgu.spldev.featurecopp.lang;

import static org.junit.Assert.*;

import org.eclipse.core.runtime.CoreException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CParserTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetStats() {
		boolean debug = true;
		try {
			// the empty word
			{
				CParser cparser = new CParser(debug, "", null);
			}
			{
				CParser cparser = new CParser(debug, "int i = 1; i = i + 1;", null);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fail();
	}

}
