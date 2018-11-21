/**
 * Sunny Yeung
 * HeatMap.java
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
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;

public class HeatMap {
    private static final int DIM = 16;
    private static final String REPLAY = "Replay";
    private static JFrame application;
    private static JButton button;
    private static Color[][] grid;
    private static ArrayList<Tally> sums;
    private static int CURRENT = 0;

    static class Tally {
        private int[] cells;
        private Observation[] history;
        private ArrayList<ArrayList<Observation>> queue;
        public Map<Long,ArrayList<Observation>> all;
        private int front, back;
        public Map<Long, TimeStamp> collection;


        public Tally() {
            cells = new int[DIM*DIM];
            history = new Observation[DIM];
            collection = new HashMap<>();
            all = new HashMap<>();
            queue = new ArrayList<>();
            front = 0;
            back = 0;

        }

        public Tally(Observation o) {
            this();
            accum(o);
        }


        public void incrCell(int row, int col) {
            cells[row * DIM + col]++;
        }
        public void decrCell(int row, int col) {
            cells[row * DIM + col]--;
        }

        public static Tally combine(Tally a, Tally b) {
            Tally tally = new Tally();
            ArrayList<Observation> aList;
            for (Map.Entry<Long,ArrayList<Observation>> temp : a.all.entrySet()) {
                aList = temp.getValue();
                for(Observation o : aList) {
                    tally.accum(o);
                }
            }
            for (Map.Entry<Long,ArrayList<Observation>> temp : b.all.entrySet()) {
                aList = temp.getValue();
                for(Observation o : aList) {
                    tally.accum(o);
                }
            }
            return tally;
//            for (int aFront = 0; aFront<a.back; aFront=nextQueue(aFront)) {
//                tally.accum(a.history[aFront]);
//            }
//            for (int bFront = 0; bFront<b.back; bFront=nextQueue(bFront)) {
//                tally.accum(b.history[bFront]);
//            }
//            return tally;
        }

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
//            if (datum != null) {
//                incrCell(place(datum.x),place(datum.y));
//                if (fullQueue()) {
//                    Observation old = deQueue();
//                    if (old != null) {
//                        decrCell(place(old.x),place(old.y));
//                    }
//                }
//                enQueue(datum);
//            }
        }

        private void enQueue(Observation o) {
            if (!fullQueue()) {
                history[back] = o;
                back = nextQueue(back);
            }
        }

        private Observation deQueue() {
            Observation o = null;
            if (!emptyQueue()) {
                o = history[front];
                //history[front] = null;
                front = nextQueue(front);
                return o;
            }
            return null;
        }
        public int place(double val) {
            return (int) ((val + 1.0) / (2.0/DIM));
        }

        public int getCell(int row, int col) {
            return cells[row * DIM + col];
        }
        private boolean fullQueue() {
            return nextQueue(back) == front;
        }
        private boolean emptyQueue() {
            return front==back;
        }

        private static int nextQueue(int i) {
            return (i+1) % (DIM);
        }

        public String toString() {
            String test = "";
            for (int i : cells) {
                test += " " + i;
            }
            return test;
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
        ArrayList<Observation> gridData = new ArrayList<>();
        uniformSpray();
        readFile(gridData);
        System.out.println(gridData.size());
        HeatScan scan = new HeatScan(gridData);
        //Map<Long, ArrayList<Observation>> test = scan.getReduction().all;
        //System.out.println(test.size()+" "+ test);

        Map<Long, TimeStamp> collection = scan.getReduction().collection;
        System.out.println(collection);

        sums = scan.getScan();
        //for (Tally t : sums) { System.out.println(t); }

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
    public static void uniformSpray() throws FileNotFoundException{
        final String FILENAME = "obs_uniform_spray.dat";
        Random rand = new Random();
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILENAME));
            int t = 0;
            for (double r = -1; r <= 1; r += 0.1) {
                t++; // each row has the same value of t so we can see differences in step 7
                for (double c = -1; c <= 1; c += 0.1) {
                    double d = rand.nextDouble()*2 -1;
                    out.writeObject(new Observation(t, r, c));
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

    public static void readFile(ArrayList<Observation> data) throws FileNotFoundException {
        final String FILENAME = "obs_uniform_spray.dat";
        //this reads the file below
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILENAME));
            int count = 0;
            Observation obs = (Observation) in.readObject();
            while (!obs.isEOF()) {
                //System.out.println(++count + ": " + obs);
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
    private static void animate() throws InterruptedException {
        button.setEnabled(false);
        for (int i = 0; i < sums.size(); i++) {
            fillGrid(grid);
            CURRENT++;
            application.repaint();
            Thread.sleep(50);
        }
        button.setEnabled(true);
        application.repaint();
    }

    static class BHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (REPLAY.equals(e.getActionCommand())) {
                CURRENT=0;
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

    static private final Color COLD = new Color(0x0a, 0x37, 0x66), HOT = Color.RED;

    private static void fillGrid(Color[][] grid) {
        int pixels = grid.length * grid[0].length;
        int counter = 0;
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                grid[r][c] = interpolateColor(sums.get(CURRENT).getCell(r, c)%DIM*DIM, COLD, HOT);
            }
        }
    }

    private static Color interpolateColor(double ratio, Color a, Color b) {
        ratio = Math.min(ratio, 1.0);
        int ax = a.getRed();
        int ay = a.getGreen();
        int az = a.getBlue();
        int cx = ax + (int) ((b.getRed() - ax) * ratio);
        int cy = ay + (int) ((b.getGreen() - ay) * ratio);
        int cz = az + (int) ((b.getBlue() - az) * ratio);
        return new Color(cx, cy, cz);
    }
}
