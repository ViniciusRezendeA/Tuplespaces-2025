package pt.ulisboa.tecnico.tuplespaces.front.services;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.front.enums.OperationType;

public class PendingOperation {

    private OperationType type;
    private Object request;
    private StreamObserver<?> responseObserver;
    private int[] delays;

    public PendingOperation(OperationType type, Object request, StreamObserver<?> responseObserver, int[] delays) {
        this.type = type;
        this.request = request;
        this.responseObserver = responseObserver;
        this.delays = delays;
    }

    public OperationType getType() {
        return type;
    }

    public Object getRequest() {
        return request;
    }

    public StreamObserver<?> getResponseObserver() {
        return responseObserver;
    }

    public int[] getDelays() {
        return delays;
    }

    public void setDelays(int[] delays) {
        this.delays = delays;
    }

    public void setType(OperationType type) {
        this.type = type;
    }

    public void setRequest(Object request) {
        this.request = request;
    }

    public void setResponseObserver(StreamObserver<?> responseObserver) {
        this.responseObserver = responseObserver;
    }

    @Override
    public String toString() {
        return "PendingOperation{" +
                "type=" + type +
                ", request=" + request +
                ", responseObserver=" + responseObserver +
                ", delays=" + delays +
                '}';
    }
}