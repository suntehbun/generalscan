/**
 * Sunny Yeung
 * HeatMap.java
 * This file is the driver program that runs general scan as well as our general reduce.
 * It has a static tally class as well as a static handcraftedscan class which keeps tracks of timestamps
 * and a way to parallelize the second pass through. It uses hashmaps to store timestamps as well as calculating the scan.
 */

package CPSC5600;

import java.io.*;
import java.util.ArrayList;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JButton;
import javax.swing.JFrame;

public class HeatMap {
    private static final int DIM = 20;      //size of our dimensions for the array
    private static final String REPLAY = "Replay";
    private static JFrame application;
    private static JButton button;
    private static Color[][] grid;          //the display color grid
    private static Tally reduced;           //for our first pass tally for reduction
    private static long CURRENT = 1;        //because our timestamp starts at 1 second
    private static Map<Long,int[]> heatmaps = new HashMap<>();



    static class Tally {

        public Map<Long,ArrayList<Observation>> all;    //for storing into our map first time
        public Map<Long, TimeStamp> collection;         //stores map matching timestamp second

        private int[]cells;
        private Object[] history;
        private int front, back;

        /**
         * Tally for part 2 that holds a collection of int array per timestamp seconds
         */
        public Tally() {
            collection = new HashMap<>();
            all = new HashMap<>();
        }


        public Tally(Observation o) {
            this();
            accum(o);
        }

        /**
         * Combine function which is not used for this part 2.
         * Used in last hw for the extra credit
         * @param a the first tally
         * @param b the second tally
         * @return the new combined tally
         */
        public static Tally combine(Tally a, Tally b) {
            Tally tally = new Tally();
            TimeStamp stamp;
            long time;
            for (Map.Entry<Long,TimeStamp> temp : a.collection.entrySet()) {
                stamp = temp.getValue();
                time = temp.getKey();
                if (b.collection.containsKey(time)) {
                    TimeStamp other = b.collection.get(time);
                    int combined[] = stamp.combine(other);
                    other.setCells(combined);
                    b.collection.put(time,other);
                } else {
                    b.collection.put(time,stamp);
                }
            }
            tally.setCollection(b.collection);
            return tally;
        }

        /**
         * This function adds an observation into our map by checking if its timestamp
         * is relevant, if not it adds the timestamp to that map being built.
         * @param o
         */
        public void accum(Observation o) {
            if (o!=null) {
                long time = o.time;
                if(all.containsKey(time)&&collection.containsKey(time)) {
                    all.get(time).add(o);
                    collection.get(time).incrCell(place(o.x),place(o.y));
                } else {
                    ArrayList<Observation> timestamp = new ArrayList<>();
                    timestamp.add(o);
                    all.put(time,timestamp);
                    TimeStamp t = new TimeStamp(DIM);
                    t.incrCell(place(o.x),place(o.y));
                    collection.put(time,t);
                }
            }
        }

        public void setCollection(Map<Long,TimeStamp> other) {
            this.collection=other;
        }

        /**
         * places an observation in the correct place in our dim dim array
         * @param val the location of the point
         * @return the location on the array
         */
        public int place(double val) {
            return (int) ((val + 1.0) / (2.0/DIM));
        }

        public String toString() {
            String aString = "";
            for (int i = 1; i<collection.size()+1;i++) {
                Long aLong = (long) i;
                TimeStamp t = collection.get(aLong);
                int[] temp = t.cells;
                for(int j = 0; j<temp.length;j++) {
                    aString += temp[j];
                }
            }
            return aString;
        }
    }

    /**
     * custom thread class for implementing the new scan.
     * It scans based on a map and returns decay at a point.
     */
    static class HandCraftedScan implements Runnable {
        Long time;
        long decay = 3; //arbitrary decay threshold
        int[] cells;
        Map<Long, TimeStamp> collection;

        public HandCraftedScan(HeatMap.Tally tally, int time) {
            this.time=(long) time;
            this.collection=tally.collection;
            this.cells=collection.get(this.time).cells;
        }

        @Override
        public void run() {
            Long min =  (time - decay);
            if(min<=0) {
                min = 1L;
            }
            int counter = 1;
            for (Long i = time; i>min; i--) {
                weight(collection.get(i).cells,counter);
                counter++;
            }
            heatmaps.put(time,cells);
        }

        /**
         * This function applies a history weighting where it divides
         * by the weight to get the weight.
         * @param timeAtCell
         * @param weight
         */
        private void weight(int[] timeAtCell, int weight) {
            for (int i = 0; i< timeAtCell.length;i++) {
                cells[i]+=timeAtCell[i]/weight;
            }
        }
    }

    static class HeatScan extends GeneralScan<Observation, Tally> {
        public HeatScan(ArrayList<Observation> data) {
            super(data);
        }

        @Override
        protected Tally init() {
            return new Tally();
        }

        @Override
        protected Tally prepare(Observation o) {
            return new Tally(o);
        }

        @Override
        protected Tally combine(Tally left, Tally right) {
            return Tally.combine(left, right);
        }

        @Override
        protected void accum(Tally tally, Observation o) {
            tally.accum(o);
        }
    }

    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        //creates a list of observation
        ArrayList<Observation> gridData = new ArrayList<>();
        uniformSpray();
        readFile(gridData);

        //initialize our general scan
        HeatScan scan = new HeatScan(gridData);
        ExecutorService threadPool = Executors.newCachedThreadPool();

        //this gets the reduce after our first pass
        reduced = scan.getReduction();

        /**
         * Handcrafted parallel process below since it doesn't write to the same
         * place in the array, we can afford to call multiple creates at the same
         * time.
         */
        for (int i = 1; i<=reduced.collection.size();i++) {
            threadPool.execute(new HandCraftedScan(reduced,i));
        }

        //code snippets lifted from the other griddemo to animate our sample
        grid = new Color[DIM][DIM];
        application = new JFrame();
        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fillGrid(grid);

        ColoredGrid gridPanel = new ColoredGrid(grid);
        application.add(gridPanel, BorderLayout.CENTER);

        button = new JButton(REPLAY);
        button.addActionListener(new BHandler());
        application.add(button, BorderLayout.PAGE_END);

        application.setSize(DIM * DIM, DIM * DIM);
        application.setVisible(true);
        application.repaint();
        animate();
    }

    /**
     * This generates a dataset and puts it into a file. Lifted from the observation file.
     * @throws FileNotFoundException
     */
    public static void uniformSpray() {
        final String FILENAME = "obs_uniform_spray.dat";
        //generates
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILENAME));
            int t = 0;
            for (double r = -.95; r <= .95; r += 0.1) {
                t++; // each row has the same value of t so we can see differences in step 7
                for (double c = -.95; c <= .95; c += 0.1) {
                    out.writeObject(new Observation(t,r,-r));
                    out.writeObject(new Observation(t,-r,r));
                    out.writeObject(new Observation(t,r,r));
                    out.writeObject(new Observation(t,-r,-r));
                    out.writeObject(new Observation(t,0,-r));
                    out.writeObject(new Observation(t,-r,0));
                    out.writeObject(new Observation(t,0,r));
                    out.writeObject(new Observation(t,r,0));
                }
            }
            for (double r = -.95; r <= .95; r += 0.1) {

                out.writeObject(new Observation(t,0,0));
                t++; // each row has the same value of t so we can see differences in step 7
                for (double c = -.95; c <= .95; c += 0.1) {

                    if (c>0 && r > 0) {
                        out.writeObject(new Observation(t, r, -r));
                        out.writeObject(new Observation(t, -r, r));
                        out.writeObject(new Observation(t, r, r));
                        out.writeObject(new Observation(t, -r, -r));
                        out.writeObject(new Observation(t, 0, -r));
                        out.writeObject(new Observation(t, -r, 0));
                        out.writeObject(new Observation(t, 0, r));
                        out.writeObject(new Observation(t, r, 0));
                    }
                }
            }
            out.writeObject(new Observation());  // to mark EOF
            out.close();
        } catch (IOException e) {
            System.out.println("writing to " + FILENAME + "failed: " + e);
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("wrote " + FILENAME);
    }

    /**
     * This function reads in a file and puts all the data in the observations into an arraylist for processing
     * @param data the arraylist to be filled in
     * @throws FileNotFoundException
     */
    public static void readFile(ArrayList<Observation> data) throws FileNotFoundException {
        final String FILENAME = "obs_uniform_spray.dat";
        //this reads the file below
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILENAME));
            Observation obs = (Observation) in.readObject();
            while (!obs.isEOF()) {
                obs = (Observation) in.readObject();
                if(!obs.isEOF()) {
                    data.add(obs);
                }
            }
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("reading from " + FILENAME + "failed: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * This class acts as a thread that starts another animation if the user presses the replay button
     */
    static class BHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (REPLAY.equals(e.getActionCommand())) {
                CURRENT=1;
                new Thread() {
                    public void run() {
                        try {
                            animate();
                        } catch (InterruptedException e) {
                            System.exit(0);
                        }
                    }
                }.start();
            }
        }
    }

    /**
     * Animating function that shows our grid being filled in at each timestamp
     * @throws InterruptedException
     */
    private static void animate() throws InterruptedException {
        button.setEnabled(false);
        //do it for reduced size as thats the timestamps amount per second.
        for (int i = 0; i < reduced.collection.size(); i++) {
            fillGrid(grid);
            CURRENT++;
            application.repaint();
            Thread.sleep(50);
        }
        button.setEnabled(true);
        application.repaint();
    }
    //colors to represent our hits and non hits.
    static private final Color COLD = new Color(0x0a, 0x37, 0x66), HOT = Color.RED;

    /**
     * This function fills in the grid object with consists of multiple color objects, using our observed hits.
     * @param grid the grid to be filled with colors.
     */
    private static void fillGrid(Color[][] grid) {
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                grid[r][c] = interpolateColor(heatmaps.get(CURRENT)[r * DIM + c], COLD, HOT);
            }
        }
    }

    /**
     * This method fills the color for the specific point in the grid by grasping the red, blue, green of a point.
     * @param ratio the point's observed hits
     * @param a the black color
     * @param b the red color
     * @return the color object created with the new color points.
     */
    private static Color interpolateColor(double ratio, Color a, Color b) {
        if (ratio>=20.0) {
            ratio=1;
        } else if (ratio>15.0) {
            ratio = .75;
        } else if (ratio>5) {
            ratio = .5;
        } else if (ratio > 0) {
            ratio = .25;
        } else {
            ratio = 0;
        }
        int ax = a.getRed();
        int ay = a.getGreen();
        int az = a.getBlue();
        int cx = ax + (int) ((b.getRed() - ax) * ratio);
        int cy = ay + (int) ((b.getGreen() - ay) * ratio);
        int cz = az + (int) ((b.getBlue() - az) * ratio);
        return new Color(cx, cy, cz);
    }
}
