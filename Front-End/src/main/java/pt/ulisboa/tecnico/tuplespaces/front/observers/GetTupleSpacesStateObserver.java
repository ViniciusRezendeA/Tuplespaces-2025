package pt.ulisboa.tecnico.tuplespaces.front.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.GetTupleSpacesStateResponse;
import pt.ulisboa.tecnico.tuplespaces.front.colletors.ResponseCollector;

public class GetTupleSpacesStateObserver implements StreamObserver<GetTupleSpacesStateResponse> {
    ResponseCollector responseCollector;

    public GetTupleSpacesStateObserver(ResponseCollector responseCollector) {
        this.responseCollector = responseCollector;
    }

    @Override
    public void onNext(GetTupleSpacesStateResponse value) {
        value.getTupleList().forEach(tuple -> responseCollector.addString(tuple));
    }

    @Override
    public void onError(Throwable t) {
        System.out.println("Error: " + t.getMessage());
    }

    @Override
    public void onCompleted() {
        this.responseCollector.addCompleted("GetTupleSpacesState");

    }

}
