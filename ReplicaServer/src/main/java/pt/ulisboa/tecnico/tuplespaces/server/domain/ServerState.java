package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.TakeRequest;
import pt.ulisboa.tecnico.tuplespaces.server.model.Tuple;

public class ServerState {

  private List<Tuple> tuples;

  public ServerState() {
    this.tuples = new ArrayList<Tuple>();

  }

  public synchronized void put(String tuple) {
    Tuple newTuple = new Tuple(tuple);
    this.tuples.add(newTuple);
    notifyAll();
  }

  private Tuple getMatchingTuple(String pattern) {
    for (Tuple tuple : this.tuples) {
      if (tuple.getValue().matches(pattern) || tuple.getValue().equals(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  public synchronized String read(String pattern) {
    Tuple tuple = getMatchingTuple(pattern);
    try {
      while (tuple == null) {
        wait();
        tuple = getMatchingTuple(pattern);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return tuple.getValue();
  }

  public synchronized String take(TakeRequest request) {
    String pattern = request.getSearchPattern();

    Tuple tuple = getMatchingTuple(pattern);
    while (tuple == null) {
      try {
        wait();
        tuple = getMatchingTuple(pattern);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    String result = this.tuples.remove(this.tuples.indexOf(tuple)).getValue();
    notifyAll();
    return result;
  }

  public List<String> getTupleSpacesState() {
    return this.convertTupleListToStringList(tuples);
  }

  private List<Tuple> findAll(TakeRequest request) {
    String pattern = request.getSearchPattern();
    int clientId = request.getClientId();

    List<Tuple> result = new ArrayList<Tuple>();
    for (Tuple tuple : this.tuples) {
      if (tuple.getValue().matches(pattern) || tuple.getValue().equals(pattern) && !tuple.isLocked()) {
        tuple.lock(clientId);
        result.add(tuple);
      }
    }
    return result;
  }

  public synchronized List<String> find(TakeRequest request) {
    List<Tuple> result = findAll(request);

    while (result.isEmpty()) {
      try {
        wait();
        result = findAll(request);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return this.convertTupleListToStringList(result);
  }

  private List<String> convertTupleListToStringList(List<Tuple> tupleList) {
    List<String> result = new ArrayList<String>();
    for (Tuple tuple : tupleList) {
      result.add(tuple.getValue());
    }
    return result;
  }

  public synchronized void unlockAllByClientId(int clientId) {
    for (Tuple tuple : this.tuples) {
      if (tuple.getClientId() == clientId) {
        tuple.unlock();
      }
    }
    notifyAll();

  }

}
