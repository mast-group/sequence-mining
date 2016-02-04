package sequencemining.main;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import sequencemining.main.InferenceAlgorithms.InferGreedy;
import sequencemining.main.InferenceAlgorithms.InferenceAlgorithm;
import sequencemining.sequence.Sequence;
import sequencemining.transaction.Transaction;

public class SequenceMiningTest {

	@Test
	public void testDoInference() {

		// TODO better tests??

		// Subsequences
		final Sequence s1 = new Sequence(3, 4, 5, 8);
		final Map<Integer, Double> p1 = new HashMap<>();
		p1.put(0, 0.6);
		p1.put(1, 0.4);
		final Sequence s2 = new Sequence(7, 9);
		final Map<Integer, Double> p2 = new HashMap<>();
		p2.put(0, 0.7);
		p2.put(1, 0.3);
		// final Sequence s3 = new Sequence(8, 4, 5, 6); // with overlap
		// final double p3 = 0.2;
		final Sequence s3 = new Sequence(8, 6);
		final Map<Integer, Double> p3 = new HashMap<>();
		p3.put(0, 0.8);
		p3.put(1, 0.2);

		// Transaction #1
		final Transaction transaction1 = new Transaction(7, 3, 8, 9, 4, 5, 6, 8);
		transaction1.initializeCachedSequences(HashBasedTable.create());
		transaction1.addSequenceCache(s1, p1);
		transaction1.addSequenceCache(s2, p2);
		transaction1.addSequenceCache(s3, p3);

		// Expected solution #1
		final Multiset<Sequence> expected1 = HashMultiset.create();
		expected1.add(s1);
		expected1.add(s2);
		expected1.add(s3);
		// final HashSet<Integer> order1 = new HashSet<>();
		// order1.add(0);
		// order1.add(1);
		// order1.add(2);

		// Test greedy
		final InferenceAlgorithm inferGreedy = new InferGreedy();
		final Multiset<Sequence> actual = inferGreedy.infer(transaction1);
		System.out.println(actual);
		assertEquals(expected1, actual);
		// assertTrue(order1.containsAll(actual.values()));

		// Subsequences
		final Sequence s4 = new Sequence(1, 2);
		final Map<Integer, Double> p4 = new HashMap<>();
		p4.put(0, 0.5);
		p4.put(1, 0.3);
		p4.put(2, 0.1);
		p4.put(3, 0.1);

		// Transaction #2
		final Transaction transaction2 = new Transaction(1, 2, 1, 2, 1, 2);
		transaction2.initializeCachedSequences(HashBasedTable.create());
		transaction2.addSequenceCache(s4, p4);

		// Expected solution #2
		final Multiset<Sequence> expected2 = HashMultiset.create();
		expected2.add(s4, 3);
		// final HashSet<Integer> order2 = new HashSet<>();
		// order2.add(0);
		// order2.add(2);
		// order2.add(4);

		int lenCovering = 0;
		final int occur = 3;
		double expectedCost2 = -Math.log(p4.get(occur));
		for (int m = 1; m <= 3; m++) {
			expectedCost2 += sumLogRange(lenCovering + 1, lenCovering + s4.size());
			lenCovering += s4.size();
		}

		// Test greedy
		final Multiset<Sequence> actual2 = inferGreedy.infer(transaction2);
		System.out.println(actual2);
		assertEquals(expected2, actual2);
		// assertTrue(order2.containsAll(actual2.values()));
		transaction2.setCachedCovering(actual2);
		assertEquals(expectedCost2, transaction2.getCachedCost(), 1e-15);

	}

	private double sumLogRange(final int a, final int b) {
		double sum = 0;
		for (int i = a; i <= b; i++)
			sum += Math.log(i);
		return sum;
	}

	// @Test
	// public void testCombLoop() {
	//
	// final ArrayList<Sequence> sequences = new ArrayList<>();
	// for (int i = 1; i < 10; i++)
	// sequences.add(new Sequence(i));
	//
	// final int len = sequences.size();
	// for (int k = 0; k < 2 * len - 2; k++) {
	// for (int i = 0; i < len && i < k + 1; i++) {
	// for (int j = 0; j < len && i + j < k + 1; j++) {
	// if (k <= i + j && i != j) {
	// final Sequence s1 = sequences.get(i);
	// final Sequence s2 = sequences.get(j);
	// System.out.println(s1.toString() + s2.toString());
	// }
	// }
	// }
	// }
	//
	// }

}
