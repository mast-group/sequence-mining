# Plot itemset precision-recall
import matplotlib.pyplot as plt
from matplotlib import rc
import numpy as np

rc('xtick', labelsize=16) 
rc('ytick', labelsize=16) 

def main():
    
    path = '/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Sequences/PrecisionRecall/Parallel/'
    probname = 'parallel'    
    cols = ['b','g','m']
    prefixes = ['ISM','SQS', 'GoKrimp']
    
    for prefix in prefixes:
    
        precision, recall = readdata(open(path+prefix+'_'+probname+'_pr.txt'))
	col = cols[prefixes.index(prefix)]
        
	plt.figure(2)
        plt.hold(True)
        plt.plot(range(1,len(recall)+1),precision,'.-',color=col,linewidth=2,markersize=12,clip_on=False)
        plt.xlabel('top k',fontsize=16)
        plt.ylabel('Precision',fontsize=16)
	plt.xlim([0,50])
        plt.ylim([0,1])
        plt.grid(True)

        plt.figure(3)
        plt.hold(True)
        plt.plot(range(1,len(recall)+1),recall,'.-',color=col,linewidth=2,markersize=12,clip_on=False)
        plt.xlabel('top k',fontsize=16)
        plt.ylabel('Recall',fontsize=16)
        plt.xlim([0,50])
        plt.ylim([0,1])
        plt.grid(True)

        # Calculate interpolated precision
        pt_recall = np.arange(0,1.1,0.1)
        interp_precision = [pinterp(zip(precision,recall),r) for r in pt_recall]
        plotfigpr(interp_precision,pt_recall,probname,col,1)
        
    plt.figure(1)   
    plt.legend(prefixes,'lower right')

    plt.figure(2)   
    plt.legend(prefixes,'lower right')

    plt.figure(3)   
    plt.legend(prefixes,'lower right')

    plt.show()


# Interpolate precision
def pinterp(prarray,recall):

    m = [p for (p,r) in prarray if r >= recall]
    if(len(m)==0):
        return np.nan
    else:
        return max(m) 

def plotfigpr(precision,recall,probname,col,figno):

    # sort
    ind = np.array(recall).argsort()
    r_d = np.array(recall)[ind]
    p_d = np.array(precision)[ind]

    plt.figure(figno)
    plt.hold(True)
    plt.plot(r_d,p_d,'.-',color=col,linewidth=2,markersize=12,clip_on=False)
    #plt.title(probname+' top-k precison-recall')
    plt.xlabel('Recall',fontsize=16)
    plt.ylabel('Precision',fontsize=16)
    plt.xlim([0,1])
    plt.ylim([0,1])
    plt.grid(True)

def readdata(fl):
   
    for line in fl:
      if 'Precision' in line:
	pre = line.strip().split(': ')[1].replace('[','').replace(']','').split(', ')
      if 'Recall' in line:
	rec = line.strip().split(': ')[1].replace('[','').replace(']','').split(', ')

    return (map(float,pre),map(float,rec))

main()
