package sequencemining.transaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;

import sequencemining.sequence.AbstractSequence;
import sequencemining.sequence.Sequence;

/** A transaction is an ordered list of items */
public class Transaction extends AbstractSequence implements Serializable {
	private static final long serialVersionUID = 3327396055332538091L;

	/** Cached sequences and probabilities for this transaction */
	private Table<Sequence, Integer, Double> cachedSequences;

	/** Cached covering for this transaction */
	private Multiset<Sequence> cachedCovering;
	private Multiset<Sequence> tempCachedCovering;

	public void initializeCachedSequences(final Table<Sequence, Integer, Double> initProbs) {
		final Table<Sequence, Integer, Double> probs = HashBasedTable.create();
		for (final Sequence seq : initProbs.rowKeySet()) {
			if (this.contains(seq))
				probs.row(seq).putAll(initProbs.row(seq));
		}
		cachedSequences = probs;
	}

	public Table<Sequence, Integer, Double> getCachedSequences() {
		return cachedSequences;
	}

	public void addSequenceCache(final Sequence candidate, final Map<Integer, Double> prob) {
		cachedSequences.row(candidate).putAll(prob);
	}

	public void removeSequenceCache(final Sequence candidate) {
		cachedSequences.row(candidate).clear();
	}

	public void updateCachedSequences(final Table<Sequence, Integer, Double> newSequences) {
		for (final Sequence seq : cachedSequences.rowKeySet()) {
			final double zeroProb = newSequences.get(seq, 0);
			// seq.size() != 1 so we can fill incomplete coverings
			if (seq.size() != 1 && (int) zeroProb == 1) {
				cachedSequences.row(seq).clear();
			} else {
				cachedSequences.row(seq).clear();
				cachedSequences.row(seq).putAll(newSequences.row(seq));
			}
		}
	}

	/** Get cost of cached covering for hard EM-step */
	public double getCachedCost() {
		double totalCost = 0;
		int lenCovering = 0;
		// TODO triple check that this is right!!!
		// Calculate (3.3)
		for (final Sequence seq : cachedSequences.rowKeySet()) {
			if (cachedCovering.contains(seq)) {
				final int occur = cachedCovering.count(seq);
				totalCost += -Math.log(cachedSequences.get(seq, occur));
				for (int m = 1; m <= occur; m++) {
					totalCost += sumLogRange(lenCovering + 1, lenCovering + seq.size()) - sumLogRange(1, seq.size());
					lenCovering += seq.size();
				}
			} else
				totalCost += -Math.log(cachedSequences.get(seq, 0));
		}
		return totalCost;
	}

	/** Get cost of cached covering for structural EM-step */
	public double getCachedCost(final Table<Sequence, Integer, Double> sequences) {
		return calculateCachedCost(sequences, cachedCovering);
	}

	/** Get cost of temp. cached covering for structural EM-step */
	public double getTempCachedCost(final Table<Sequence, Integer, Double> sequences) {
		return calculateCachedCost(sequences, tempCachedCovering);
	}

	/** Calculate cached cost for structural EM-step */
	private double calculateCachedCost(final Table<Sequence, Integer, Double> sequences,
			final Multiset<Sequence> covering) {
		double totalCost = 0;
		int lenCovering = 0;
		for (final Sequence seq : cachedSequences.rowKeySet()) {
			if (sequences.containsColumn(seq)) {
				if (covering.contains(seq)) {
					final int occur = covering.count(seq);
					totalCost += -Math.log(sequences.get(seq, occur));
					for (int m = 1; m <= occur; m++) {
						totalCost += sumLogRange(lenCovering + 1, lenCovering + seq.size())
								- sumLogRange(1, seq.size());
						lenCovering += seq.size();
					}
				} else
					totalCost += -Math.log(sequences.get(seq, 0));
			}
		}
		return totalCost;
	}

	private double sumLogRange(final int a, final int b) {
		double sum = 0;
		for (int i = a; i <= b; i++)
			sum += Math.log(i);
		return sum;
	}

	public void setCachedCovering(final Multiset<Sequence> covering) {
		cachedCovering = covering;
	}

	public Multiset<Sequence> getCachedCovering() {
		return cachedCovering;
	}

	public void setTempCachedCovering(final Multiset<Sequence> covering) {
		tempCachedCovering = covering;
	}

	public Multiset<Sequence> getTempCachedCovering() {
		return tempCachedCovering;
	}

	/**
	 * Constructor
	 */
	public Transaction() {
		this.items = new ArrayList<>();
	}

	/**
	 * Constructor
	 *
	 * @param items
	 *            an array of items that should be added to the new sequence
	 */
	public Transaction(final Integer... items) {
		this.items = new ArrayList<>(Arrays.asList(items));
	}

	/**
	 * Constructor
	 *
	 * @param items
	 *            a List of items that should be added to the new sequence
	 */
	public Transaction(final List<Integer> items) {
		this.items = new ArrayList<>(items);
	}

}