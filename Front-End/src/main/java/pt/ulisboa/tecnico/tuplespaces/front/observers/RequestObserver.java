package pt.ulisboa.tecnico.tuplespaces.front.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass.RequestResponse;
import pt.ulisboa.tecnico.tuplespaces.front.colletors.ResponseCollector;

public class RequestObserver implements StreamObserver<RequestResponse> {

    ResponseCollector responseCollector;

    public RequestObserver(ResponseCollector responseCollector) {
        this.responseCollector = responseCollector;
    }

    @Override
    public void onNext(RequestResponse value) {
        value.getTupleList().forEach(tuple -> responseCollector.addString(tuple));
    }

    @Override
    public void onError(Throwable t) {
        System.out.println("RequestObserver Error: " + t.getMessage());
    }

    @Override
    public void onCompleted() {
        this.responseCollector.addCompleted("Request");

    }
}
