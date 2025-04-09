package pt.ulisboa.tecnico.tuplespaces.server.service;

import java.util.List;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass.EmptyResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerOuterClass.RequestResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.GetTupleSpacesStateRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.GetTupleSpacesStateResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.PutRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.PutResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.ReadRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.TakeRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.TakeResponse;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import pt.ulisboa.tecnico.tuplespaces.server.interceptor.ContextKeys;
import pt.ulisboa.tecnico.tuplespaces.server.utils.Validator;

public class ReplicaServerImpl extends ReplicaServerGrpc.ReplicaServerImplBase {

    private ServerState serverState = new ServerState();
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    private void applyDelayIfPresent() {
        String delayStr = ContextKeys.DELAY_KEY.get();
        int delay = 0;
        try {
            delay = (delayStr != null) ? Integer.parseInt(delayStr) : 0;
            if (delay > 0) {
                debug("Applying delay of " + delay + " seconds...");
                Thread.sleep(delay * 1000L);
            }
        } catch (InterruptedException | NumberFormatException e) {
            debug("Failed to apply delay: " + e.getMessage());
        }
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        debug("Received put request");
        applyDelayIfPresent();

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
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        debug("Received read request");
        applyDelayIfPresent();

        String pattern = request.getSearchPattern();
        if (!Validator.checkHasValidTuple(pattern)) {
            debug("Invalid input: " + pattern);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid input!").asRuntimeException());
            return;
        }

        ReadResponse response = ReadResponse.newBuilder()
                .setResult(serverState.read(pattern))
                .build();

        debug("Sending response");
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getTupleSpacesState(GetTupleSpacesStateRequest request,
            StreamObserver<GetTupleSpacesStateResponse> responseObserver) {
        debug("Received getTupleSpacesState request");
        GetTupleSpacesStateResponse response = GetTupleSpacesStateResponse.newBuilder()
                .addAllTuple(serverState.getTupleSpacesState())
                .build();
        debug("Sending response");
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void request(TakeRequest request, StreamObserver<RequestResponse> responseObserver) {
        applyDelayIfPresent();
        debug("Received request to client " + request.getClientId());
        // Validate the search pattern
        String pattern = request.getSearchPattern();
        if (!Validator.checkHasValidTuple(pattern)) {
            debug("Invalid input for request: " + pattern);
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription("Invalid input!").asRuntimeException());
            return;
        }

        // Return the matching tuples (if any) to the client
        List<String> result = serverState.find(request);
        responseObserver.onNext(RequestResponse.newBuilder().addAllTuple(result).build());
        responseObserver.onCompleted();
        debug("Sent response to client " + request.getClientId() + " with " + result.size() + " tuples.");
        debug("Result: " + result);
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        String pattern = request.getSearchPattern();
        int clientId = request.getClientId();

        if (!Validator.checkHasValidTuple(pattern)) {
            debug("Invalid input: " + pattern);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid input!").asRuntimeException());
            return;
        }
        String tuple = serverState.take(request);

        debug(tuple + " taken by client " + clientId);
        responseObserver.onNext(TakeResponse.newBuilder().setResult(tuple).build());
        responseObserver.onCompleted();
    }

    @Override
    public void release(TakeRequest request, StreamObserver<EmptyResponse> responseObserver) {

        debug("Received release request to release vote for client " + request.getClientId());
        // Unlock all tuples locked by the client
        serverState.unlockAllByClientId(request.getClientId());

        debug("Unlocked all tuples for client " + request.getClientId());
        responseObserver.onNext(EmptyResponse.newBuilder().build());
        responseObserver.onCompleted();
    }
}
