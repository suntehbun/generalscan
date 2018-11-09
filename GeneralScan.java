package CPSC5600;

import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * Sunny Yeung
 * First port
 */


public class GeneralScan<ElemType, TallyType> {

    public static final int N_THREADS = 16;
    private final int ROOT = 0;
    public static final int THREAD_THRESHOLD = 10000;
    private int threshold;

    private ArrayList<ElemType> data;
    private ArrayList<TallyType> interior;
    private ArrayList<TallyType> output;
    ForkJoinPool pool;


    private boolean reduced;
    public int n; //for size
    private int height;

    /*
        general
     */
    GeneralScan(ArrayList<ElemType> data) {

        this.data=data;
        n = data.size();
        double temp = Math.log(n)/Math.log(2);
        height = (int)Math.ceil(temp);
        this.interior = new ArrayList<>(n-1);
        for (int i = 0; i <  (n-1);i++){
            interior.add(init());
        }
        reduced = false;
        if (1<<height!=n) {
            throw new java.lang.RuntimeException("data must be power of 2 for now"); //fixme
        }
        threshold=THREAD_THRESHOLD;
        this.pool= new ForkJoinPool();
    }

    /**
     * step 3
     * @param data
     * @param threshold
     */
    GeneralScan(ArrayList<ElemType> data, int threshold) {
        this.threshold=threshold;
        this.data = data;
        n = data.size();
        double temp = Math.log(n) / Math.log(2);
        height = (int) Math.ceil(temp);
        this.interior = new ArrayList<>(n - 1);
        //need to pad the interior as well as the data nodes
        for (int i = 0; i < (n - 1); i++) {
            interior.add(init());
        }
        reduced = false;
//        if (1 << height != n) {
//            throw new java.lang.RuntimeException("data must be power of 2 for now"); //fixme
//        }
        pool = new ForkJoinPool();
    }
    @SuppressWarnings("serial")
    class ComputeReduction extends RecursiveAction {
        private int i;

        public ComputeReduction(int i) {
            this.i=i;
        }

        @Override
        protected void compute() {
            if (!isLeaf(i)) {
                if(dataSplit(i)) {
                    invokeAll(new ComputeReduction(left(i)),new ComputeReduction(right(i)));
                } else {
                    reduce(i);
                }
                interior.set(i,combine(value(left(i)),value(right(i))));
            }
        }
//        protected void compute() {
//            if (!isLeaf(i)) {
//                if(dataPosition(i)<=threshold) {
//                    invokeAll(new ComputeReduction(left(i)),new ComputeReduction(right(i)));
//                } else {
//                    reduce(i);
//                }
//                interior.set(i,combine(value(left(i)),value(right(i))));
//            }
//        }
    }
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
                if (dataSplit(i)) {
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

    TallyType getReduction() {
//        reduced= reduced || reduce(ROOT);
//        return value(ROOT);
        if(!reduced) {
            pool.invoke(new ComputeReduction(ROOT));
            reduced=true;
        }
        return value(ROOT);
    }


//    void getScan(ArrayList<TallyType> output) {
//        reduced = reduced || reduce(ROOT);
//        scan(ROOT,init(),output);
//    }

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
//
//    private int parent(int i) {
//        return (i-1)/2;
//    }

    private int right(int i) {
        return left(i)+1;
    }

    private int left(int i) {
        return i*2+1;
    }

    private boolean isLeaf(int i) {
        return right(i) >=size();
    }

    private boolean reduce(int i) {
        if (!isLeaf(i)) {
            reduce(left(i));
            reduce(right(i));
            interior.set(i,combine(value(left(i)),value(right(i))));
        }
        return true;
    }

    private void scan(int i, TallyType tallyPrior, ArrayList<TallyType> out) {
        if (isLeaf(i)) {
            out.set(i-(n-1) , combine(tallyPrior, value(i)));
        } else {
            scan(left(i), tallyPrior, out);
            scan(right(i), combine(tallyPrior,value(left(i))),out);
        }
    }

    private boolean dataSplit(int i) {
        return i%threshold==0;
    }

    //add more padding stuff LATER
    private void makePower2() {
        int nNew = 1<<height;
        int dif = nNew-n;
        for (int i = 0; i<dif;i++) {
        }
    }
}
