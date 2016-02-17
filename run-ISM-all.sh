#!/bin/bash
for db in GAZELLE1 alice_punc jmlr SIGN context auslan2 pioneer aslbu skating aslgt parallel
do
java -cp target/driver-sequence-mining-1.0-SNAPSHOT.jar sequencemining.main.SequenceMining -f ~/Code/Sequences/Datasets/Paper/$db.dat
done 
