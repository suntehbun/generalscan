package CPSC5600;

import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class PrefixSum {

    static class Tally {
        public double d;

        public Tally() {
            d = 0.0;
        }

        public Tally(double d) {
            this.d = d;
        }

        public static Tally combine(Tally a, Tally b) {
            return new Tally(a.d + b.d);
        }

        public void accum(double datum) {
            this.d += datum;
        }
    }
    //this is the heap class
    static class SumScan extends GeneralScan<Double, Tally> {

        public SumScan(ArrayList<Double> raw) {
            super(raw);
        }

        @Override
        protected Tally init(){
            return new Tally();
        }
        @Override
        protected Tally prepare(Double datum) {
            return new Tally(datum);
        }
        @Override
        protected Tally combine(Tally left, Tally right) {
            return Tally.combine(left,right);
        }
        @Override
        protected void accum(Tally tally, Double datum) {
            tally.accum(datum);
        }
    }



    public static void main(String[] args) {
        ArrayList<Double> temp =  generate(1<<6);
        //for (Double d : temp) { System.out.println(d); }

        SumScan test = new SumScan(temp);
        //System.out.println(test.n);
        Tally test1 = test.init();
        System.out.println(test.getReduction().d + " am the reduction.");
        ArrayList<Tally> out = test.getScan();
        for (Tally d : out) { System.out.println(d.d); }
        System.out.println("end of serial\n");
        ForkJoinPool pool = new ForkJoinPool();

    }

    private static ArrayList<Double> generate(int size) {
        ArrayList<Double> temp = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            temp.add(1.0);
        }
        return temp;
    }
}
