ISM: Interesting Sequence Miner [![Build Status](https://travis-ci.org/mast-group/sequence-mining.svg?branch=master)](https://travis-ci.org/mast-group/sequence-mining)
================
 
ISM is a novel algorithm that mines the subsequences that are most interesting under a probablistic model of a sequence database. Our model is able to efficiently infer interesting subsequences directly from the database.

This is an implementation of the sequence miner from our paper:  
[*A Subsequence Interleaving Model for Sequential Pattern Mining*](http://arxiv.org/abs/1602.05012)  
J. Fowkes and C. Sutton. KDD 2016.   


Installation 
------------

#### Installing in Eclipse

Simply import as a maven project into [Eclipse](https://eclipse.org/) using the *File -> Import...* menu option (note that this requires [m2eclipse](http://eclipse.org/m2e/)). 

It's also possible to export a runnable jar from Eclipse using the *File -> Export...* menu option.

#### Compiling a Runnable Jar

To compile a standalone runnable jar, simply run

```
mvn package
```

in the top-level directory (note that this requires [maven](https://maven.apache.org/)). This will create the standalone runnable jar ```sequence-mining-1.0.jar``` in the sequence-mining/target subdirectory. The main class is *sequencemining.main.SequenceMining* (see below).


Running ISM
-----------

ISM uses a Bayesian Network Model to determine which subsequences are the most interesting in a given dataset.  

#### Mining Interesting Sequences 

Main class *sequencemining.main.SequencesMining* mines subsequences from a specified sequences database file. It has the following command line options:

* **-f**  &nbsp;  database file to mine (in [SPMF](http://www.philippe-fournier-viger.com/spmf/) format)
* **-i**  &nbsp;  max. no. iterations
* **-s**  &nbsp;  max. no. structure steps
* **-r**  &nbsp;  max. runtime (min)
* **-l**  &nbsp;  log level (INFO/FINE/FINER/FINEST)
* **-v**  &nbsp;  print to console instead of log file   

See the individual file javadocs in *sequencemining.main.SequenceMining* for information on the Java interface.
In Eclipse you can set command line arguments for the ISM interface using the *Run Configurations...* menu option. 

#### Example Usage

A complete example using the command line interface on a runnable jar. We can mine the provided example dataset ```example.dat``` as follows: 

  ```sh 
  $ java -cp sequence-mining/target/sequence-mining-1.0.jar sequencemining.main.SequenceMining     
   -i 100
   -f example.dat 
   -v 
  ```

which will output to the console. Omitting the ```-v``` flag will redirect output to a log-file in ```/tmp/```. 

Input/Output Formats
--------------------

#### Input Format

ISM takes as input a sequence database file in [SPMF](http://www.philippe-fournier-viger.com/spmf/) format. The SPMF format is very simple: each line of the input file represents a database sequence 
and each sequence is a list of items, represented by positive integers, separated by -1 and ending with -2. For example, the first few lines (database sequences) from ```example.dat``` are:

```text
1 -1 2 -1 3 -1 4 -1 -2
3 -1 5 -1 6 -1 4 -1 -2
3 -1 4 -1 -2
3 -1 5 -1 6 -1 7 -1 8 -1 4 -1 -2
3 -1 4 -1 -2
```

Note that any other item formats (e.g. words for text corpora) 
need to be manually mapped to (and from) positive integers by means of a dictionary.   

#### Output Format

ISM outputs a list of interesting sequences, one sequence per line, ordered first by their interestingness (given in the 'int' column) followed by their probability (given in the 'prob' column). 
For example, the first few lines of output for the usage example above are:

```text
============= INTERESTING SEQUENCES =============
[3] prob: 1.00000   int: 1.00000 
[4] prob: 1.00000   int: 1.00000 
[7, 8]  prob: 0.47500   int: 1.00000 
[5, 6]  prob: 0.32000   int: 1.00000 
[9] prob: 0.10500   int: 1.00000 
[12]    prob: 0.02000   int: 1.00000 
[16]    prob: 0.01000   int: 1.00000 
[9, 10, 5, 6, 10, 6, 9, 10, 5, 6]   prob: 0.01000   int: 1.00000 
[11, 7, 8, 5, 6]    prob: 0.00500   int: 1.00000 
[13, 14, 15, 13, 14, 5, 6]  prob: 0.00500   int: 1.00000 
```

See the accompanying [paper](http://arxiv.org/abs/1602.05012) for details of how to interpret 'interestingness' and 'probability' under ISM's probabilistic model.


Bugs
----

Please report any bugs using GitHub's issue tracker.


License
-------

This algorithm is released under the GNU GPLv3 license. Other licenses available on request.
