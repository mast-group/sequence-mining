''' Plot Classification Results '''
import matplotlib.pyplot as plt
from matplotlib import rc

rc('ps', fonttype=42)
rc('pdf', fonttype=42)

rc('xtick', labelsize=16) 
rc('ytick', labelsize=16) 

path = '/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/Classification/'
datasets = ['aslbu','aslgt','auslan2','context','pioneer','skating']
algs = ['ISM','SQS','GoKrimp','BIDE','Singletons']
cols = ['b','g','m','r','k']

for db in datasets:

    lines = open(path+db+'.txt').readlines()
    x = map(int,lines[1].strip().split(': ')[1].replace('[','').replace(']','').split(', '))
    
    noseqs = {}
    for i in range(2,7):
	line = lines[i].strip().split(' ')
	noseqs[line[1]] = int(line[0])

    n = 1	
    for suffix in ['','_SVM']:
	for alg in algs:
            plt.figure(n)
            plt.subplot(2,3,datasets.index(db)+1)
	    name = alg+suffix	
	    for i in range(7,17):
		line = lines[i].strip().split(': ')
		if name != line[0]:	
		    continue	
		y = map(float,line[1].replace('[','').replace(']','').split(', '))
		
		xx = []
		yy = []
		nseqs = noseqs[name.replace('_SVM','')]
		if(x[0] > nseqs):
		    xx.append(nseqs)
		    yy.append(y[0])
		for k in range(0,len(x)):
		    if(x[k] > nseqs):
			break
		    xx.append(x[k])
		    yy.append(y[k])

		if n == 2:
		    yy = map(lambda y:0.01*y,yy)		
		plt.figure(n)
		plt.plot(xx,yy,'.-',linewidth=2,markersize=12,color=cols[algs.index(alg)],clip_on=False)

        plt.figure(n)
	#if(n == 1):
	#     plt.suptitle('Naive Bayes')
	#else:
	#     plt.suptitle('Linear SVM')										
    	plt.title(db,fontsize=16)
        if(datasets.index(db)==0):
    	    plt.legend(algs,'lower right')	
    	plt.xlabel('top k',fontsize=16)
    	plt.ylabel('Classification Accuracy',fontsize=16)
   	plt.xlim([0,100])
    	plt.grid()
   	n+=1

plt.show()
