package CPSC5600;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * PrefixSum.java
 * Sunny Yeung
 * This is the testing file with the main for GeneralScan testing.
 * Extended from GeneralScan for our heap class.
 */

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

        //this constructor for making every padded to the power of 2.
        public SumScan(ArrayList<Double> raw, Double d,int threshold) {
            super(raw,d,threshold);
        }
        public SumScan(ArrayList<Double> raw) {
            super(raw);
        }
        public SumScan(ArrayList<Double> raw, int threshold){
            super(raw,threshold);
        }

        /**
         * Overridden methods below that override the parent class functions
         * @return a new tally
         */
        @Override
        protected Tally init(){
            return new Tally();
        }

        /**
         * Returns the tally with the double type
         * @param datum the data element
         * @return tally with double data
         */
        @Override
        protected Tally prepare(Double datum) {
            return new Tally(datum);
        }

        /**
         * Combine function using our doubles
         * @param left the left tally
         * @param right the right tally
         * @return the combined tally
         */
        @Override
        protected Tally combine(Tally left, Tally right) {
            return Tally.combine(left,right);
        }

        /**
         * Adds data to our tally object
         * @param tally the tally
         * @param datum the data
         */
        @Override
        protected void accum(Tally tally, Double datum) {
            tally.accum(datum);
        }
    }

    public static void main(String[] args) {
        int bit = 1<<8;
        ArrayList<Double> temp =  generate(bit);
        ArrayList<Observation> gridData = new ArrayList<>();

        SumScan test = new SumScan(temp,100);
        System.out.println(test.getReduction().d + " am the reduction.");
        List<Tally> out = test.getScan();
        for (int i =0; i<out.size();i++) {
            if (i%100==0) {
                System.out.println(out.get(i).d);
            }
        }
        System.out.println(out.get(out.size()-1).d);

    }

    /**
     * generates an array filled values
     * @param size of the array
     * @return the array generated
     */
    private static ArrayList<Double> generate(int size) {
        Random rand = new Random();
        ArrayList<Double> temp = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            //temp.add(rand.nextDouble());
            temp.add(1.0);
        }
        return temp;
    }
}
