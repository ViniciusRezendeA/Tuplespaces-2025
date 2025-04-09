package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.GetTupleSpacesStateResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.PutRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.PutResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.ReadRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.TakeRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.TakeResponse;

public class ClientService {

    private TupleSpacesGrpc.TupleSpacesBlockingStub stub;
    private int clientId;

    public ClientService(String host_port, int clientId) {

        final ManagedChannel channel = ManagedChannelBuilder.forTarget(host_port).usePlaintext().build();
        stub = TupleSpacesGrpc.newBlockingStub(channel);
        this.clientId = clientId;

    }

    public String take(String pattern, int[] delays) {
        TakeRequest request = TakeRequest.newBuilder().setSearchPattern(pattern).setClientId(clientId).build();
        try {
            Metadata metadata = createDelayMetadata(delays);

            TakeResponse response = stub.withCallCredentials(new MetadataCredentials(metadata))
                    .take(request);
            System.out.println("OK");
            return response.getResult();
        } catch (StatusRuntimeException e) {
            Status status = e.getStatus();
            return status.getDescription();
        }
    }

    public String read(String pattern, int[] delays) {
        ReadRequest request = ReadRequest.newBuilder().setSearchPattern(pattern).build();
        try {
            Metadata metadata = createDelayMetadata(delays);

            ReadResponse response = stub.withCallCredentials(new MetadataCredentials(metadata)).read(request);
            System.out.println("OK");
            return response.getResult();
        } catch (StatusRuntimeException e) {
            Status status = e.getStatus();
            return (status.getDescription());
        }
    }

    public List<String> getTupleSpacesState() {
        try {
            GetTupleSpacesStateResponse response = stub.getTupleSpacesState(null);
            System.out.println("OK");
            return response.getTupleList();
        } catch (StatusRuntimeException e) {
            Status status = e.getStatus();
            System.out.println(status.getDescription());
            return List.of();
        }
    }

    public String put(String tuple, int[] delays) {
        PutRequest request = PutRequest.newBuilder().setNewTuple(tuple).build();
        try {
            Metadata metadata = createDelayMetadata(delays);
            PutResponse response = stub.withCallCredentials(new MetadataCredentials(metadata)).put(request);
            System.out.println("OK");
            return response.toString();
        } catch (StatusRuntimeException e) {
            Status status = e.getStatus();
            return (status.getDescription());
        }
    }

    private Metadata createDelayMetadata(int[] delays) {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("delay-replica-0", Metadata.ASCII_STRING_MARSHALLER), String.valueOf(delays[0]));
        metadata.put(Metadata.Key.of("delay-replica-1", Metadata.ASCII_STRING_MARSHALLER), String.valueOf(delays[1]));
        metadata.put(Metadata.Key.of("delay-replica-2", Metadata.ASCII_STRING_MARSHALLER), String.valueOf(delays[2]));
        metadata.put(Metadata.Key.of("client-id", Metadata.ASCII_STRING_MARSHALLER), this.clientId + "");
        return metadata;
    }

}
