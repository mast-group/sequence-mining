package sequencemining.eval;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Functions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import com.google.common.io.Files;

import sequencemining.main.SequenceMining;
import sequencemining.sequence.Sequence;
import sequencemining.transaction.Transaction;
import sequencemining.transaction.TransactionList;

public class IntervalClassification {

	public static void main(final String[] args) throws IOException {

		final String[] datasets = new String[] { "context", "auslan2", "pioneer", "aslbu", "skating", "aslgt" };
		final int[] topNs = new int[] { 10, 40, 70, 100 };
		final String baseFolder = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/";
		final String datasetFolder = baseFolder + "Datasets/Intervals/";
		final String outFolder = baseFolder + "Classification/";

		for (int i = 0; i < datasets.length; i++) {
			final String dataset = datasets[i];

			System.out.println("===== Dataset: " + dataset + " =====");
			final File outFile = new File(outFolder + dataset + ".txt");
			final Writer writer = Files.newWriter(outFile, Charsets.UTF_8);
			writer.write("===== " + dataset + " =====\n");
			writer.write("topN: " + Arrays.toString(topNs) + "\n");

			// Read dataset
			final File dbFile = new File(datasetFolder + dataset + "/" + dataset + ".dat");
			final TransactionList dbTrans = SequenceMining.readTransactions(dbFile);
			final File labelFile = new File(datasetFolder + dataset + "/" + dataset + ".lab");

			// Read SQS seqs
			final File outSQS = new File(baseFolder + "SQS/" + dataset + ".txt");
			final Map<Sequence, Double> seqsSQS = StatisticalSequenceMining.readSQSSequences(outSQS);
			// seqsSQS = removeSingletons(seqsSQS);
			System.out.println("SQS: " + seqsSQS);
			writer.write(seqsSQS.size() + " SQS seqs \n");

			// Read GOKRIMP seqs
			final File outGOKRIMP = new File(baseFolder + "GoKrimp/" + dataset + ".txt");
			final Map<Sequence, Double> seqsGOKRIMP = StatisticalSequenceMining.readGoKrimpSequences(outGOKRIMP);
			// seqsGOKRIMP = removeSingletons(seqsGOKRIMP);
			System.out.println("GoKrimp: " + seqsGOKRIMP);
			writer.write(seqsGOKRIMP.size() + " GoKrimp seqs \n");

			// Read ISM seqs
			final File outISM = new File(baseFolder + "Logs/" + dataset + ".log");
			final Map<Sequence, Double> seqsISM = SequenceMining.readISMSequences(outISM);
			System.out.println("ISM: " + seqsISM);
			writer.write(seqsISM.size() + " ISM seqs \n");

			// Read BIDE seqs
			final File outBIDE = new File(baseFolder + "BIDE/" + dataset + ".txt");
			final Map<Sequence, Integer> seqsBIDE = FrequentSequenceMining.readFrequentSequences(outBIDE);
			// seqsBIDE = removeSingletons(seqsBIDE);
			System.out.println("BIDE: " + seqsBIDE);
			writer.write(seqsBIDE.size() + " BIDE seqs \n");

			// Generate simple features
			Map<Sequence, Double> seqsSingleton = new HashMap<>();
			final Table<Sequence, Integer, Double> singletons = SequenceMining
					.scanDatabaseToDetermineInitialProbabilities(dbFile);
			for (final Sequence seq : singletons.rowKeySet())
				seqsSingleton.put(seq, 1 - singletons.get(seq, 0));
			// Sort by support
			final Ordering<Sequence> comparator = Ordering.natural().reverse()
					.onResultOf(Functions.forMap(seqsSingleton)).compound(Ordering.usingToString());
			seqsSingleton = ImmutableSortedMap.copyOf(seqsSingleton, comparator);
			System.out.println("Singeltons: " + seqsSingleton);
			writer.write(seqsSingleton.size() + " Singletons seqs \n");

			// Classify
			final Multimap<String, Double> accuracy = ArrayListMultimap.create();
			for (final int n : topNs) {
				// Run MALLET Naive Bayes classifier
				accuracy.put("SQS", classify(n, seqsSQS, dbTrans, labelFile));
				accuracy.put("GoKrimp", classify(n, seqsGOKRIMP, dbTrans, labelFile));
				accuracy.put("ISM", classify(n, seqsISM, dbTrans, labelFile));
				accuracy.put("BIDE", classify(n, seqsBIDE, dbTrans, labelFile));
				accuracy.put("Singletons", classify(n, seqsSingleton, dbTrans, labelFile));
				// Run libSVM Linear classifier
				accuracy.put("SQS_SVM", classifySVM(n, seqsSQS, dbTrans, labelFile));
				accuracy.put("GoKrimp_SVM", classifySVM(n, seqsGOKRIMP, dbTrans, labelFile));
				accuracy.put("ISM_SVM", classifySVM(n, seqsISM, dbTrans, labelFile));
				accuracy.put("BIDE_SVM", classifySVM(n, seqsBIDE, dbTrans, labelFile));
				accuracy.put("Singletons_SVM", classifySVM(n, seqsSingleton, dbTrans, labelFile));
			}
			for (final String alg : accuracy.keySet())
				writer.write(alg + ": " + accuracy.get(alg) + "\n");
			writer.close();
		}
	}

	/** Classify using MALLET Naive Bayes */
	static <V> Double classify(final int topN, final Map<Sequence, V> seqs, final TransactionList dbTrans,
			final File labelFile) throws IOException {
		if (seqs.size() == 0)
			return 0.;

		// Create temp files
		final File featureFile = File.createTempFile("features_temp", ".txt");
		final File tmpFile = File.createTempFile("mallet_temp", ".txt");
		final File outFile = File.createTempFile("mallet_output_temp", ".txt");

		// Generate features
		generateFeatures(topN, seqs, dbTrans, featureFile, labelFile);

		// Convert to binary MALLET format
		final String cmd[] = new String[4];
		cmd[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Packages/mallet-2.0.7/bin/mallet";
		cmd[1] = "import-svmlight";
		cmd[2] = "--input " + featureFile;
		cmd[3] = "--output " + tmpFile;
		runScript(cmd, null);

		// Classify
		final String cmd2[] = new String[5];
		cmd2[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Packages/mallet-2.0.7/bin/mallet";
		cmd2[1] = "train-classifier";
		cmd2[2] = "--input " + tmpFile;
		cmd2[3] = "--cross-validation 10";
		cmd2[4] = "--report test:accuracy";
		runScript(cmd2, outFile);

		// Print output to screen
		final String cmd3[] = new String[3];
		cmd3[0] = "tail";
		cmd3[1] = "-n 2";
		cmd3[2] = "" + outFile;
		runScript(cmd3, null);

		// Get accuracy
		final String[] lines = FileUtils.readFileToString(outFile).split("\n");
		final double accuracy = Double.parseDouble(lines[lines.length - 1].split(" ")[5]);

		// Remove temp files
		featureFile.delete();
		tmpFile.delete();
		outFile.delete();

		return accuracy;
	}

	/** Classify using libSVM linear kernel */
	static <V> Double classifySVM(final int topN, final Map<Sequence, V> seqs, final TransactionList dbTrans,
			final File labelFile) throws IOException {
		if (seqs.size() == 0)
			return 0.;

		// Create temp files
		final File featureFile = File.createTempFile("features_temp", ".txt");
		final File outFile = File.createTempFile("libsvm_output_temp", ".txt");

		// Generate features
		generateFeatures(topN, seqs, dbTrans, featureFile, labelFile);

		// Classify
		final String cmd[] = new String[4];
		cmd[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Packages/libsvm/svm.sh";
		cmd[1] = "-t 0"; // Linear kernel
		cmd[2] = "-v 10"; // 10-fold cross-validation
		cmd[3] = "" + featureFile;
		runScript(cmd, outFile);

		// Print output to screen
		final String cmd2[] = new String[3];
		cmd2[0] = "tail";
		cmd2[1] = "-n 2";
		cmd2[2] = "" + outFile;
		runScript(cmd2, null);

		// Get accuracy
		final String[] lines = FileUtils.readFileToString(outFile).split("\n");
		final double accuracy = Double.parseDouble(lines[lines.length - 1].split(" ")[4].replace("%", ""));

		// Remove temp files
		featureFile.delete();
		outFile.delete();

		return accuracy;
	}

	private static <V> boolean generateFeatures(final int topN, final Map<Sequence, V> sequences,
			final TransactionList dbTrans, final File featureFile, final File labelFile) throws IOException {

		// Get topN sequences
		final Set<Sequence> topSeqs = getTopSequences(sequences, topN);

		// Set output file
		final PrintWriter out = new PrintWriter(featureFile, "UTF-8");

		// Read transaction labels
		final String[] labels = FileUtils.readFileToString(labelFile).split("\n");

		// Generate features
		int count = 0;
		for (final Transaction trans : dbTrans.getTransactionList()) {
			out.print(labels[count] + " ");
			int fNum = 0;
			for (final Sequence seq : topSeqs) {
				if (trans.contains(seq))
					out.print(fNum + ":1 ");
				else
					out.print(fNum + ":0 ");
				fNum++;
			}
			out.println();
			count++;
		}
		out.close();

		return true;
	}

	/** Get top sequences */
	private static <V> Set<Sequence> getTopSequences(final Map<Sequence, V> sequences, final int topN) {

		int count = 0;
		final Set<Sequence> topItemsets = new HashSet<>();
		for (final Sequence set : sequences.keySet()) {
			topItemsets.add(set);
			count++;
			if (count == topN)
				break;
		}
		if (count < topN)
			System.out.println("WARNING: not enough sequences in set: " + count);

		return topItemsets;
	}

	@SuppressWarnings("unused")
	private static <V> Map<Sequence, V> removeSingletons(final Map<Sequence, V> oldSeqs) {
		final Map<Sequence, V> newSeqs = new HashMap<>();
		for (final Entry<Sequence, V> entry : oldSeqs.entrySet()) {
			if (entry.getKey().size() > 1)
				newSeqs.put(entry.getKey(), entry.getValue());
		}
		return newSeqs;
	}

	/** Run shell script with command line arguments */
	public static void runScript(final String cmd[], final File outFile) {

		try {
			final ProcessBuilder pb = new ProcessBuilder(cmd);
			if (outFile != null)
				pb.redirectOutput(outFile);
			else
				pb.redirectOutput(Redirect.INHERIT);
			pb.redirectError(Redirect.INHERIT);
			final Process process = pb.start();
			process.waitFor();
			process.destroy();
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

}
