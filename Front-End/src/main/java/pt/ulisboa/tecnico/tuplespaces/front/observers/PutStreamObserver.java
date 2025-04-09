package pt.ulisboa.tecnico.tuplespaces.front.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.PutResponse;
import pt.ulisboa.tecnico.tuplespaces.front.colletors.ResponseCollector;

public class PutStreamObserver implements StreamObserver<PutResponse> {
    ResponseCollector responseCollector;

    public PutStreamObserver(ResponseCollector responseCollector) {
        this.responseCollector = responseCollector;
    }

    @Override
    public void onNext(PutResponse response) {
        responseCollector.addString("OK");

    }

    @Override
    public void onError(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }

    @Override
    public void onCompleted() {
        this.responseCollector.addCompleted("Put");
    }

}
