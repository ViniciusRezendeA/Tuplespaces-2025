package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;

public class ServerState {

  private List<String> tuples;

  public ServerState() {
    this.tuples = new ArrayList<String>();

  }

  public synchronized void put(String tuple) {
    tuples.add(tuple);
    notifyAll();
  }

  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern) || tuple.equals(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  public synchronized String read(String pattern) {
    String tuple = getMatchingTuple(pattern);
    try {
      while (tuple == null) {
        wait();
        tuple = getMatchingTuple(pattern);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return tuple;
  }

  public synchronized String take(String pattern) {
    String tuple = getMatchingTuple(pattern);
    while (tuple == null) {
      try {
        wait();
        tuple = getMatchingTuple(pattern);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    String result = this.tuples.remove(this.tuples.indexOf(tuple));
    notifyAll();
    return result;
  }

  public List<String> getTupleSpacesState() {
    return this.tuples;
  }

}
