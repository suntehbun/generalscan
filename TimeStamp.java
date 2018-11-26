/**
 * Sunny Yeung
 * TimeStamp.java
 * This class is a wrapper class to store as an object for our grid dim dim array.
 */

package CPSC5600;

public class TimeStamp {
    int[] cells;    //the dim dim array
    int size;

    public TimeStamp(int size) {
         this.size = size;
         cells = new int[size*size];
    }

    /**
     * Increments the cell location based on whether or not there was a hit from an observation
     * @param row the row to increment
     * @param col the column to increment
     */
    public void incrCell(int row, int col) {
        cells[row * size + col]++;
    }

    /**
     * Set the cells to the tally to replace the cells
     * @param array
     */
    public void setCells(int[] array) {
        this.cells=array;
    }

    /**
     * This combines another Timestamps cells array for use in accum.
     * @param another the other TimeStamp object that contains the other dimxdim array
     * @return the int array after the merge
     */
    public int[] combine (TimeStamp another) {
        int[] otherCell = another.cells;
        if (otherCell.length!=cells.length) {
            System.out.println("BROKEN LENGTH");
        }
        for (int i = 0; i<cells.length;i++) {
            cells[i]+=otherCell[i];
        }
        return cells;
    }

    /**
     * To see the cell entries
     * @return the string to be returned from the listing of the whole int array
     */
    public String toString() {
        String test = "";
        for (int i : cells) {
            test += " " + i;
        }
        return test;
    }


}
