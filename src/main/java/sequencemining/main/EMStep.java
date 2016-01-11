package sequencemining.main;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;

import scala.Tuple2;
import sequencemining.main.InferenceAlgorithms.InferenceAlgorithm;
import sequencemining.sequence.Sequence;
import sequencemining.transaction.Transaction;
import sequencemining.transaction.TransactionDatabase;

/** Class to hold the various transaction EM Steps */
public class EMStep {

	/** Initialize cached sequences */
	static void initializeCachedSequences(final TransactionDatabase transactions,
			final Table<Sequence, Integer, Double> initProbs) {
		transactions.getTransactionList().parallelStream().forEach(t -> t.initializeCachedSequences(initProbs));
	}

	/** EM-step for hard EM */
	static Table<Sequence, Integer, Double> hardEMStep(final List<Transaction> transactions,
			final InferenceAlgorithm inferenceAlgorithm) {
		final double noTransactions = transactions.size();

		// E-step
		final Map<Multiset.Entry<Sequence>, Long> coveringWithCounts = transactions.parallelStream().map(t -> {
			final Multiset<Sequence> covering = inferenceAlgorithm.infer(t);
			t.setCachedCovering(covering);
			return covering.entrySet();
		}).flatMap(Set::stream).collect(groupingBy(identity(), counting()));

		// M-step
		final Table<Sequence, Integer, Double> newSequences = coveringWithCounts.entrySet().parallelStream().collect(
				HashBasedTable::create,
				(t, e) -> t.put(e.getKey().getElement(), e.getKey().getCount(), e.getValue() / noTransactions),
				Table::putAll);
		newSequences.rowKeySet().parallelStream().forEach(seq -> {
			// Pad with zero counts for non-occurrences
			final int maxOccur = Collections.max(newSequences.row(seq).keySet());
			for (int occur = 1; occur <= maxOccur; occur++) {
				if (!newSequences.contains(seq, occur))
					newSequences.put(seq, occur, 0.);
			} // Add probabilities for zero occurrences
			double rowSum = 0;
			for (final Double count : newSequences.row(seq).values())
				rowSum += count;
			newSequences.put(seq, 0, 1 - rowSum);
		});

		// Update cached sequences
		transactions.parallelStream().forEach(t -> t.updateCachedSequences(newSequences));

		return newSequences;
	}

	/** Get average cost of last EM-step */
	static void calculateAndSetAverageCost(final TransactionDatabase transactions) {
		final double noTransactions = transactions.size();
		final double averageCost = transactions.getTransactionList().parallelStream()
				.mapToDouble(Transaction::getCachedCost).sum() / noTransactions;
		transactions.setAverageCost(averageCost);
	}

	/** EM-step for structural EM */
	static Tuple2<Double, Map<Integer, Double>> structuralEMStep(final TransactionDatabase transactions,
			final InferenceAlgorithm inferenceAlgorithm, final Sequence candidate) {
		final double noTransactions = transactions.size();

		// Calculate max. no. of candidate occurrences
		final int maxReps = transactions.getTransactionList().parallelStream().mapToInt(t -> t.repetitions(candidate))
				.max().getAsInt();
		final Map<Integer, Double> initProb = new HashMap<>();
		initProb.put(0, 0.);
		for (int occur = 1; occur <= maxReps; occur++)
			initProb.put(occur, 1.);

		// E-step (adding candidate to transactions that support it)
		final Map<Multiset.Entry<Sequence>, Long> coveringWithCounts = transactions.getTransactionList()
				.parallelStream().map(t -> {
					if (t.contains(candidate)) {
						t.addSequenceCache(candidate, initProb);
						final Multiset<Sequence> covering = inferenceAlgorithm.infer(t);
						t.setTempCachedCovering(covering);
						return covering.entrySet();
					}
					return t.getCachedCovering().entrySet();
				}).flatMap(Set::stream).collect(groupingBy(identity(), counting()));

		// M-step
		final Table<Sequence, Integer, Double> newSequences = coveringWithCounts.entrySet().parallelStream().collect(
				HashBasedTable::create,
				(t, e) -> t.put(e.getKey().getElement(), e.getKey().getCount(), e.getValue() / noTransactions),
				Table::putAll);
		newSequences.rowKeySet().parallelStream().forEach(seq -> {
			// Pad with zero counts for non-occurrences
			final int maxOccur = Collections.max(newSequences.row(seq).keySet());
			for (int occur = 1; occur <= maxOccur; occur++) {
				if (!newSequences.contains(seq, occur))
					newSequences.put(seq, occur, 0.);
			} // Add probabilities for zero occurrences
			double rowSum = 0;
			for (final Double count : newSequences.row(seq).values())
				rowSum += count;
			newSequences.put(seq, 0, 1 - rowSum);
		});

		// Get average cost (removing candidate from supported transactions)
		final double averageCost = transactions.getTransactionList().parallelStream().mapToDouble(t -> {
			double cost;
			if (t.contains(candidate))
				cost = t.getTempCachedCost(newSequences);
			else
				cost = t.getCachedCost(newSequences);
			t.removeSequenceCache(candidate);
			return cost;
		}).sum() / noTransactions;

		// Get candidate prob
		final Map<Integer, Double> prob = newSequences.row(candidate);

		return new Tuple2<Double, Map<Integer, Double>>(averageCost, prob);
	}

	/** Add accepted candidate itemset to cache */
	static Table<Sequence, Integer, Double> addAcceptedCandidateCache(final TransactionDatabase transactions,
			final Sequence candidate, final Map<Integer, Double> prob) {
		final double noTransactions = transactions.size();

		// Cached E-step (adding candidate to transactions that support it)
		final Map<Multiset.Entry<Sequence>, Long> coveringWithCounts = transactions.getTransactionList()
				.parallelStream().map(t -> {
					if (t.contains(candidate)) {
						t.addSequenceCache(candidate, prob);
						final Multiset<Sequence> covering = t.getTempCachedCovering();
						t.setCachedCovering(covering);
						return covering.entrySet();
					}
					return t.getCachedCovering().entrySet();
				}).flatMap(Set::stream).collect(groupingBy(identity(), counting()));

		// M-step
		final Table<Sequence, Integer, Double> newSequences = coveringWithCounts.entrySet().parallelStream().collect(
				HashBasedTable::create,
				(t, e) -> t.put(e.getKey().getElement(), e.getKey().getCount(), e.getValue() / noTransactions),
				Table::putAll);
		newSequences.rowKeySet().parallelStream().forEach(seq -> {
			// Pad with zero counts for non-occurrences
			final int maxOccur = Collections.max(newSequences.row(seq).keySet());
			for (int occur = 1; occur <= maxOccur; occur++) {
				if (!newSequences.contains(seq, occur))
					newSequences.put(seq, occur, 0.);
			} // Add probabilities for zero occurrences
			double rowSum = 0;
			for (final Double count : newSequences.row(seq).values())
				rowSum += count;
			newSequences.put(seq, 0, 1 - rowSum);
		});

		// Update cached itemsets
		transactions.getTransactionList().parallelStream().forEach(t -> t.updateCachedSequences(newSequences));

		return newSequences;
	}

	/** Get the support of requested sequence */
	static int getSupportOfSequence(final TransactionDatabase transactions, final Sequence seq) {
		return transactions.getTransactionList().parallelStream().mapToInt(t -> {
			if (t.contains(seq))
				return 1;
			return 0;
		}).sum();
	}

	private EMStep() {
	}

}
