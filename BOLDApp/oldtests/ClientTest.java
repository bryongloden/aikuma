import au.edu.unimelb.aikuma.sync.Client;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import org.apache.commons.io.FileUtils;

import static org.junit.Assert.assertEquals;

public class ClientTest {
	Client client;

	@Before
	public void setUp() {
		client = new Client();
		client.setClientBaseDir("tests/bold/");
		client.setServerBaseDir("/part0/share/bold/");

	}

	@Test
	public void loginLogout() {
		assertEquals(true, client.login("192.168.1.1", "admin", "admin"));
		assertEquals(true, client.logout());

		assertEquals(false, client.login("192.168.1.1", "admin", "wrongpass"));
		assertEquals(false, client.logout());
	}

/*
	@Test
	public void pushPull() {

		// Make the directory
		File boldDir = new File("tests/bold");
		boldDir.mkdir();

		File boldExampleDir = new File("tests/bold_example");

		try {
			FileUtils.copyDirectory(boldExampleDir, boldDir);
		} catch (Exception e) {
		}

		assertEquals(true, client.login("192.168.1.1", "admin", "admin"));

		assertEquals(true, client.push());

		// Clear all the files in the directory
		for (File file : boldDir.listFiles()) {
			file.delete();
		}

		// Pull files from server
		assertEquals(true, client.pull());

		assertEquals(true, client.logout());

	}
*/

/*
	@Test
	public void recursivePushPull() {
		// Make the directory
		File boldDir = new File("tests/bold");
		boldDir.mkdir();

		File boldExampleDir = new File("tests/bold_example");

		try {
			FileUtils.copyDirectory(boldExampleDir, boldDir);
		} catch (Exception e) {
		}

		assertEquals(true, client.login("192.168.1.1", "admin", "admin"));
		assertEquals(true, client.push());
		// Clear all the files in the directory
		for (File file : boldDir.listFiles()) {
			file.delete();
		}
		System.out.println(" " + Arrays.asList(boldDir.listFiles()));
		assertEquals(true, client.pull());
		assertEquals(true, client.logout());
	}
*/

	@Test
	public void sync() {
		// Make the directory
		File boldDir = new File("tests/bold");
		boldDir.mkdir();

		File boldExampleDir = new File("tests/bold_example");

		assertEquals(true, client.login("192.168.1.1", "admin", "admin"));
		assertEquals(true, client.sync());
		assertEquals(true, client.logout());
	}
}