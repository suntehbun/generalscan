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

    private ArrayList<ElemType> data;
    private ArrayList<TallyType> interior;
    private ArrayList<TallyType> output;
    //private TallyType tallyFactory;


    private boolean reduced;
    public int n; //for size
    private int height;

    GeneralScan(ArrayList<ElemType> data) {

        this.data=data;
        //this.tallyFactory=factory;
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

    }

    TallyType getReduction() {
        reduced= reduced || reduce(ROOT);
        return value(ROOT);
    }

    void getScan(ArrayList<TallyType> output) {
        reduced = reduced || reduce(ROOT);
        scan(ROOT,init(),output);
    }

    ArrayList<TallyType> getScan() {
        if(!reduced) {
            getReduction();
        }

        ArrayList<TallyType> output= new ArrayList<>();
        for (int i = 0; i<data.size(); i++) {
            output.add(init());
        }
        scan(ROOT, init(),output);
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
            if (i<N_THREADS-2) {
                reduce(left(i));
                reduce(right(i));//this will be fork join pool
            } else {
                reduce(left(i));
                reduce(right(i));
            }
            interior.set(i,combine(value(left(i)),value(right(i))));
        }
        return true;
    }

    private void scan(int i, TallyType tallyPrior, ArrayList<TallyType> out) {
        if (isLeaf(i)) {
            out.set(i-(n-1) , combine(tallyPrior, value(i)));
        } else {
            if (i<N_THREADS-2) {
                scan(left(i), combine(tallyPrior,value(left(i))),out);
                scan(right(i), combine(tallyPrior,value(left(i))),out);

            } else {
                scan(left(i), tallyPrior, out);
                scan(right(i), combine(tallyPrior,value(left(i))),out);
            }
        }
    }
}
