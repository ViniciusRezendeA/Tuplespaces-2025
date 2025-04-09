package pt.ulisboa.tecnico.tuplespaces.front;

import java.io.IOException;
import java.util.List;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import pt.ulisboa.tecnico.tuplespaces.front.interceptors.MetadataInterceptor;
import pt.ulisboa.tecnico.tuplespaces.front.services.FrontServiceImp;

public class FrontMain {
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
      debug(String.format("Usage: java %s port%n", FrontMain.class.getName()));
      return;
    }

    final int port = Integer.parseInt(args[0]);
    final List<String> host_port = List.of(args[1].split(","));
    final BindableService impl = new FrontServiceImp(host_port);

    Server server = ServerBuilder.forPort(port)
        .addService(ServerInterceptors.intercept(impl, new MetadataInterceptor()))
        .build();

    try {
      server.start();

      debug("Front-End started");

      server.awaitTermination();
    } catch (IOException e) {
      debug("Failed to start the Front-End: " + e.getMessage());
      e.printStackTrace();
      return;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }
}
