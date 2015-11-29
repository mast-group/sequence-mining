package sequencemining.main;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.junit.Test;

import com.google.common.collect.Table;

import sequencemining.sequence.Sequence;

public class InitialProbabilitiesTest {

	@Test
	public void testScanDatabaseToDetermineInitialProbabilities() throws IOException {

		final File input = getTestFile("TOY.txt"); // database
		final Table<Sequence, Integer, Double> probs = SequenceMining
				.scanDatabaseToDetermineInitialProbabilities(input);
		System.out.println(SequenceMiningCore.probsToString(probs));

	}

	public File getTestFile(final String filename) throws UnsupportedEncodingException {
		final URL url = this.getClass().getClassLoader().getResource(filename);
		return new File(java.net.URLDecoder.decode(url.getPath(), "UTF-8"));
	}

}
