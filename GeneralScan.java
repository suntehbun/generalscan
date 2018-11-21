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

    private final int ROOT = 0;
    public static final int THREAD_THRESHOLD = 10000;
    protected int threshold;          //to split the work later

    protected ArrayList<ElemType> data;
    public ArrayList<TallyType> interior;
    ForkJoinPool pool;      //pool of threads

    protected boolean reduced;
    protected int n; //for size
    protected int height;
    protected int firstDataIndex; //index for first data element in arraylist

    /**
     * Initial constructor
     * @param data
     */
    GeneralScan(ArrayList<ElemType> data) {
        reduced = false;
        n = data.size();
        this.data = data;
        height = 0;
        while ((1<<height) < n)
            height++;
        firstDataIndex = (1<<height) - 1;
        threshold = THREAD_THRESHOLD;
        int m = 4 * (1 + firstDataIndex/threshold);
        interior = new ArrayList<>(m);
        for (int i = 0; i < m; i++)
            interior.add(init());
        pool = new ForkJoinPool();
    }

    /**
     * step 3
     * @param data the data arrray
     * @param threshold with choice of threshold
     */
    GeneralScan(ArrayList<ElemType> data, int threshold) {
        reduced=false;
        this.threshold=threshold;
        this.data = data;
        n = data.size();
        height = 0;
        while ((1<<height) < n)
            height++;
        firstDataIndex = (1<<height) - 1;
        int m = 4 * (1 + firstDataIndex/threshold);
        interior = new ArrayList<>(m);
        for (int i = 0; i < m; i++)
            interior.add(init());
        pool = new ForkJoinPool();
    }

    /**
     * For padding to power of 2
     * @param data the data array
     * @param e the element to be added (0.0) in this case
     */
    GeneralScan(ArrayList<ElemType> data,ElemType e, int threshold) {
        this.data=data;
        n = data.size();
        this.threshold=threshold;
        int checker = n/threshold;
        int modChecker = n%threshold;

        double temp = Math.log(checker)/Math.log(2);
        height = (int)Math.ceil(temp);
        reduced = false;
        this.pool= new ForkJoinPool();

        if (1 << height != n/threshold || modChecker!=0) {
            int m = calculateNext2(n/threshold);
            m = 1<<m;
            makePower2(e);
            //System.out.println("m = " + m);
            this.interior = new ArrayList<>(m-1);
            for (int i = 0; i <  m;i++){
                System.out.println("squaring m " + i);
                interior.add(init());
            }
        } else {
            this.interior = new ArrayList<>();
            for (int i = 0; i <  checker;i++){
                System.out.println("nothing has changed");
                interior.add(init());
            }
        }
        this.firstDataIndex=1<<height-1;
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

    protected int size() {
        return firstDataIndex + n;
    }

    protected TallyType value(int i) {
        if (i<firstDataIndex) {
            return interior.get(i);
        } else {
            return prepare(data.get(i-firstDataIndex));
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
                accum(tally, leafValue(j));
            }
            interior.set(i,tally);
        }
    }

    @SuppressWarnings("serial")
    class ComputeReduction extends RecursiveAction {
        private int i;

        public ComputeReduction(int i) {
            this.i=i;
        }

        protected void compute() {
            if (dataCount(i) < threshold) {
                reduce(i);
                return;
            }
            invokeAll(
                    new ComputeReduction(left(i)),
                    new ComputeReduction(right(i)));
            interior.set(i, combine(value(left(i)), value(right(i))));
        }
    }


    /**
     * version 2 that retrieves scan values using forkjoinpool
     * @return the output scan arraylist
     */
    ArrayList<TallyType> getScan() {
        if(!reduced) {
            getReduction();
        }

        ArrayList<TallyType> output= new ArrayList<>(n);
        for (int i = 0; i<data.size(); i++) {
            output.add(init());
        }
        pool.invoke(new ComputeScan(ROOT,init(),output));
        return output;
    }

    ArrayList<TallyType> getScan(double d) {
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

//    protected void scan(int i, TallyType tallyPrior, ArrayList<TallyType> output) {
//        int first = getFirstDataIndex(i), last = getLastDataIndex(i);
//        if (first != -1)
//            for (int j = first; j <= last; j++) {
//                tallyPrior = combine(tallyPrior, value(j));
//                if ((j-firstDataIndex) % 16 == 0 && (j-firstDataIndex)
//                        /16 < data.size()/16) {
//                    output.set((j - firstDataIndex) / 16, tallyPrior);
//                }
//            }
//    }

    /**
     * version 2/3 scan that uses
     * @param i index of node
     * @param tallyPrior the prior tally for later calculations
     * @param out the output array with our scans
     */
    protected void scan(int i, TallyType tallyPrior, ArrayList<TallyType> out) {
        int first = getFirstDataIndex(i);
        int last = getLastDataIndex(i);
        if (first != -1) {
            for (int j = first; j <= last; j++) {
                tallyPrior = combine(tallyPrior, value(j));
                out.set(j - firstDataIndex, tallyPrior);
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
            if (dataCount(i) < threshold) {
                scan(i, tallyPrior, out);
                return;
            }
            invokeAll(
                    new ComputeScan(left(i), tallyPrior, out),
                    new ComputeScan(right(i), combine(tallyPrior, value(left(i))), out));
        }
    }

    protected ElemType leafValue(int i) {
        if (i < firstDataIndex || i >= size())
            throw new IllegalArgumentException("bad i " + i);
        return data.get(i - firstDataIndex);
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
    protected boolean isLeaf(int i) {
        return left(i) >=size();
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

    //get data within their range.
    private int dataCount(int i) {
        return getLastDataIndex(i)-getFirstDataIndex(i);
    }

    private int calculateNext2() {
        int before = n/threshold;
        if (before!=0) {
            int after = 32 - Integer.numberOfLeadingZeros(before - 1);
            if (before == after) {
                return before;
            }
        }
        return before<<1;
    }
    private int calculateNext2(int num) {
        int before = num/threshold;
        if (before!=0) {
            int after = 32 - Integer.numberOfLeadingZeros(before - 1);
            if (before == after) {
                return before;
            }
        }
        return before<<1;
    }

    //add more padding stuff
    protected void makePower2(ElemType e) {
        int m = calculateNext2();
        //System.out.println("i am m " + m);
        int dataPad = 4*threshold;
        //System.out.println("padding by this much " + (dataPad-n));
        for (int i = n; i<dataPad; i++) {
            data.add(e);
        }
        n=data.size();
    }
}
