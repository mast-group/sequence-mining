package sequencemining.eval;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.output.TeeOutputStream;

import com.google.common.collect.Table;

import sequencemining.main.InferenceAlgorithms.InferGreedy;
import sequencemining.main.SequenceMining;
import sequencemining.main.SequenceMiningCore;
import sequencemining.sequence.Sequence;
import sequencemining.transaction.TransactionGenerator;
import sequencemining.util.Logging;

public class SequenceScaling {

	/** Main Settings */
	private static final File dbFile = new File("/disk/data1/jfowkes/sequence.txt");
	private static final File saveDir = new File("/disk/data1/jfowkes/logs/");

	/** Set of mined itemsets to use for background */
	private static final String name = "SIGN-based";
	private static final File sequenceLog = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Logs/SIGN.log");

	/** Spark Settings */
	private static final long MAX_RUNTIME = 24 * 60; // 24hrs
	private static final int maxStructureSteps = 100_000;
	private static final int maxEMIterations = 100;

	public static void main(final String[] args) throws IOException, ClassNotFoundException {

		// Run
		scalingTransactions(64, new int[] { 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000 });
	}

	public static void scalingTransactions(final int noCores, final int[] trans)
			throws IOException, ClassNotFoundException {

		final double[] time = new double[trans.length];
		final DecimalFormat formatter = new DecimalFormat("0.0E0");

		// Save to file
		final FileOutputStream outFile = new FileOutputStream(saveDir + "/" + name + "_scaling_" + noCores + ".txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		// Read in previously mined sequences
		final Map<Sequence, Double> sequences = SequenceMiningCore.readISMSequences(sequenceLog);
		System.out.print("\n============= ACTUAL SEQUENCES =============\n");
		for (final Entry<Sequence, Double> entry : sequences.entrySet()) {
			System.out.print(String.format("%s\tprob: %1.5f %n", entry.getKey(), entry.getValue()));
		}
		System.out.println("\nNo sequences: " + sequences.size());
		System.out.println("No items: " + countNoItems(sequences.keySet()));

		// Read in associated sequence count distribution
		@SuppressWarnings("unchecked")
		final Table<Sequence, Integer, Double> countDist = (Table<Sequence, Integer, Double>) Logging
				.deserializeFrom(FilenameUtils.removeExtension(sequenceLog.getAbsolutePath()) + ".dist");

		transloop: for (int i = 0; i < trans.length; i++) {

			final int tran = trans[i];
			System.out.println("\n========= " + formatter.format(tran) + " Transactions");

			// Generate transaction database
			TransactionGenerator.generateTransactionDatabase(sequences, countDist, tran, dbFile);
			SequenceScaling.printTransactionDBStats(dbFile);

			// Mine sequences
			final File logFile = Logging.getLogFileName("IIM", true, saveDir, dbFile);
			final long startTime = System.currentTimeMillis();
			SequenceMining.mineSequences(dbFile, new InferGreedy(), maxStructureSteps, maxEMIterations, logFile, false);

			final long endTime = System.currentTimeMillis();
			final double tim = (endTime - startTime) / (double) 1000;
			time[i] += tim;

			System.out.printf("Time (s): %.2f%n", tim);

			if (tim > MAX_RUNTIME * 60)
				break transloop;

		}

		// Print time
		System.out.println("\n========" + name + "========");
		System.out.println("Transactions:" + Arrays.toString(trans));
		System.out.println("Time: " + Arrays.toString(time));

		// and save to file
		out.close();
	}

	/**
	 * Count the number of items in the sequences (sequences need not be
	 * independent)
	 */
	public static int countNoItems(final Set<Sequence> sequences) {
		final Set<Integer> items = new HashSet<>();
		for (final Sequence sequence : sequences)
			items.addAll(sequence);
		return items.size();
	}

	/** Print useful statistics for the transaction database */
	public static void printTransactionDBStats(final File dbFile) throws IOException {

		int noTransactions = 0;
		double sparsity = 0;
		final Set<Integer> singletons = new HashSet<>();
		final LineIterator it = FileUtils.lineIterator(dbFile, "UTF-8");
		while (it.hasNext()) {
			final String[] items = it.nextLine().replace("-2", "").split(" -1 ");
			for (final String item : items)
				singletons.add(Integer.parseInt(item));
			sparsity += items.length;
			noTransactions++;
		}
		LineIterator.closeQuietly(it);

		System.out.println("\nDatabase: " + dbFile);
		System.out.println("Items: " + singletons.size());
		System.out.println("Transactions: " + noTransactions);
		System.out.println("Avg. items per transaction: " + sparsity / noTransactions + "\n");

	}

}
