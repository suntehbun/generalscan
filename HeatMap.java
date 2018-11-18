package CPSC5600;

import java.io.*;
import java.util.ArrayList;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;

import javax.swing.JButton;
import javax.swing.JFrame;

public class HeatMap {
    private static final int DIM = 150;
    private static final String REPLAY = "Replay";
    private static JFrame application;
    private static JButton button;
    private static Color[][] grid;

    static class Tally {
        private int[] cells;
        public double x,y;

        public Tally() {
            x=0.0;
            y=0.0;
        }

        public Tally(Observation o) {
            this.x=o.x;
            this.y=o.y;
        }

        public Tally(double x, double y) {
            this.x=x;
            this.y=y;
        }

        public static Tally combine(Tally a, Tally b) {
            return new Tally(a.x + b.x, a.y + b.y);
        }

        public void accum(double datum) {
            this.x += datum;
        }

        public String toString() {
            return "x: " + x + " y: " + y;
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
            tally.x+=o.x;
            tally.y+=o.y;
        }
    }
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        ArrayList<Observation> gridData = new ArrayList<>();
        uniformSpray();
        readFile(gridData);
        System.out.println(gridData.size());
        HeatScan scan = new HeatScan(gridData);
        ArrayList<Tally> sums = scan.getScan();
        for (Tally t : sums) { System.out.println(t); }
        System.out.println(sums.size());

        grid = new Color[DIM][DIM];
        application = new JFrame();
        application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fillGrid(grid);

        ColoredGrid gridPanel = new ColoredGrid(grid);
        application.add(gridPanel, BorderLayout.CENTER);

        button = new JButton(REPLAY);
        button.addActionListener(new BHandler());
        application.add(button, BorderLayout.PAGE_END);

        application.setSize(DIM * 4, (int)(DIM * 4));
        application.setVisible(true);
        application.repaint();
        animate();

    }
    public static void uniformSpray() {
        final String FILENAME = "obs_uniform_spray.dat";
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILENAME));
            int t = 0;
            for (double r = -0.95; r <= 0.95; r += 0.125) {
                t++; // each row has the same value of t so we can see differences in step 7
                for (double c = -0.95; c <= 0.95; c += 0.125)
                    out.writeObject(new Observation(t, c, r));
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

    public static void readFile(ArrayList<Observation> data) {
        final String FILENAME = "obs_uniform_spray.dat";
        //this reads the file below
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILENAME));
            int count = 0;
            Observation obs = (Observation) in.readObject();
            while (!obs.isEOF()) {
                System.out.println(++count + ": " + obs);
                obs = (Observation) in.readObject();
                data.add(obs);
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
        for (int i = 0; i < DIM; i++) {
            fillGrid(grid);
            application.repaint();
            Thread.sleep(50);
        }
        button.setEnabled(true);
        application.repaint();
    }

    static class BHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (REPLAY.equals(e.getActionCommand())) {
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
    };

    static private final Color COLD = new Color(0x0a, 0x37, 0x66), HOT = Color.RED;
    static private int offset = 0;

    private static void fillGrid(Color[][] grid) {
        int pixels = grid.length * grid[0].length;
        for (int r = 0; r < grid.length; r++)
            for (int c = 0; c < grid[r].length; c++)
                grid[r][c] = interpolateColor((r*c+offset)%pixels / (double)pixels, COLD, HOT);
        offset += DIM;
    }

    private static Color interpolateColor(double ratio, Color a, Color b) {
        int ax = a.getRed();
        int ay = a.getGreen();
        int az = a.getBlue();
        int cx = ax + (int) ((b.getRed() - ax) * ratio);
        int cy = ay + (int) ((b.getGreen() - ay) * ratio);
        int cz = az + (int) ((b.getBlue() - az) * ratio);
        return new Color(cx, cy, cz);
    }
}