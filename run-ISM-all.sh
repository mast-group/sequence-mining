#!/bin/bash
for db in gazelle alice jmlr sign parallel
do
java -cp sequence-mining/target/sequence-mining-1.0.jar sequencemining.main.SequenceMining -f datasets/$db.dat
done
for db in context auslan2 pioneer aslbu skating aslgt
do
java -cp sequence-mining/target/sequence-mining-1.0.jar sequencemining.main.SequenceMining -f datasets/classification/$db.dat
done 
