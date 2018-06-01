package de.ovgu.spldev.featurecopp.filesystem;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.internal.runners.statements.Fail;

public class FilesystemTest {
	/**
	 * 
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * 
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * 
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

	}

	/**
	 * 
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void test() {
		try {
			System.out.println("#### FILESYSTEMTEST ####");
			fail();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
