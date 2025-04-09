package pt.ulisboa.tecnico.tuplespaces.front.interceptors;

import java.util.concurrent.Executor;

import io.grpc.CallCredentials;
import io.grpc.Metadata;

public class MetadataCredentials extends CallCredentials {
    private final Metadata metadata;

    public MetadataCredentials(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
        appExecutor.execute(() -> applier.apply(metadata));
    }

    @Override
    public void thisUsesUnstableApi() {

    }
}
