package pt.ulisboa.tecnico.tuplespaces.front.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.front.services.ResponseCollector;

public class ReadStreamObserver implements StreamObserver<ReadResponse> {

    ResponseCollector responseCollector;

    public ReadStreamObserver(ResponseCollector responseCollector) {
        this.responseCollector = responseCollector;
    }

    @Override
    public void onNext(ReadResponse value) {
        responseCollector.addString(value.getResult());
    }

    @Override
    public void onError(Throwable t) {
        System.out.println("ReadObserver Error: " + t.getMessage());
    }

    @Override
    public void onCompleted() {
        this.responseCollector.addCompleted("Read");
    }

}
