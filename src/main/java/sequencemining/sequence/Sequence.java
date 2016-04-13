package sequencemining.sequence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sequence extends AbstractSequence implements Serializable {
	private static final long serialVersionUID = -2766830126344921771L;

	/**
	 * Constructor
	 */
	public Sequence() {
		this.items = new ArrayList<>();
	}

	/**
	 * Shallow Copy Constructor
	 *
	 * @param seq
	 *            sequence to shallow copy
	 */
	public Sequence(final Sequence seq) {
		this.items = seq.items;
	}

	/**
	 * Constructor
	 *
	 * @param items
	 *            a list of items that should be added to the new sequence
	 */
	public Sequence(final List<Integer> items) {
		this.items = new ArrayList<>(items);
	}

	/**
	 * Constructor
	 *
	 * @param items
	 *            an array of items that should be added to the new sequence
	 */
	public Sequence(final Integer... items) {
		this.items = new ArrayList<>(Arrays.asList(items));
	}

	/**
	 * Join Constructor
	 *
	 * @param seqs
	 *            two sequences that should be joined
	 */
	public Sequence(final Sequence seq1, final Sequence seq2) {
		this.items = new ArrayList<>(seq1.items);
		this.items.addAll(seq2.items);
	}

}
