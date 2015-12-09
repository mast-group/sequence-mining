package sequencemining.main;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Charsets;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import com.google.common.io.Files;

import sequencemining.main.InferenceAlgorithms.InferGreedy;
import sequencemining.main.InferenceAlgorithms.InferenceAlgorithm;
import sequencemining.sequence.Sequence;
import sequencemining.transaction.Transaction;
import sequencemining.transaction.TransactionList;
import sequencemining.util.Logging;

public class SequenceMining extends SequenceMiningCore {

	/** Main function parameters */
	public static class Parameters {

		@Parameter(names = { "-f", "--file" }, description = "Dataset filename")
		private final File dataset = new File(
				"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Datasets/Paper/jmlr.dat");

		@Parameter(names = { "-s", "--maxSteps" }, description = "Max structure steps")
		int maxStructureSteps = 100_000;

		@Parameter(names = { "-i", "--iterations" }, description = "Max iterations")
		int maxEMIterations = 1_000;

		@Parameter(names = { "-l", "--log-level" }, description = "Log level", converter = LogLevelConverter.class)
		Level logLevel = Level.FINE;

		@Parameter(names = { "-r", "--runtime" }, description = "Max Runtime (min)")
		long maxRunTime = 72 * 60; // 12hrs

		@Parameter(names = { "-t", "--timestamp" }, description = "Timestamp Logfile", arity = 1)
		boolean timestampLog = true;

		@Parameter(names = { "-d", "--dist" }, description = "Save sequence count distribution")
		private boolean saveCountDist = false;

		@Parameter(names = { "-v", "--verbose" }, description = "Print to console instead of logfile")
		private boolean verbose = false;
	}

	public static void main(final String[] args) throws IOException {

		// Main fixed parameters
		final InferenceAlgorithm inferenceAlg = new InferGreedy();

		// Runtime parameters
		final Parameters params = new Parameters();
		final JCommander jc = new JCommander(params);

		try {
			jc.parse(args);

			// Set loglevel, runtime, timestamp and log file
			LOG_LEVEL = params.logLevel;
			MAX_RUNTIME = params.maxRunTime * 60 * 1_000;
			File logFile = null;
			if (!params.verbose)
				logFile = Logging.getLogFileName("ISM", params.timestampLog, LOG_DIR, params.dataset);

			// Mine interesting sequences
			mineSequences(params.dataset, inferenceAlg, params.maxStructureSteps, params.maxEMIterations, logFile,
					params.saveCountDist);

		} catch (final ParameterException e) {
			System.out.println(e.getMessage());
			jc.usage();
		}

	}

	/** Mine interesting sequences */
	public static Map<Sequence, Double> mineSequences(final File inputFile, final InferenceAlgorithm inferenceAlgorithm,
			final int maxStructureSteps, final int maxEMIterations, final File logFile, final boolean saveCountDist)
					throws IOException {

		// Set up logging
		if (logFile != null)
			Logging.setUpFileLogger(logger, LOG_LEVEL, logFile);
		else
			Logging.setUpConsoleLogger(logger, LOG_LEVEL);

		// Echo input parameters
		logger.info("========== INTERESTING SEQUENCE MINING ============");
		logger.info("\n Time: " + new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss").format(new Date()));
		logger.info("\n Inputs: -f " + inputFile + " -s " + maxStructureSteps + " -i " + maxEMIterations + " -r "
				+ MAX_RUNTIME / 60_000);

		// Read in transaction database
		final TransactionList transactions = readTransactions(inputFile);

		// Determine initial probabilities
		final Table<Sequence, Integer, Double> initProbs = scanDatabaseToDetermineInitialProbabilities(inputFile);

		// Run inference to find interesting sequences
		logger.fine("\n============= SEQUENCE INFERENCE =============\n");
		final Table<Sequence, Integer, Double> sequences = structuralEM(transactions, initProbs, inferenceAlgorithm,
				maxStructureSteps, maxEMIterations);
		if (LOG_LEVEL.equals(Level.FINEST))
			logger.finest(
					"\n======= Transaction Database =======\n" + Files.toString(inputFile, Charsets.UTF_8) + "\n");

		// Calculate probabilities: p(S \in X) = p(z_S >= 1) = 1 - \pi_S_0
		final HashMap<Sequence, Double> sequenceMap = new HashMap<>();
		for (final Sequence seq : sequences.rowKeySet())
			sequenceMap.put(seq, 1 - sequences.get(seq, 0));

		// Sort sequences by interestingness
		final HashMap<Sequence, Double> intMap = calculateInterestingness(sequenceMap, transactions);
		final Map<Sequence, Double> sortedSequences = sortSequences(sequenceMap, intMap);

		logger.info("\n============= INTERESTING SEQUENCES =============\n");
		for (final Entry<Sequence, Double> entry : sortedSequences.entrySet()) {
			logger.info(String.format("%s\tprob: %1.5f \tint: %1.5f %n", entry.getKey(), entry.getValue(),
					intMap.get(entry.getKey())));
		}
		logger.info("\n");

		// Optionally save sequence count distribution
		if (saveCountDist) {
			Logging.serialize(sequences, FilenameUtils.removeExtension(logFile.getAbsolutePath()) + ".dist");
		}

		return sortedSequences;
	}

	public static TransactionList readTransactions(final File inputFile) throws IOException {

		final List<Transaction> transactions = new ArrayList<>();
		final List<String> cachedLines = new ArrayList<>();

		// for each line (transaction) until the end of file
		final LineIterator it = FileUtils.lineIterator(inputFile, "UTF-8");
		while (it.hasNext()) {

			final String line = it.nextLine();
			// if the line is a comment, is empty or is a
			// kind of metadata
			if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}

			// cache line as string (for fast support counting)
			cachedLines.add(line);

			// split the transaction into items
			final String[] lineSplited = line.split(" ");
			// convert to Transaction class and add it to the structure
			transactions.add(getTransaction(lineSplited));

		}
		// close the input file
		LineIterator.closeQuietly(it);

		// Convert cached lines to array
		final String[] cachedDB = cachedLines.toArray(new String[cachedLines.size()]);

		return new TransactionList(transactions, cachedDB);
	}

	/**
	 * Create and add the Transaction in the String array
	 *
	 * @param integers
	 *            one line of integers in the sequence database
	 */
	public static Transaction getTransaction(final String[] integers) {
		final Transaction sequence = new Transaction();

		for (int i = 0; i < integers.length; i++) {
			if (integers[i].equals("-1")) { // end of item

			} else if (integers[i].equals("-2")) { // end of sequence
				return sequence;
			} else { // extract the value for an item
				sequence.add(Integer.parseInt(integers[i]));
			}
		}
		throw new RuntimeException("Corrupt sequence database.");
	}

	/**
	 * This method scans the input database to determine the initial
	 * probabilities of single items
	 *
	 * @param inputFile
	 *            the input file
	 * @return class storing the support of every occurrence of each singleton
	 */
	public static Table<Sequence, Integer, Double> scanDatabaseToDetermineInitialProbabilities(final File inputFile)
			throws IOException {

		// Sequence x occurence x count
		final Table<Sequence, Integer, Double> supports = HashBasedTable.create();

		// for each line (transaction) until the end of file
		int noTransactions = 0;
		final LineIterator it = FileUtils.lineIterator(inputFile, "UTF-8");
		while (it.hasNext()) {

			final String line = it.nextLine();
			// if the line is a comment, is empty or is a kind of metadata
			if (line.isEmpty() == true || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}

			// split the line into items
			final String[] lineSplit = line.split(" ");
			// for each item
			final Multiset<Sequence> seenItems = HashMultiset.create();
			for (final String itemString : lineSplit) {
				final int item = Integer.parseInt(itemString);
				if (item >= 0) // ignore end of itemset/sequence tags
					seenItems.add(new Sequence(item));
			}
			// increase the support count of the items
			for (final Sequence seq : seenItems.elementSet()) {
				final int occur = seenItems.count(seq);
				if (supports.contains(seq, occur)) {
					final double supp = supports.get(seq, occur);
					supports.put(seq, occur, supp + 1);
				} else {
					supports.put(seq, occur, 1.);
				}
			}

			noTransactions++;
		}

		// close the input file
		LineIterator.closeQuietly(it);

		for (final Sequence seq : supports.rowKeySet()) {
			// Pad with zero counts for non-occurrences
			final int maxOccur = Collections.max(supports.row(seq).keySet());
			for (int occur = 1; occur <= maxOccur; occur++) {
				if (!supports.contains(seq, occur))
					supports.put(seq, occur, 0.);
			} // Add counts for zero occurrences
			double rowSum = 0;
			for (final Double count : supports.row(seq).values())
				rowSum += count;
			supports.put(seq, 0, noTransactions - rowSum);
		}

		// Normalize
		for (final Sequence seq : supports.rowKeySet()) {
			double rowSum = 0;
			for (final Double prob : supports.row(seq).values())
				rowSum += prob;
			for (final Integer occur : supports.row(seq).keySet()) {
				final double normProb = supports.get(seq, occur) / rowSum;
				supports.put(seq, occur, normProb);
			}
		}

		return supports;
	}

	/** Convert string level to level class */
	public static class LogLevelConverter implements IStringConverter<Level> {
		@Override
		public Level convert(final String value) {
			if (value.equals("SEVERE"))
				return Level.SEVERE;
			else if (value.equals("WARNING"))
				return Level.WARNING;
			else if (value.equals("INFO"))
				return Level.INFO;
			else if (value.equals("CONFIG"))
				return Level.CONFIG;
			else if (value.equals("FINE"))
				return Level.FINE;
			else if (value.equals("FINER"))
				return Level.FINER;
			else if (value.equals("FINEST"))
				return Level.FINEST;
			else
				throw new RuntimeException("Incorrect Log Level.");
		}
	}

}