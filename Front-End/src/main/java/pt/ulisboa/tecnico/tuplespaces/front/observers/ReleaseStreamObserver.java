package pt.ulisboa.tecnico.tuplespaces.front.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass.EmptyResponse;
import pt.ulisboa.tecnico.tuplespaces.front.services.ResponseCollector;

public class ReleaseStreamObserver implements StreamObserver<EmptyResponse> {
    ResponseCollector responseCollector;

    public ReleaseStreamObserver(ResponseCollector responseCollector) {
        this.responseCollector = responseCollector;
    }

    @Override
    public void onNext(EmptyResponse value) {

    }

    @Override
    public void onError(Throwable t) {
        System.out.println("ReleaseObserver Error:  " + t.getMessage());
    }

    @Override
    public void onCompleted() {
        this.responseCollector.addCompleted("Response");

    }
}
