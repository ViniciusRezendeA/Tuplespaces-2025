package pt.ulisboa.tecnico.tuplespaces.front.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.TakeResponse;
import pt.ulisboa.tecnico.tuplespaces.front.services.ResponseCollector;

public class TakeStreamObserver
        implements StreamObserver<TakeResponse> {
    ResponseCollector responseCollector;

    public TakeStreamObserver(ResponseCollector responseCollector) {
        this.responseCollector = responseCollector;
    }

    @Override
    public void onNext(TakeResponse value) {
        responseCollector.addString(value.getResult());
    }

    @Override
    public void onError(Throwable t) {
        System.out.println("TakeObserver Error: " + t.getMessage());
    }

    @Override
    public void onCompleted() {
        this.responseCollector.addCompleted("Take");

    }
}