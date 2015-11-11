package sequencemining.transaction;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.io.LineIterator;
import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import sequencemining.sequence.Sequence;

public class TransactionGenerator {

	private static final boolean VERBOSE = false;

	/**
	 * Create interesting sequences that highlight problems
	 *
	 * @param difficultyLevel
	 *            An integer between 0 and 10
	 *
	 * @param noInstances
	 *            The number of example sequecne instances
	 */
	public static HashMap<Sequence, Double> generateExampleSequences(final String name, final int noInstances,
			final int difficultyLevel) {

		final HashMap<Sequence, Double> sequences = new HashMap<>();

		// Difficulty scaling (times 10^0 to 10^-1)
		final double scaling = Math.pow(10, -difficultyLevel / 10.);

		int maxElement = 80;
		for (int j = 0; j < noInstances; j++) {

			// Here [1, 2] is the champagne & caviar problem
			// (not retrieved when support is too high)
			if (name.equals("caviar")) {

				// Champagne & Caviar
				final Sequence s12 = new Sequence(maxElement + 1, maxElement + 2);
				final double p12 = 0.05 * scaling;
				sequences.put(s12, p12);
				maxElement += 2;

			}
			// Here [B, 1] would be seen as a frequent sequence
			// if both [B] and [1] have sufficiently high support
			else if (name.equals("freerider")) {

				final Sequence s1 = new Sequence(maxElement + 1);
				final double p1 = 0.5 * scaling;
				sequences.put(s1, p1);
				maxElement += 1;

			}
			// Here [1, B] is known as a cross-support pattern
			// if [1] has high support and [B] low support
			// (spuriously generated when support is too low)
			else if (name.equals("cross-supp")) {

				final Sequence s1 = new Sequence(maxElement + 1);
				final double p1 = 0.95 * scaling;
				sequences.put(s1, p1);
				maxElement += 1;

			} else
				throw new IllegalArgumentException("Incorrect problem name.");

		}

		return sequences;
	}

	/**
	 * Generate transactions from set of interesting sequences
	 *
	 * @return set of sequences added to transaction
	 */
	public static HashMap<Sequence, Double> generateTransactionDatabase(final Map<Sequence, Double> sequences,
			final Table<Sequence, Integer, Double> counts, final int noTransactions, final File outFile)
					throws IOException {

		// Set random number seeds
		final Random random = new Random(1);
		final Random randomI = new Random(10);
		final RandomGenerator randomC = new JDKRandomGenerator();
		randomC.setSeed(100);

		// Storage for sequences actually added
		final HashMap<Sequence, Double> addedSequences = new HashMap<>();

		// Set output file
		final PrintWriter out = new PrintWriter(outFile, "UTF-8");

		// Normalize counts
		final Map<Sequence, EnumeratedIntegerDistribution> countDists = new HashMap<>();
		for (final Sequence seq : sequences.keySet()) {
			final List<Integer> singletons = new ArrayList<>();
			final List<Double> probs = new ArrayList<>();
			for (final Entry<Integer, Double> entry : counts.row(seq).entrySet()) {
				singletons.add(entry.getKey());
				probs.add(entry.getValue());
			}
			final EnumeratedIntegerDistribution countDist = new EnumeratedIntegerDistribution(randomC,
					Ints.toArray(singletons), Doubles.toArray(probs));
			countDists.put(seq, countDist);
		}

		// Generate transaction database
		int count = 0;
		while (count < noTransactions) {

			// Generate transaction from distribution
			final Transaction transaction = sampleFromDistribution(random, sequences, countDists, addedSequences,
					randomI);
			for (final int item : transaction) {
				out.print(item + " -1 ");
			}
			if (!transaction.isEmpty()) {
				out.print("-2");
				out.println();
				count++;
			}

		}
		out.close();

		// Print file to screen
		if (VERBOSE) {
			final FileReader reader = new FileReader(outFile);
			final LineIterator it = new LineIterator(reader);
			while (it.hasNext()) {
				System.out.println(it.nextLine());
			}
			LineIterator.closeQuietly(it);
		}

		return addedSequences;
	}

	/**
	 * Randomly generate sequence with its probability, randomly interleaving
	 * subsequences
	 */
	public static Transaction sampleFromDistribution(final Random random, final Map<Sequence, Double> sequences,
			final Map<Sequence, EnumeratedIntegerDistribution> countDists,
			final HashMap<Sequence, Double> addedSequences, final Random randomI) {

		// Sample counts for interesting sequences
		final Multiset<Sequence> seqsWithRep = HashMultiset.create();
		for (final Sequence seq : sequences.keySet()) {
			final int count = countDists.get(seq).sample();
			seqsWithRep.add(seq, count);
		}

		final ArrayList<Integer> transaction = new ArrayList<>();
		for (final Sequence seq : seqsWithRep) {
			if (random.nextDouble() < sequences.get(seq)) {
				interleave(transaction, seq, randomI);
				addedSequences.put(seq, sequences.get(seq));
			}
		}

		return new Transaction(transaction);
	}

	/** Randomly interleave sequence into transaction */
	private static void interleave(final ArrayList<Integer> transaction, final Sequence seq, final Random randomI) {
		if (transaction.size() == 0) {
			transaction.addAll(seq);
		} else {
			int prev = 0;
			for (final Integer item : seq) {
				final int insertionPoint = randomI.nextInt((transaction.size() - prev) + 1) + prev;
				transaction.add(insertionPoint, item);
				prev = insertionPoint + 1;
			}
		}
	}

}
