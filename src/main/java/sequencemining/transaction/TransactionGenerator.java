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
