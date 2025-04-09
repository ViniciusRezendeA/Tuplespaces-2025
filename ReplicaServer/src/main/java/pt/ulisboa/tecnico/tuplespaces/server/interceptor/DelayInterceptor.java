package pt.ulisboa.tecnico.tuplespaces.server.interceptor;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class DelayInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String delay = null;

        // Tenta extrair delay-replica-0, delay-replica-1, delay-replica-2
        for (int i = 0; i < 3; i++) {
            Metadata.Key<String> key = Metadata.Key.of("delay-replica-" + i, Metadata.ASCII_STRING_MARSHALLER);
            delay = headers.get(key);
            if (delay != null) break;
        }

        Context contextWithDelay = Context.current().withValue(ContextKeys.DELAY_KEY, delay);
        return Contexts.interceptCall(contextWithDelay, call, headers, next);
    }
}
