package CPSC5600;

public class TimeStamp {
    int[] cells;
    int size;

    public TimeStamp(int size) {
         this.size = size;
         cells = new int[size*size];
    }


    public void incrCell(int row, int col) {
        cells[row * size + col]++;
    }
    public int place(double val) {
        return (int) ((val + 1.0) / (2.0/size));
    }

    public int getCell(int row, int col) {
        return cells[row * size + col];
    }

    public String toString() {
        String test = "";
        for (int i : cells) {
            test += " " + i;
        }
        return test;
    }


}
