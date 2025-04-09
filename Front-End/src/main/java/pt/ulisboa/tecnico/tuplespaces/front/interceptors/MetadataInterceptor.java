package pt.ulisboa.tecnico.tuplespaces.front.interceptors;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class MetadataInterceptor implements ServerInterceptor {
    private static final Context.Key<Metadata> METADATA_KEY = Context.key("metadata");

    public static Metadata getMetadata() {
        return METADATA_KEY.get();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        Context ctx = Context.current().withValue(METADATA_KEY, headers);
        return Contexts.interceptCall(ctx, call, headers, next);
    }
}
