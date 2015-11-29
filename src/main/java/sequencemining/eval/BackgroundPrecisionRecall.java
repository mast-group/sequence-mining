package sequencemining.eval;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.TeeOutputStream;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import sequencemining.main.InferenceAlgorithms.InferGreedy;
import sequencemining.main.SequenceMining;
import sequencemining.main.SequenceMiningCore;
import sequencemining.sequence.Sequence;
import sequencemining.transaction.TransactionGenerator;
import sequencemining.util.Logging;

public class BackgroundPrecisionRecall {

	/** Main Settings */
	private static final File dbFile = new File("/disk/data1/jfowkes/sequence.txt");
	private static final File saveDir = new File("/disk/data1/jfowkes/logs/");

	/** FSM Issues to incorporate */
	private static final String name = "Background";
	private static final int noIterations = 5_000;

	/** Previously mined Sequences to use for background distribution */
	private static final File sequenceLog = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Logs/ISM-SIGN-11.11.2015-13:41:54.log");
	private static final int noTransactions = 10_000;

	/** Sequence Miner Settings */
	private static final int maxStructureSteps = 100_000;
	private static final double minSup = 0.05;

	public static void main(final String[] args) throws IOException, ClassNotFoundException {

		// Read in background distribution
		final Map<Sequence, Double> backgroundSequences = SequenceMiningCore.readISMSequences(sequenceLog);

		// Read in associated sequence count distribution
		@SuppressWarnings("unchecked")
		final Table<Sequence, Integer, Double> countDist = (Table<Sequence, Integer, Double>) Logging
				.deserializeFrom(FilenameUtils.removeExtension(sequenceLog.getAbsolutePath()) + ".dist");

		final HashMap<Sequence, Double> sequences = TransactionGenerator
				.generateTransactionDatabase(backgroundSequences, countDist, noTransactions, dbFile);
		System.out.print("\n============= ACTUAL SEQUENCES =============\n");
		for (final Entry<Sequence, Double> entry : sequences.entrySet()) {
			System.out.print(String.format("%s\tprob: %1.5f %n", entry.getKey(), entry.getValue()));
		}
		System.out.println("\nNo sequences: " + sequences.size());
		SequenceScaling.printTransactionDBStats(dbFile);

		precisionRecall(sequences, "GoKrimp");
		precisionRecall(sequences, "SQS");
		precisionRecall(sequences, "FSM");
		precisionRecall(sequences, "ISM");

	}

	public static void precisionRecall(final Map<Sequence, Double> itemsets, final String algorithm)
			throws IOException {

		// Set up logging
		final FileOutputStream outFile = new FileOutputStream(saveDir + "/" + algorithm + "_" + name + "_pr.txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		// Mine sequences
		Set<Sequence> minedSequences = null;
		final File logFile = Logging.getLogFileName(algorithm, true, saveDir, dbFile);
		final long startTime = System.currentTimeMillis();
		if (algorithm.equals("FSM")) {
			FrequentSequenceMining.mineClosedFrequentSequencesBIDE(dbFile.getAbsolutePath(), logFile.getAbsolutePath(),
					minSup);
			minedSequences = FrequentSequenceMining.readFrequentSequences(logFile).keySet();
		} else if (algorithm.equals("ISM")) {
			final Map<Sequence, Double> minedIntSeqs = SequenceMining.mineSequences(dbFile, new InferGreedy(),
					maxStructureSteps, noIterations, logFile, false);
			final Ordering<Sequence> comparator = Ordering.natural().reverse()
					.onResultOf(Functions.forMap(minedIntSeqs)).compound(Ordering.usingToString());
			minedSequences = ImmutableSortedMap.copyOf(minedIntSeqs, comparator).keySet();
		} else if (algorithm.equals("GoKrimp")) {
			minedSequences = StatisticalSequenceMining.mineGoKrimpSequences(dbFile, logFile).keySet();
		} else if (algorithm.equals("SQS")) {
			minedSequences = StatisticalSequenceMining.mineSQSSequences(dbFile, logFile, 1).keySet();
		} else
			throw new RuntimeException("Incorrect algorithm name.");
		final long endTime = System.currentTimeMillis();
		final double time = (endTime - startTime) / (double) 1000;

		// Calculate sorted precision and recall
		final int len = minedSequences.size();
		System.out.println("No. mined sequences: " + len);
		final double[] precision = new double[len];
		final double[] recall = new double[len];
		for (int k = 1; k <= minedSequences.size(); k++) {

			final Set<Sequence> topKMined = Sets.newHashSet();
			for (final Sequence seq : minedSequences) {
				topKMined.add(seq);
				if (topKMined.size() == k)
					break;
			}

			final double noInBoth = Sets.intersection(itemsets.keySet(), topKMined).size();
			final double pr = noInBoth / (double) topKMined.size();
			final double rec = noInBoth / (double) itemsets.size();
			precision[k - 1] = pr;
			recall[k - 1] = rec;
		}

		// Output precision and recall
		System.out.println("\n======== " + name + " ========");
		System.out.println("Time: " + time);
		System.out.println("Precision (all): " + Arrays.toString(precision));
		System.out.println("Recall (all): " + Arrays.toString(recall));

	}

}
