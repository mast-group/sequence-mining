package sequencemining.eval;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.output.TeeOutputStream;

import sequencemining.main.SequenceMining;
import sequencemining.sequence.Sequence;

public class PrecisionRecallParallel {

	public static void main(final String[] args) throws IOException, ClassNotFoundException {

		final String baseFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/";
		// final File dbFile = new File(baseFolder + "Datasets/parallel",
		// ".dat");
		// generateParallelDataset(dbFile);

		// Set up logging
		final FileOutputStream outFile = new FileOutputStream(baseFolder + "PrecisionRecall/parallel_pr.txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		// Read SQS sequences
		final File outSQS = new File(baseFolder + "SQS/parallel_partial.txt");
		final Map<Sequence, Double> seqsSQS = StatisticalSequenceMining.readSQSSequences(outSQS);

		// Read GoKrimp sequences
		final File outGOKRIMP = new File(baseFolder + "GoKrimp/parallel.txt");
		final Map<Sequence, Double> seqsGORKIMP = StatisticalSequenceMining.readGoKrimpSequences(outGOKRIMP);

		// Read ISM sequences
		final File outISM = new File(baseFolder + "Logs/parallel.log");
		final Map<Sequence, Double> seqsISM = SequenceMining.readISMSequences(outISM);

		// Precision-recall
		precisionRecall(seqsSQS, "SQS");
		precisionRecall(seqsGORKIMP, "GoKrimp");
		precisionRecall(seqsISM, "ISM");

	}

	private static void precisionRecall(final Map<Sequence, Double> seqs, final String alg) {

		// Calculate sorted precision and recall
		final int len = seqs.size();
		final double[] precision = new double[len];
		final double[] recall = new double[len];
		for (int k = 1; k <= seqs.size(); k++) {

			final Set<Sequence> topKMined = new HashSet<>();
			for (final Sequence seq : seqs.keySet()) {
				topKMined.add(seq);
				if (topKMined.size() == k)
					break;
			}

			// Calculate number of right patterns
			double right = 0;
			final Set<Integer> procs = new HashSet<>();
			for (final Sequence seq : topKMined) {
				final int proc = seq.get(0) / 10;
				for (int i = 1; i < seq.size(); i++) {
					if (seq.get(i) / 10 != proc)
						continue;
				}
				right++;
				procs.add(proc);
			}

			precision[k - 1] = right / topKMined.size();
			recall[k - 1] = procs.size() / 5.;
		}

		// Output precision and recall
		System.out.println("\n======== " + alg + " ========");
		System.out.println("No. mined sequences: " + len);
		System.out.println("Precision: " + Arrays.toString(precision));
		System.out.println("Recall: " + Arrays.toString(recall));

	}

	/** Generate parallel dataset */
	@SuppressWarnings("unused")
	private static void generateParallelDataset(final File dbFile)
			throws FileNotFoundException, UnsupportedEncodingException {
		final Random rand = new Random(1);
		final int[] states = new int[] { 0, 0, 0, 0, 0 };
		final PrintWriter db = new PrintWriter(dbFile, "UTF-8");
		for (int j = 1; j <= 1_000_000; j++) {
			final int proc = rand.nextInt(5);
			final int lab1 = proc + 1;
			final int lab2 = (states[proc] % 5) + 1;
			states[proc] += 1;
			db.write(lab1 + "" + lab2 + " -1 ");
			if (j % 100 == 0)
				db.write("-2\n");
		}
		db.close();
	}

}
