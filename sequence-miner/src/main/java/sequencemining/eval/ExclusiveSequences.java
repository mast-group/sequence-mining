package sequencemining.eval;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.TeeOutputStream;

import sequencemining.main.SequenceMiningCore;
import sequencemining.sequence.Sequence;

public class ExclusiveSequences {

	public static void main(final String[] args) throws IOException {

		final int topN = 20;
		final String baseDir = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/";
		// final String dataset = "jmlr";
		// final String seqLabels = baseDir + "Datasets/JMLR/jmlr.lab";
		final String dataset = "alice_punc";
		final String seqLabels = baseDir + "Datasets/Alice/WithPunctuation/alice_punc.lab";

		// Set up logging
		final FileOutputStream outFile = new FileOutputStream(baseDir + dataset + "_exclusive.txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		final Map<Sequence, Double> ismSeqs = SequenceMiningCore
				.readISMSequences(new File(baseDir + "Logs/" + dataset + ".log"));
		final Map<Sequence, Double> sqsSeqs = StatisticalSequenceMining
				.readSQSSequences(new File(baseDir + "SQS/" + dataset + ".txt"));
		final Map<Sequence, Double> gokrimpSeqs = StatisticalSequenceMining
				.readGoKrimpSequences(new File(baseDir + "GoKrimp/" + dataset + ".txt"));

		final Set<Sequence> ISMnotSQSorGoKrimp = getExclusiveSequences(ismSeqs.keySet(), sqsSeqs.keySet(),
				gokrimpSeqs.keySet());
		final Set<Sequence> SQSnotISMorGoKrimp = getExclusiveSequences(sqsSeqs.keySet(), ismSeqs.keySet(),
				gokrimpSeqs.keySet());
		final Set<Sequence> GoKrimpnotISMorSQS = getExclusiveSequences(gokrimpSeqs.keySet(), ismSeqs.keySet(),
				sqsSeqs.keySet());

		final List<String> dict = FileUtils.readLines(new File(seqLabels));

		// Print top ten
		System.out.print("\n============= ISM not SQS/GoKrimp =============\n");
		printTopExclusiveSequences(topN, ismSeqs, ISMnotSQSorGoKrimp, dict);
		System.out.print("\n============= SQS not ISM/GoKrimp =============\n");
		printTopExclusiveSequences(topN, sqsSeqs, SQSnotISMorGoKrimp, dict);
		System.out.print("\n============= GoKrimp not ISM/SQS =============\n");
		printTopExclusiveSequences(topN, gokrimpSeqs, GoKrimpnotISMorSQS, dict);

	}

	/**
	 * Set A \ B u C
	 * <p>
	 * Note: slow but Guava contains/Set.difference doesn't work here
	 */
	private static Set<Sequence> getExclusiveSequences(final Set<Sequence> setA, final Set<Sequence> setB,
			final Set<Sequence> setC) {
		final Set<Sequence> exclSeqs = new HashSet<>();
		outer: for (final Sequence seqA : setA) {
			for (final Sequence seqB : setB) {
				if (seqA.equals(seqB))
					continue outer;
			}
			for (final Sequence seqC : setC) {
				if (seqA.equals(seqC))
					continue outer;
			}
			exclSeqs.add(seqA);
		}
		return exclSeqs;
	}

	private static void printTopExclusiveSequences(final int topN, final Map<Sequence, Double> seqs,
			final Set<Sequence> exclusiveSeqs, final List<String> dict) {
		int count = 0;
		for (final Entry<Sequence, Double> entry : seqs.entrySet()) {
			final Sequence set = entry.getKey();
			if (set.size() > 1 && exclusiveSeqs.contains(set)) {
				System.out.print(String.format("%s\tprob: %1.5f %n", decode(entry.getKey(), dict), entry.getValue()));
				count++;
				if (count == topN)
					break;
			}
		}
		System.out.println();
	}

	private static String decode(final Sequence seq, final List<String> dict) {
		String prefix = "";
		final StringBuilder sb = new StringBuilder();
		for (final Integer item : seq) {
			sb.append(prefix);
			sb.append(dict.get(item - 1));
			prefix = ", ";
		}
		return sb.toString();
	}

}
