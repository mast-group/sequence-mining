package sequencemining.eval;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import ca.pfv.spmf.algorithms.sequentialpatterns.goKrimp.AlgoGoKrimp;
import ca.pfv.spmf.algorithms.sequentialpatterns.goKrimp.DataReader;
import sequencemining.sequence.Sequence;

public class StatisticalSequenceMining {

	public static void main(final String[] args) throws IOException {

		// Datasets
		final String[] datasets = new String[] { "libraries_filtered" };
		for (int i = 0; i < datasets.length; i++) {
			final File dbPath = new File(
					"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/Paper/" + datasets[i] + ".dat");

			// Run GoKRIMP
			final File saveFileGoKRIMP = new File(
					"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/GoKRIMP/" + datasets[i] + ".txt");
			mineGoKrimpSequences(dbPath, saveFileGoKRIMP);

			// Run SQS
			final File saveFileSQS = new File(
					"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/SQS/" + datasets[i] + ".txt");
			mineSQSSequences(dbPath, saveFileSQS, 1);
		}

	}

	public static LinkedHashMap<Sequence, Double> mineGoKrimpSequences(final File dataset, final File saveFile)
			throws IOException {

		final DataReader d = new DataReader();
		final AlgoGoKrimp g = d.readData_SPMF(dataset.getAbsolutePath(), "");
		// g.printData();
		g.setOutputFilePath(saveFile.getAbsolutePath());
		g.gokrimp();

		return readGoKRIMPSequences(saveFile);
	}

	public static LinkedHashMap<Sequence, Double> mineSQSSequences(final File dataset, final File saveFile,
			final int minUsage) throws IOException {

		// Convert to SQS Dataset format
		final File TMPDB = File.createTempFile("sqs-dataset", ".dat");
		convertDatasetSQSFormat(dataset, TMPDB);

		// Set MTV settings
		final String cmd[] = new String[5];
		cmd[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Packages/sqs/sqs.sh";
		cmd[1] = "-i " + TMPDB;
		cmd[2] = "-t " + minUsage; // default is 1
		cmd[3] = "-o " + saveFile;
		cmd[4] = "-m search"; // search - scan db directly, order - compress
								// given patterns
		// cmd[5] = "-p"; // patterns file (for order method)
		runScript(cmd);

		TMPDB.delete();

		return readSQSSequences(saveFile);
	}

	/** Convert dataset from SPMF format to SQS format */
	private static void convertDatasetSQSFormat(final File inputDB, final File outputDB) throws IOException {

		// Output DB
		final BufferedWriter db = new BufferedWriter(new FileWriter(outputDB));

		// for each line (transaction) until the end of file
		boolean newSeq = false;
		final LineIterator it = FileUtils.lineIterator(inputDB, "UTF-8");
		while (it.hasNext()) {

			final String line = it.nextLine();
			// if the line is a comment, is empty or is a
			// kind of metadata
			if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}

			// sequence separator
			if (newSeq)
				db.write("-1 ");

			// split the transaction into items
			final String[] lineSplited = line.split(" ");

			for (int i = 0; i < lineSplited.length; i++) {
				if (lineSplited[i].equals("-1")) { // end of item

				} else if (lineSplited[i].equals("-2")) { // end of sequence
					newSeq = true;
				} else { // extract the value for an item
					db.write(lineSplited[i] + " ");
				}
			}

		}
		db.newLine();
		db.close();

		// close the input file
		LineIterator.closeQuietly(it);

	}

	/** Read in GOKRIMP sequences (sorted by compression benefit) */
	public static LinkedHashMap<Sequence, Double> readGoKRIMPSequences(final File output) throws IOException {
		final LinkedHashMap<Sequence, Double> sequences = new LinkedHashMap<>();

		final LineIterator it = FileUtils.lineIterator(output);
		while (it.hasNext()) {
			final String line = it.nextLine();
			if (!line.trim().isEmpty()) {
				final String[] splitLine = line.split("#SUP:");
				final String[] items = splitLine[0].trim().split(" ");
				final Sequence seq = new Sequence();
				for (final String item : items)
					seq.add(Integer.parseInt(item.trim()));
				final double compressionBenefit = Double.parseDouble(splitLine[1].trim());
				sequences.put(seq, compressionBenefit);
			}
		}

		return sequences;
	}

	/** Read in SQS itemsets (sorted by worth) */
	public static LinkedHashMap<Sequence, Double> readSQSSequences(final File output) throws IOException {
		final LinkedHashMap<Sequence, Double> sequences = new LinkedHashMap<>();

		final LineIterator it = FileUtils.lineIterator(output);
		while (it.hasNext()) {
			final String line = it.nextLine();
			if (!line.trim().isEmpty()) {
				final String[] splitLine = line.split("  ");
				final String[] items = splitLine[0].split(" ");
				final Sequence seq = new Sequence();
				for (final String item : items)
					seq.add(Integer.parseInt(item));
				final double worth = Double.parseDouble(splitLine[1].split(" ")[1]);
				sequences.put(seq, worth);
			}
		}

		return sequences;
	}

	/** Run shell script with command line arguments in working directory */
	public static void runScript(final String cmd[]) {

		try {
			final ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.inheritIO();
			final Process process = pb.start();
			process.waitFor();
			process.destroy();
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

}
