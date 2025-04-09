package pt.ulisboa.tecnico.tuplespaces.server.service;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.GetTupleSpacesStateRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.GetTupleSpacesStateResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.PutRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.PutResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.ReadRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.TakeRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.TakeResponse;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import pt.ulisboa.tecnico.tuplespaces.server.utils.Validator;

public class TupleSpacesServiceImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    private ServerState serverState = new ServerState();
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        debug("Received put request");
        String tuple = request.getNewTuple();
        if (!Validator.checkHasValidTuple(tuple)) {
            debug("Invalid tuple: " + tuple);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid input!").asRuntimeException());
            return;
        }
        serverState.put(tuple);
        debug("Tuple added: " + tuple);
        responseObserver.onNext(PutResponse.newBuilder().build());
        responseObserver.onCompleted();

    }

    @Override
    public void read(ReadRequest request,
            StreamObserver<ReadResponse> responseObserver) {
        debug("Received read request");
        String pattern = request.getSearchPattern();
        if (!Validator.checkHasValidTuple(pattern)) {
            debug("Invalid input: " + pattern);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid input!").asRuntimeException());
            return;
        }
        ReadResponse response = ReadResponse.newBuilder().setResult(serverState.read(pattern)).build();
        debug("Sending response");
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void take(TakeRequest request,
            StreamObserver<TakeResponse> responseObserver) {
        debug("Received take request");
        String pattern = request.getSearchPattern();
        if (!Validator.checkHasValidTuple(pattern)) {
            debug("Invalid input: " + pattern);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid input!").asRuntimeException());
            return;
        }
        TakeResponse response = TakeResponse.newBuilder().setResult(serverState.take(pattern)).build();
        debug("Sending response");
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getTupleSpacesState(GetTupleSpacesStateRequest request,
            StreamObserver<GetTupleSpacesStateResponse> responseObserver) {
        debug("Received getTupleSpacesState request");
        GetTupleSpacesStateResponse response = GetTupleSpacesStateResponse.newBuilder()
                .addAllTuple(serverState.getTupleSpacesState()).build();
        debug("Sending response");
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
