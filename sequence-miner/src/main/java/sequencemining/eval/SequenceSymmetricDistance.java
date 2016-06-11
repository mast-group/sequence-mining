package sequencemining.eval;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.output.TeeOutputStream;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import sequencemining.main.SequenceMiningCore;
import sequencemining.sequence.Sequence;

public class SequenceSymmetricDistance {

	public static void main(final String[] args) throws IOException {

		// TODO re-run BIDE...
		final int topN = 50;
		final String baseDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/";
		final String[] datasets = new String[] { "alice_punc", "GAZELLE1", "jmlr", "SIGN", "aslbu", "aslgt", "auslan2",
				"context", "pioneer", "skating" };

		// Set up logging
		final FileOutputStream outFile = new FileOutputStream(baseDir + "redundancy.txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		final Writer writer = Files.newWriter(new File(baseDir + "redundancy.tex"), Charsets.UTF_8);

		for (int i = 0; i < datasets.length; i++) {

			System.out.println("===== Dataset: " + datasets[i]);

			// ISM sequences
			final Map<Sequence, Double> intSequences = SequenceMiningCore
					.readISMSequences(new File(baseDir + "Logs/" + datasets[i] + ".log"));
			calculateRedundancyStats("ISM", intSequences, topN, writer);

			// SQS sequences
			final Map<Sequence, Double> sqsSequences = StatisticalSequenceMining
					.readSQSSequences(new File(baseDir + "SQS/" + datasets[i] + ".txt"));
			calculateRedundancyStats("SQS", sqsSequences, topN, writer);

			// GoKrimp sequences
			final Map<Sequence, Double> gokrimpSequences = StatisticalSequenceMining
					.readGoKrimpSequences(new File(baseDir + "GoKrimp/" + datasets[i] + ".txt"));
			calculateRedundancyStats("GoKrimp", gokrimpSequences, topN, writer);

			// BIDE sequences
			final Map<Sequence, Integer> bideSequences = FrequentSequenceMining
					.readFrequentSequences(new File(baseDir + "BIDE/" + datasets[i] + ".txt"));
			calculateRedundancyStats("BIDE", bideSequences, topN, writer);

			System.out.println();
		}
		writer.close();

	}

	private static <V> void calculateRedundancyStats(final String name, final Map<Sequence, V> intSequences,
			final int topN, final Writer writer) throws IOException {
		System.out.println("\n" + name + " Sequences\n-----------");
		System.out.println("No. sequences: " + intSequences.size());
		if (name.equals("ISM"))
			System.out.println(
					"No. non-singleton sequences: " + filterSingletons(intSequences, Integer.MAX_VALUE).size());
		System.out.println("No. items: " + countNoItems(intSequences.keySet()));

		// Get top sequences and calculate stats
		final Set<Sequence> topIntSequences = filterSingletons(intSequences, topN);

		final double avgMinDiff = calculateRedundancy(topIntSequences);
		System.out.println("\nAvg. min edit dist: " + avgMinDiff);
		writer.write("$" + avgMinDiff + "$ & ");

		// Calculate spuriousness
		final double avgMaxSpur = calculateSpuriousness(topIntSequences);
		System.out.println("Avg. no. subseq: " + avgMaxSpur);
		writer.write("$" + avgMaxSpur + "$ & ");

		// Calculate no. items
		final int noItems = countNoItems(topIntSequences);
		System.out.println("No. items: " + noItems);
		writer.write("$" + noItems + "$ & ");

		// Calculate size
		final double avgSize = calculateAverageSize(topIntSequences);
		System.out.println("Avg. subseq size: " + avgSize);

		writer.write("\n");
	}

	private static <V> double calculateRedundancy(final Set<Sequence> topSequences) {

		double avgMinDiff = 0;
		for (final Sequence seq1 : topSequences) {

			int minDiff = Integer.MAX_VALUE;
			for (final Sequence seq2 : topSequences) {
				if (!seq1.equals(seq2)) {
					final int diff = editDistance(seq1, seq2);
					if (diff < minDiff)
						minDiff = diff;
				}
			}
			avgMinDiff += minDiff;
		}
		avgMinDiff /= topSequences.size();

		return avgMinDiff;
	}

	/**
	 * Calculate the Levenshtein distance between two sequences using the
	 * Wagner-Fischer algorithm
	 *
	 * @see http://en.wikipedia.org/wiki/Levenshtein_distance
	 */
	private static int editDistance(final Sequence s, final Sequence t) {
		final int m = s.size();
		final int n = t.size();

		// for all i and j, d[i,j] will hold the Levenshtein distance between
		// the first i characters of s and the first j characters of t;
		final int[][] d = new int[m + 1][n + 1];

		// the distance of any first string to an empty second string
		for (int i = 1; i <= m; i++)
			d[i][0] = i;

		// the distance of any second string to an empty first string
		for (int j = 1; j <= n; j++)
			d[0][j] = j;

		for (int j = 1; j <= n; j++) {
			for (int i = 1; i <= m; i++) {
				if (s.get(i - 1) == t.get(j - 1)) {
					d[i][j] = d[i - 1][j - 1]; // no operation required
				} else {
					d[i][j] = Math.min(d[i - 1][j] + 1, // a deletion
							Math.min(d[i][j - 1] + 1, // an insertion
									d[i - 1][j - 1] + 1)); // a substitution
				}
			}
		}

		return d[m][n];
	}

	/**
	 * Count the number of distinct items in the set of sequences
	 */
	public static int countNoItems(final Set<Sequence> sequences) {
		final Set<Integer> items = new HashSet<>();
		for (final Sequence seq : sequences)
			items.addAll(seq.getItems());
		return items.size();
	}

	private static double calculateAverageSize(final Set<Sequence> topSequences) {

		double avgSize = 0;
		for (final Sequence seq : topSequences)
			avgSize += seq.size();
		return avgSize / topSequences.size();
	}

	private static <V> double calculateSpuriousness(final Set<Sequence> topSequences) {

		double avgSubseq = 0;
		for (final Sequence seq1 : topSequences) {
			for (final Sequence seq2 : topSequences) {
				if (!seq1.equals(seq2))
					avgSubseq += isSubseq(seq1, seq2);
			}
		}
		avgSubseq /= topSequences.size();

		return avgSubseq;
	}

	/** Filter out singletons */
	static <V> Set<Sequence> filterSingletons(final Map<Sequence, V> seqs, final int topN) {

		int count = 0;
		final Set<Sequence> topSeqs = new HashSet<>();
		for (final Sequence seq : seqs.keySet()) {
			if (seq.size() != 1) {
				topSeqs.add(seq);
				count++;
			}
			if (count == topN)
				break;
		}
		if (topN != Integer.MAX_VALUE && count < topN)
			System.out.println("WARNING: not enough non-singleton sequences in set: " + count);

		return topSeqs;
	}

	private static int isSubseq(final Sequence seq1, final Sequence seq2) {
		if (seq2.contains(seq1))
			return 1;
		return 0;
	}

}