package com.example.volkerpetersen.sailingrace;

/**
 * Created by Volker Petersen - November 2015.
 *
 * FIFO Queue for "double" variables implementation using the LinkedList
 */

import android.util.Log;

import java.text.DecimalFormat;
import java.util.LinkedList;

public class fifoQueueDouble {
    static final String LOG_TAG = fifoQueueDouble.class.getSimpleName();
    private LinkedList<Double> queue = new LinkedList<Double>();
    private int size;

    // class initialization.  Create a new instance of a FIFO queue of size "size"
    public fifoQueueDouble(int queue_size) {
        this.size = queue_size;
    }

    // Inserts the specified element into this queue if it is possible to do so
    // immediately without violating capacity restrictions, otherwise drop first
    // value in queue off and then add value to the end of this queue.
    public void add(double value) {
        if (queue.size() >= size) {
            queue.poll();
        }
        queue.add(value);
    }

    // Removes a single instance of the specified element from this collection
    public void remove(double value) {
        queue.remove(value);
    }

    // Retrieves and removes the head (first in) of this queue, or returns null if this
    // queue is empty.
    public double poll() {
        double data = queue.poll();
        return data;
    }

    // Returns true if this collection contains no elements
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    // Return an element from queue
    // returns the value or NaN if the element index is out of range
    public double getElement(int i) {
        if (i < 0 || i > queue.size()-1) {
            return Double.NaN;
        } else {
            return queue.get(i);
        }
    }

    // Returns the first element from queue
    // returns the value or NaN if the queue is empty
    public double getFirst() {
        if (queue.size() == 0) {
            return Double.NaN;
        } else {
            return queue.peekFirst();
        }
    }

    // Returns the first element from queue
    // returns the value or NaN if the queue is empty
    public double getLast() {
        int i = queue.size();
        if (i == 0) {
            return Double.NaN;
        } else {
            return queue.peekLast();
        }
    }

    // Returns the number of elements in this collection. If this collection
    // contains more than Integer.MAX_VALUE elements, returns Integer.MAX_VALUE
    public int getTotalSize() {
        return queue.size();
    }

    // compute the average value of the values in the FIFO queue
    public double average() {
        double sum = 0.0;
        if (queue.isEmpty()) {
            return sum;
        }
        for (int i=0; i < queue.size(); i++) {
            sum = sum + queue.get(i);
        }
        return sum/(double)queue.size();
    }

    // Show all FIFO queue elements in Log.d
    public void logQueue() {
        DecimalFormat dfOne = new DecimalFormat("#");
        DecimalFormat df2 = new DecimalFormat("#0.00");

        if (queue.isEmpty()) {
            Log.d(LOG_TAG, "FIFI Queue is empty");
        }
        for (int i=0; i < queue.size(); i++) {
            Log.d(LOG_TAG, "FIFO Queue element "+dfOne.format(i)+" value = "+df2.format(queue.get(i)));
        }

    }
}
