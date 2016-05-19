package sequencemining.sequence;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.junit.Test;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import sequencemining.transaction.Transaction;
import sequencemining.transaction.TransactionGenerator;

public class PartitionTest {

	double EPS = 1E-15; // Approx. machine epsilon

	@Test
	public void testNormalisingConstant() {

		final Set<Sequence> seqs = new HashSet<>();

		// Test #1
		seqs.add(new Sequence(1, 2));
		seqs.add(new Sequence(3));
		assertEquals(3, modP(seqs.iterator()), EPS);

		// Test #2
		seqs.add(new Sequence(4));
		assertEquals(12, modP(seqs.iterator()), EPS);

		// Test #3
		seqs.clear();
		seqs.add(new Sequence(1, 2, 3));
		seqs.add(new Sequence(4));
		assertEquals(4, modP(seqs.iterator()), EPS);

		// Test #4
		seqs.clear();
		seqs.add(new Sequence(1));
		seqs.add(new Sequence(2));
		seqs.add(new Sequence(3));
		seqs.add(new Sequence(4));
		assertEquals(24, modP(seqs.iterator()), EPS); // 4Perm4 = 24

		// Test #5
		seqs.clear();
		seqs.add(new Sequence(1, 2, 3, 4));
		assertEquals(1, modP(seqs.iterator()), EPS); // 4Comb4 = 1

	}

	@Test
	public void testInterleavingGenerator() {

		final Random random = new Random(1);
		final Random randomI = new Random(10);
		final RandomGenerator randomC = new JDKRandomGenerator();
		randomC.setSeed(100);

		final Multiset<Sequence> seqsI = HashMultiset.create();
		seqsI.add(new Sequence(1, 2, 3));
		seqsI.add(new Sequence(4, 5));
		seqsI.add(new Sequence(6));
		seqsI.add(new Sequence(7));

		final HashMap<Sequence, Double> seqsG = new HashMap<>();
		for (final Sequence seq : seqsI.elementSet()) {
			seqsG.put(seq, 1.0);
		}

		final Map<Sequence, EnumeratedIntegerDistribution> countDists = new HashMap<>();
		final EnumeratedIntegerDistribution oneRepeat = new EnumeratedIntegerDistribution(randomC, new int[] { 1 },
				new double[] { 1.0 });
		countDists.put(new Sequence(1, 2, 3), oneRepeat);
		countDists.put(new Sequence(4, 5), oneRepeat);
		countDists.put(new Sequence(6), oneRepeat);
		countDists.put(new Sequence(7), oneRepeat);

		final HashSet<Transaction> transG = new HashSet<>();
		for (int i = 0; i < 700000; i++)
			transG.add(
					TransactionGenerator.sampleFromDistribution(random, seqsG, countDists, new HashMap<>(), randomI));
		// Note that upper bound is exact when there are no repetitions
		assertEquals(transG.size(), modP(seqsI.iterator()), EPS);
	}

	/**
	 * Calculate upper bound on interleaving model normalization constant for the given set of
	 * sequences (bound is exact when there are no repetitions)
	 */
	public static double modP(final Iterator<Sequence> iterator) {
		double prod = 1;
		int ln = 1;
		while (iterator.hasNext()) {
			final int seqSize = iterator.next().size();
			prod *= nMk(ln, seqSize);
			ln += seqSize;
		}
		return prod;
	}

	/** N multichoose K (combinations with repetition) **/
	public static double nMk(final int n, final int k) {
		double prodNum = 1;
		for (int i = n; i <= n + k - 1; i++)
			prodNum *= i;
		double prodDenom = 1;
		for (int i = 2; i <= k; i++)
			prodDenom *= i;
		return prodNum / prodDenom;
	}

	// /**
	// * Calculate interleaving model normalization constant for the given set
	// of
	// * sequences
	// */
	// public static double modP(final Set<Sequence> sequences) {
	// int freeSlots = 0;
	// for (final Sequence seq : sequences)
	// freeSlots += seq.size();
	//
	// double prod = 1;
	// for (final Sequence seq : sequences) {
	// final int seqSize = seq.size();
	// prod *= nestedSum(freeSlots, seqSize);
	// freeSlots -= seqSize;
	// }
	// return prod;
	// }
	//
	// private static double nestedSum(final int n, final int e) {
	// if (e == 1)
	// return n - e + 1;
	// double sum = 0;
	// for (int i = 1; i <= n - e + 1; i++)
	// sum += nestedSum(n - i, e - 1);
	// return sum;
	// }

}
