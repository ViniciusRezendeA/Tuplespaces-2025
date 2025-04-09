package pt.ulisboa.tecnico.tuplespaces.server.interceptor;

import io.grpc.Context;

public class ContextKeys {
    public static final Context.Key<String> DELAY_KEY = Context.key("delay");
}
