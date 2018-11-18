package CPSC5600;

import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Sunny Yeung
 * GeneralScan.java
 * This program is a hybrid of parallel/sequential implementation that uses the Schwartz
 * loop to help with performing calculations when the work area is small and forks when
 * the work operations are big.
 */


public class GeneralScan<ElemType, TallyType> {

    public static final int N_THREADS = 16;
    private final int ROOT = 0;
    public static final int THREAD_THRESHOLD = 10000;
    private int threshold;          //to split the work later

    private ArrayList<ElemType> data;
    public ArrayList<TallyType> interior;
    ForkJoinPool pool;      //pool of threads


    private boolean reduced;
    public int n; //for size
    private int height;
    private int firstDataIndex; //index for first data element in arraylist

    /**
     * Initial constructor
     * @param data
     */
    GeneralScan(ArrayList<ElemType> data) {
        this.data=data;
        n = data.size();
        ElemType test =  data.get(0);
        double temp = Math.log(n)/Math.log(2);
        height = (int)Math.ceil(temp);
        this.interior = new ArrayList<>(n-1);
        for (int i = 0; i < (n-1); i++){
            interior.add(init());
        }
        reduced = false;
//        if (1<<height!=n) {
//            throw new java.lang.RuntimeException("data must be power of 2 for now");
//        }
        threshold=THREAD_THRESHOLD;
        this.pool= new ForkJoinPool();
        firstDataIndex = (1<<height) - 1;

    }

    /**
     * step 3
     * @param data the data arrray
     * @param threshold with choice of threshold
     */
    GeneralScan(ArrayList<ElemType> data, int threshold) {
        this.threshold=threshold;
        this.data = data;
        n = data.size();
        double temp = Math.log(n) / Math.log(2);
        height = (int) Math.ceil(temp);
        this.interior = new ArrayList<>(n - 1);
        //need to pad the interior as well as the data nodes
        for (int i = 0; i < (n-1); i++) {
            interior.add(init());
        }
        reduced = false;
        if (1 << height != n) {
            throw new java.lang.RuntimeException("data must be power of 2 for now"); //fixme
        }
        pool = new ForkJoinPool();
        firstDataIndex = (1<<height) - 1;
    }

    /**
     * For padding to power of 2
     * @param data the data array
     * @param e the element to be added (0.0) in this case
     */
    GeneralScan(ArrayList<ElemType> data,ElemType e, int threshold) {
        this.data=data;
        n = data.size();
        double temp = Math.log(n)/Math.log(2);
        height = (int)Math.ceil(temp);
        this.interior = new ArrayList<>(n-1);
        for (int i = 0; i <  (n-1);i++){
            interior.add(init());
        }
        reduced = false;
        makePower2(e);
        threshold=THREAD_THRESHOLD;
        this.pool= new ForkJoinPool();
        this.firstDataIndex=1<<height-1;
        this.threshold=threshold;
    }

    @SuppressWarnings("serial")
    class ComputeReduction extends RecursiveAction {
        private int i;

        public ComputeReduction(int i) {
            this.i=i;
        }

        protected void compute() {
            if (!isLeaf(i)) {
                if(dataCount(i)>threshold) {
                    invokeAll(new ComputeReduction(left(i)),new ComputeReduction(right(i)));
                    interior.set(i,combine(value(left(i)),value(right(i))));
                } else {
                    reduce(i);
                }
            }
        }
    }

    /**
     * Computing scan class that computes the scan using forkjoinpool and
     * forking when the data element is past the threshold
     */
    @SuppressWarnings("serial")
    class ComputeScan extends RecursiveAction {
        private int i;
        private TallyType tallyPrior;
        private ArrayList<TallyType> out;

        public ComputeScan(int i, TallyType tallyPrior, ArrayList<TallyType> out) {
            this.i=i;
            this.tallyPrior=tallyPrior;
            this.out=out;
        }

        @Override
        protected void compute() {
            if (isLeaf(i)) {
                out.set(i-(n-1), combine(tallyPrior,value(i)));
            } else {
                if (dataCount(i)>threshold) {
                    invokeAll(
                        new ComputeScan(left(i), tallyPrior, out),
                        new ComputeScan(right(i),combine(tallyPrior,value(left(i))),out)
                    );
                }
                else {
                    scan(i,tallyPrior,out);
                }
            }
        }
    }

    /**
     * version 2 that uses forkjoinpool to get the reduction
     * @return the reduction.
     */
    TallyType getReduction() {
        if(!reduced) {
            pool.invoke(new ComputeReduction(ROOT));
            reduced=true;
        }
        return value(ROOT);
    }

    /**
     * version 2 that retrieves scan values using forkjoinpool
     * @return the output scan arraylist
     */
    ArrayList<TallyType> getScan() {
        if(!reduced) {
            getReduction();
        }

        ArrayList<TallyType> output= new ArrayList<>();
        for (int i = 0; i<data.size(); i++) {
            output.add(init());
        }
        pool.invoke(new ComputeScan(ROOT,init(),output));
        return output;
    }

    //functions to be overridden below
    protected TallyType init() {
        throw new IllegalArgumentException("nope this is bad data type");
    }

    protected TallyType prepare(ElemType datum) {
        throw new IllegalArgumentException("prepare is bad");
    }

    protected TallyType combine(TallyType left, TallyType right) throws IllegalArgumentException {
        throw new IllegalArgumentException("combine is bad");
    }
    protected void accum(TallyType tally, ElemType datum) {
        throw new IllegalArgumentException("accum is bad");
    }


    private int size() {
        return (n-1) + n;
    }

    private TallyType value(int i) {
        if (i<n-1) {
            return interior.get(i);
        } else {
            return prepare(data.get(i-(n-1)));
        }
    }

    /**
     * Will be used for interior node reducing
     * @param i the index to return to
     * @return the parent
     */
    private int parent(int i) {
        return (i-1)/2;
    }

    /**
     * returns right child index
     * @param i the index
     * @return the child index
     */
    private int right(int i) {
        return left(i)+1;
    }

    /**
     * returns left child index
     * @param i the index
     * @return the left child index
     */
    private int left(int i) {
        return i*2+1;
    }

    /**
     * if the value of the right child is bigger than current size, its in the data
     * section since n +n-1.
     * @param i the index
     * @return true if a a leaf
     */
    private boolean isLeaf(int i) {
        return right(i) >=size();
    }

    /**
     * checks to see if there is anything on the right child.
     * @param i the index to check
     * @return if there is a child.
     */
    protected boolean hasRight(int i) {
        return right(i) < size();
    }

    /**
     * returns the first data index in tree structure
     * @param i the index currently
     * @return the index of the first data element
     */
    protected int getFirstDataIndex(int i) {
        //if at a leaf can't go down more.
        if(isLeaf(i)){
            return i < firstDataIndex ? -1 : i;
        }
        return getFirstDataIndex(left(i));
    }

    /**
     * returns the last data index for data splitting ease
     * @param i the index of the arraylist
     * @return the last data index
     */
    protected int getLastDataIndex(int i) {
        if (isLeaf(i)) {
            return i < firstDataIndex ? -1 : i;
        }
        if (hasRight(i)) {
            int right = getLastDataIndex(right(i));
            if (right != -1) {
                return right;
            }
        }
        return getLastDataIndex(left(i));
    }

    /**
     * version 2/3 reduce that uses accum instead. Eventually will use
     * parent method to transfer to interior node more.
     * @param i the index of the arraylist
     */
    protected void reduce(int i) {
        int first = getFirstDataIndex(i);
        int last = getLastDataIndex(i);
        TallyType tally = init();
        if (first!= -1) {
            for (int j = first; j<=last; j++) {
                accum(tally,data.get(i));
            }
            interior.set(i,tally);
        }
    }

    /**
     * version 2/3 scan that uses
     * @param i index of node
     * @param tallyPrior the prior tally for later calculations
     * @param out the output array with our scans
     */
    protected void scan(int i, TallyType tallyPrior, ArrayList<TallyType> out) {
        int first = getFirstDataIndex(i);
        int last = getLastDataIndex(i);
        //if not
        if (first != -1) {
            for (int j = first; j <= last; j++) {
                tallyPrior = combine(tallyPrior, value(j));
                out.set(j - firstDataIndex, tallyPrior);
            }
        }
    }

    //get data within their range.
    private int dataCount(int i) {
        return getLastDataIndex(i)-getFirstDataIndex(i);
    }


    //add more padding stuff
    protected void makePower2(ElemType e) {
        int nNew = 1<<height;
        int dif = nNew-n;
        for (int i = 0; i<dif;i++) {
            data.add(e);
            interior.add(init());
        }
        n=nNew;
    }
}
