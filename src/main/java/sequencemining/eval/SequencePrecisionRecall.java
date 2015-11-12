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

import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import sequencemining.main.InferenceAlgorithms.InferGreedy;
import sequencemining.main.SequenceMining;
import sequencemining.main.SequenceMiningCore;
import sequencemining.sequence.Sequence;
import sequencemining.transaction.TransactionGenerator;
import sequencemining.util.Logging;

public class SequencePrecisionRecall {

	/** Main Settings */
	private static final File dbFile = new File("/disk/data1/jfowkes/Sequence.txt");
	private static final File saveDir = new File("/disk/data1/jfowkes/logs/");

	/** FIM Issues to incorporate */
	private static final String name = "caviar";
	private static final int noIterations = 300;

	/** Previously mined Sequences to use for background distribution */
	private static final File sequenceLog = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Logs/ISM-SIGN-11.11.2015-13:41:54.log");
	private static final int noTransactions = 10_000;
	private static final int noSpecialSequences = 30;

	/** Sequence Miner Settings */
	private static final int maxStructureSteps = 100_000;
	private static final double minSup = 0.04;

	public static void main(final String[] args) throws IOException, ClassNotFoundException {

		// Read in background distribution
		final Map<Sequence, Double> backgroundSequences = new HashMap<>(
				SequenceMiningCore.readISMSequences(sequenceLog));

		// Read in associated sequence count distribution
		@SuppressWarnings("unchecked")
		final Table<Sequence, Integer, Double> countDist = (Table<Sequence, Integer, Double>) Logging
				.deserializeFrom(FilenameUtils.removeExtension(sequenceLog.getAbsolutePath()) + ".dist");

		// Set up transaction DB
		final HashMap<Sequence, Double> specialSequences = TransactionGenerator.generateExampleSequences(name,
				noSpecialSequences, 0);
		backgroundSequences.putAll(specialSequences);
		// Add special sequences to count distribution
		for (final Entry<Sequence, Double> entry : specialSequences.entrySet())
			countDist.put(entry.getKey(), 1, entry.getValue());

		// Generate transaction DB
		final HashMap<Sequence, Double> Sequences = TransactionGenerator
				.generateTransactionDatabase(backgroundSequences, countDist, noTransactions, dbFile);
		System.out.print("\n============= ACTUAL ITEMSETS =============\n");
		for (final Entry<Sequence, Double> entry : Sequences.entrySet()) {
			System.out.print(String.format("%s\tprob: %1.5f %n", entry.getKey(), entry.getValue()));
		}
		System.out.print("\n");
		System.out.println("No Sequences: " + Sequences.size());
		SequenceScaling.printTransactionDBStats(dbFile);

		precisionRecall(Sequences, specialSequences, "GoKrimp");
		precisionRecall(Sequences, specialSequences, "SQS");
		precisionRecall(Sequences, specialSequences, "FSM");
		precisionRecall(Sequences, specialSequences, "ISM");

	}

	public static void precisionRecall(final HashMap<Sequence, Double> Sequences,
			final HashMap<Sequence, Double> specialSequences, final String algorithm) throws IOException {

		// Set up logging
		final FileOutputStream outFile = new FileOutputStream(saveDir + "/" + algorithm + "_" + name + "_pr.txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		System.out.println("\nSpecial Sequences: " + noSpecialSequences);

		// Mine Sequences
		Set<Sequence> minedSequences = null;
		final File logFile = Logging.getLogFileName(algorithm, true, saveDir, dbFile);
		final long startTime = System.currentTimeMillis();
		if (algorithm.equals("FSM")) {
			FrequentSequenceMining.mineClosedFrequentSequencesBIDE(dbFile.getAbsolutePath(), logFile.getAbsolutePath(),
					minSup);
			minedSequences = FrequentSequenceMining.readFrequentSequences(logFile).keySet();
		} else if (algorithm.equals("ISM")) {
			minedSequences = SequenceMining
					.mineSequences(dbFile, new InferGreedy(), maxStructureSteps, noIterations, logFile, false).keySet();
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
		System.out.println("No. mined Sequences: " + len);
		final double[] precision = new double[len];
		final double[] recall = new double[len];
		for (int k = 1; k <= len; k++) {

			final Set<Sequence> topKMined = Sets.newHashSet();
			for (final Sequence seq : minedSequences) {
				topKMined.add(seq);
				if (topKMined.size() == k)
					break;
			}

			final double noInBoth = Sets.intersection(Sequences.keySet(), topKMined).size();
			final double noSpecialInBoth = Sets.intersection(specialSequences.keySet(), topKMined).size();
			final double pr = noInBoth / (double) topKMined.size();
			final double rec = noSpecialInBoth / (double) specialSequences.size();
			precision[k - 1] = pr;
			recall[k - 1] = rec;
		}

		// Output precision and recall
		System.out.println("\n======== " + name + " ========");
		System.out.println("Special Frequency: " + noSpecialSequences);
		System.out.println("Time: " + time);
		System.out.println("Precision (all): " + Arrays.toString(precision));
		System.out.println("Recall (special): " + Arrays.toString(recall));

	}

}
