package pt.ulisboa.tecnico.tuplespaces.server;

import java.io.IOException;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.ulisboa.tecnico.tuplespaces.server.service.ReplicaServerImpl;
import io.grpc.ServerInterceptors;
import pt.ulisboa.tecnico.tuplespaces.server.interceptor.DelayInterceptor;


public class ReplicaServerMain {
  private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

  private static void debug(String debugMessage) {

    if (DEBUG_FLAG)
      System.err.println(debugMessage);
  }

  public static void main(String[] args) {
    debug(String.format("Received %d arguments%n", args.length));
    for (int i = 0; i < args.length; i++) {
      debug(String.format("arg[%d] = %s%n", i, args[i]));
    }

    if (args.length < 1) {
      debug("Argument(s) missing!");
      debug(String.format("Usage: java %s port%n", ReplicaServerMain.class.getName()));
      return;
    }

    final int port = Integer.parseInt(args[0]);
    final BindableService impl = new ReplicaServerImpl();

    Server server = ServerBuilder.forPort(port)
    .addService(ServerInterceptors.intercept(impl, new DelayInterceptor()))
    .build();

    try {
      server.start();

      debug("Server started");

      server.awaitTermination();
    } catch (IOException e) {
      debug("Failed to start the server: " + e.getMessage());
      e.printStackTrace();
      return;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }
}
