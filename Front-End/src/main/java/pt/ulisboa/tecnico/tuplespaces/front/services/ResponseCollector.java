package pt.ulisboa.tecnico.tuplespaces.front.services;

import java.util.ArrayList;
import java.util.List;

public class ResponseCollector {
    private List<String> collectedResponses;
    private List<String> receiveCompleted;

    public ResponseCollector() {
        collectedResponses = new ArrayList<String>();
        receiveCompleted = new ArrayList<String>();
    }

    synchronized public void addString(String s) {
        collectedResponses.add(s);
        notifyAll();
    }

    synchronized public String getStrings() {
        String res = new String();
        for (String s : collectedResponses) {
            res = res.concat(s);
        }
        return res;
    }

    synchronized public List<String> getResponses() {
        return collectedResponses;
    }

    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (receiveCompleted.size() < n)
            wait();
    }

    synchronized public void addCompleted(String s) {
        receiveCompleted.add(s);
        notifyAll();
    }

    synchronized public void clear() {
        collectedResponses.clear();
        receiveCompleted.clear();
    }

    synchronized public String getStringWithRepeat(int n) {
        for (String s : collectedResponses) {
            int count = 0;
            for (String t : collectedResponses) {
                if (s.equals(t)) {
                    count++;
                }
            }
            if (count >= n) {
                return s;
            }
        }
        return null;
    }

    public String getFirsString() {
        return collectedResponses.get(0);
    }
}
