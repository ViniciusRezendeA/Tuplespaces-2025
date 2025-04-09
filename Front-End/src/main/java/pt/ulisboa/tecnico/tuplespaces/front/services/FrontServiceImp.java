package pt.ulisboa.tecnico.tuplespaces.front.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.ReplicaServerGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.GetTupleSpacesStateRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.GetTupleSpacesStateResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.PutRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.PutResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.ReadRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.TakeRequest;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesOuterClass.TakeResponse;
import pt.ulisboa.tecnico.tuplespaces.front.interceptors.MetadataCredentials;
import pt.ulisboa.tecnico.tuplespaces.front.interceptors.MetadataInterceptor;
import pt.ulisboa.tecnico.tuplespaces.front.observers.GetTupleSpacesStateObserver;
import pt.ulisboa.tecnico.tuplespaces.front.observers.PutStreamObserver;
import pt.ulisboa.tecnico.tuplespaces.front.observers.ReadStreamObserver;
import pt.ulisboa.tecnico.tuplespaces.front.observers.ReleaseStreamObserver;
import pt.ulisboa.tecnico.tuplespaces.front.observers.RequestObserver;
import pt.ulisboa.tecnico.tuplespaces.front.observers.TakeStreamObserver;
import pt.ulisboa.tecnico.tuplespaces.front.utils.Validator;

public class FrontServiceImp extends TupleSpacesGrpc.TupleSpacesImplBase {

    private List<ReplicaServerGrpc.ReplicaServerStub> stubs;
    private ResponseCollector responseCollector;
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    // Estruturas para controle de operações por cliente
    private Map<Integer, Lock> clientLocks = new ConcurrentHashMap<>();
    private Map<Integer, List<PendingOperation>> pendingOperations = new ConcurrentHashMap<>();
    private Map<Integer, Boolean> takeInProgress = new ConcurrentHashMap<>();

    // Usar ExecutorService em vez de Thread direta
    private final ExecutorService backgroundExecutor = Executors.newCachedThreadPool();

    // Classe para representar operações pendentes
    private class PendingOperation {
        enum OperationType {
            PUT, TAKE, READ
        }

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
    }

    public FrontServiceImp(List<String> hosts_List) {
        stubs = new ArrayList<ReplicaServerGrpc.ReplicaServerStub>();
        hosts_List.forEach(
                host -> {
                    ManagedChannel channel = ManagedChannelBuilder.forTarget(host).usePlaintext().build();
                    stubs.add(ReplicaServerGrpc.newStub(channel));
                });
        responseCollector = new ResponseCollector();
    }

    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println("[FrontEnd] " + debugMessage);
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        debug("Received put request: " + request.getNewTuple());
        try {
            String tuple = request.getNewTuple();
            if (!Validator.checkHasValidTuple(tuple)) {
                debug("Invalid tuple: " + tuple);
                responseObserver
                        .onError(Status.INVALID_ARGUMENT.withDescription("Invalid input!").asRuntimeException());
                return;
            }

            // Extrair clientId dos metadados
            Metadata metadata = MetadataInterceptor.getMetadata();
            int clientId = extractClientId(metadata);
            int[] delays = getDelaysFromMetadata(metadata);

            debug("Processing PUT for client " + clientId);

            // Usar um lock para garantir que as operações do mesmo cliente estejam na ordem
            Lock clientLock = getClientLock(clientId);
            clientLock.lock();

            try {
                // Se houver um take em andamento, não podemos prosseguir imediatamente
                if (Boolean.TRUE.equals(takeInProgress.get(clientId))) {
                    debug("Client " + clientId + " has take in progress, queueing PUT operation");

                    // Adicionar à fila de operações pendentes
                    addPendingOperation(clientId, PendingOperation.OperationType.PUT, request, responseObserver,
                            delays);
                    return;
                }

                // Sem take em andamento, executar normalmente
                executePut(request, responseObserver, delays);
            } finally {
                clientLock.unlock();
            }
        } catch (Exception e) {
            debug("Error in PUT: " + e.getMessage());
            e.printStackTrace();
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal error: " + e.getMessage()).asRuntimeException());
        }
    }

    // Método de execução real da operação PUT - OTIMIZADO (retorno imediato)
    private void executePut(PutRequest request, StreamObserver<PutResponse> responseObserver, int[] delays) {
        // OTIMIZAÇÃO 1: Responder imediatamente ao cliente, sem esperar pelas réplicas
        PutResponse response = PutResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        debug("Put response sent immediately to client (optimization 1)");

        // Enviar para as réplicas em background (não bloqueante)
        backgroundExecutor.submit(() -> {
            for (int i = 0; i < stubs.size(); i++) {
                Metadata metadata = getMetadataForReplica(i, delays);
                try {
                    stubs.get(i).withCallCredentials(new MetadataCredentials(metadata)).put(request,
                            new PutStreamObserver(new ResponseCollector()));
                    debug("Put request sent to replica " + i);
                } catch (Exception e) {
                    debug("Error sending put to replica " + i + ": " + e.getMessage());
                    // Erro ao enviar para réplica não afeta o cliente, que já recebeu resposta
                }
            }
        });
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        debug("Received read request: " + request.getSearchPattern());
        String pattern = request.getSearchPattern();

        if (!Validator.checkHasValidTuple(pattern)) {
            debug("Invalid input: " + pattern);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid input!").asRuntimeException());
            return;
        }

        // Extrair clientId dos metadados
        Metadata metadata = MetadataInterceptor.getMetadata();
        int clientId = extractClientId(metadata);
        int[] delays = getDelaysFromMetadata(metadata);

        debug("Processing READ for client " + clientId);

        // Usar um lock para garantir que as operações do mesmo cliente estejam na ordem
        Lock clientLock = getClientLock(clientId);
        clientLock.lock();

        try {
            // Se houver operações pendentes, não execute imediatamente
            if (hasPendingOperations(clientId)) {
                debug("Client " + clientId + " has pending operations, queueing READ operation");
                addPendingOperation(clientId, PendingOperation.OperationType.READ, request, responseObserver, delays);
                return;
            }

            // Nenhuma operação pendente, execute normalmente
            executeRead(request, responseObserver, delays);
        } finally {
            clientLock.unlock();
        }
    }

    private void executeRead(ReadRequest request, StreamObserver<ReadResponse> responseObserver, int[] delays) {
        ResponseCollector readCollector = new ResponseCollector();
        String pattern = request.getSearchPattern();
        debug("Executing READ with pattern: " + pattern);
        if (!Validator.checkHasValidTuple(pattern)) {
            debug("Invalid input: " + pattern);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid input!").asRuntimeException());
            return;
        }

        for (int i = 0; i < stubs.size(); i++) {
            Metadata metadata = getMetadataForReplica(i, delays);
            // Fazer a chamada assíncrona com a metadata
            stubs.get(i).withCallCredentials(new MetadataCredentials(metadata)).read(request,
                    new ReadStreamObserver(readCollector));
            ;
        }
        try {
            readCollector.waitUntilAllReceived(1);
            String result = readCollector.getFirsString();
            debug("Result: " + result);
            ReadResponse response = ReadResponse.newBuilder().setResult(result).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            readCollector.clear();
        }
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        int clientId = request.getClientId();
        debug("Received take request for client " + clientId + ": " + request.getSearchPattern());

        // Validate the search pattern
        String pattern = request.getSearchPattern();
        if (!Validator.checkHasValidTuple(pattern)) {
            debug("Invalid input: " + pattern);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid input!").asRuntimeException());
            return;
        }

        // Extrair delays dos metadados
        Metadata metadata = MetadataInterceptor.getMetadata();
        int[] delays = getDelaysFromMetadata(metadata);

        // Obter o lock do cliente
        Lock clientLock = getClientLock(clientId);
        clientLock.lock();

        try {
            // Se houver operações pendentes, adicionar à fila
            if (hasPendingOperations(clientId)) {
                debug("Client " + clientId + " has pending operations, queueing TAKE operation");
                addPendingOperation(clientId, PendingOperation.OperationType.TAKE, request, responseObserver, delays);
                return;
            }

            // Marcar que este cliente tem um TAKE em andamento
            takeInProgress.put(clientId, true);

            // Executar a operação TAKE (apenas fase 1 é bloqueante)
            executeTake(request, responseObserver, delays);
        } finally {
            clientLock.unlock();
        }
    }

    private void executeTake(TakeRequest request, StreamObserver<TakeResponse> responseObserver, int[] delays) {
        int clientId = request.getClientId();
        ResponseCollector takeResponseCollector = new ResponseCollector();

        try {
            debug("Executing TAKE phase 1 (request) for client " + clientId);

            // FASE 1: Encontrar e bloquear o tuplo usando o protocolo Maekawa
            TakeRequest newRequest = this.request(request, takeResponseCollector);

            // OTIMIZAÇÃO 2: Responder imediatamente após fase 1
            TakeResponse response = TakeResponse.newBuilder().setResult(newRequest.getSearchPattern()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            debug("Take response sent immediately after phase 1 (optimization 2): " + newRequest.getSearchPattern());

            // FASE 2: Remover o tuplo de todas as réplicas e liberar os bloqueios em
            // background
            final TakeRequest finalRequest = newRequest;
            backgroundExecutor.submit(() -> {
                try {
                    debug("Starting TAKE phase 2 for client " + clientId + " in background");

                    // Parte 1 da fase 2: Enviar take para todas as réplicas
                    debug("TAKE phase 2 part 1: Removing tuple from all replicas");
                    ResponseCollector phase2Collector = new ResponseCollector();
                    for (int i = 0; i < stubs.size(); i++) {
                        Metadata metadata = getMetadataForReplica(i, delays);
                        try {
                            stubs.get(i).withCallCredentials(new MetadataCredentials(metadata)).take(finalRequest,
                                    new TakeStreamObserver(phase2Collector));
                        } catch (Exception e) {
                            debug("Error in phase 2 (take) for replica " + i + ": " + e.getMessage());
                        }
                    }

                    // Aguardar até que todas as réplicas processem o take
                    try {
                        phase2Collector.waitUntilAllReceived(stubs.size());
                        debug("Phase 2 part 1 (take) completed for all replicas");
                    } catch (InterruptedException e) {
                        debug("Interrupted waiting for phase 2 part 1: " + e.getMessage());
                    }

                    // Parte 2 da fase 2: Liberar locks em todas as réplicas
                    debug("TAKE phase 2 part 2: Releasing locks on all replicas");
                    ResponseCollector releaseCollector = new ResponseCollector();
                    for (ReplicaServerGrpc.ReplicaServerStub stub : stubs) {
                        try {
                            stub.release(finalRequest, new ReleaseStreamObserver(releaseCollector));
                        } catch (Exception e) {
                            debug("Error in phase 2 (release): " + e.getMessage());
                        }
                    }

                    // Aguardar até que todas as réplicas liberem os locks
                    try {
                        releaseCollector.waitUntilAllReceived(stubs.size());
                        debug("Phase 2 part 2 (release) completed for all replicas");
                    } catch (InterruptedException e) {
                        debug("Interrupted waiting for phase 2 part 2: " + e.getMessage());
                    }

                    debug("TAKE phase 2 completed for client " + clientId);
                } catch (Exception e) {
                    debug("Error in phase 2: " + e.getMessage());
                } finally {
                    // Independente do resultado, marcar que o take não está mais em andamento
                    Lock lock = getClientLock(clientId);
                    lock.lock();
                    try {
                        takeInProgress.put(clientId, false);
                        processPendingOperations(clientId);
                        debug("TAKE complete for client " + clientId + " - processing any pending operations");
                    } finally {
                        lock.unlock();
                    }
                }
            });

        } catch (Exception e) {
            debug("Error in TAKE phase 1: " + e.getMessage());
            e.printStackTrace();
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Error in phase 1: " + e.getMessage()).asRuntimeException());

            // Em caso de erro, limpar o estado
            Lock lock = getClientLock(clientId);
            lock.lock();
            try {
                takeInProgress.put(clientId, false);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void getTupleSpacesState(GetTupleSpacesStateRequest request,
            StreamObserver<GetTupleSpacesStateResponse> responseObserver) {

        try {
            debug("Received getTupleSpacesState request");
            ResponseCollector stateCollector = new ResponseCollector();

            // Consultar todas as réplicas
            for (ReplicaServerGrpc.ReplicaServerStub stub : stubs) {
                stub.getTupleSpacesState(request, new GetTupleSpacesStateObserver(stateCollector));
            }

            // Aguardar respostas
            stateCollector.waitUntilAllReceived(stubs.size());
            debug(stubs.size() + " responses received for getTupleSpacesState");

            // Consolidar resultados
            List<String> result = stateCollector.getResponses();
            debug("Sending getTupleSpacesState response with " + result.size() + " tuples");

            // Enviar resposta ao cliente
            GetTupleSpacesStateResponse response = GetTupleSpacesStateResponse.newBuilder()
                    .addAllTuple(result)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InterruptedException e) {
            debug("Error getting tuple space state: " + e.getMessage());
            e.printStackTrace();
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Error getting tuple space state: " + e.getMessage())
                            .asRuntimeException());
        } finally {
            responseCollector.clear();
        }
    }

    private TakeRequest request(TakeRequest request, ResponseCollector reqCollector) throws InterruptedException {
        int clientId = request.getClientId();
        Set<Integer> V_id = Set.of(clientId % 3, (clientId + 1) % 3);
        AtomicInteger i = new AtomicInteger(0);

        debug("Sending request to voter set for client " + clientId + ": " + V_id);

        // Enviar request para o subconjunto de réplicas (protocolo Maekawa)
        stubs.forEach(stub -> {
            if (V_id.contains(i.get())) {
                stub.request(request, new RequestObserver(reqCollector));
                debug("Request sent to replica " + i.get());
            }
            i.incrementAndGet();
        });

        // Aguardar respostas do subconjunto
        reqCollector.waitUntilAllReceived((stubs.size() - 1));

        // Obter o resultado mais frequente
        String result = reqCollector.getStringWithRepeat((stubs.size() - 1));
        debug("Request (phase 1) result: " + result);

        // Criar o request com o padrão real encontrado
        TakeRequest newRequest = TakeRequest.newBuilder()
                .setSearchPattern(result)
                .setClientId(clientId)
                .build();

        reqCollector.clear();
        return newRequest;
    }

    private Metadata getMetadataForReplica(int replicaIndex, int[] delays) {
        Metadata metadata = new Metadata();
        if (replicaIndex >= 0 && replicaIndex < delays.length) {
            String key = "delay-replica-" + replicaIndex;
            metadata.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), String.valueOf(delays[replicaIndex]));
        }
        return metadata;
    }

    // Métodos auxiliares para gerenciar operações por cliente

    private Lock getClientLock(int clientId) {
        return clientLocks.computeIfAbsent(clientId, k -> new ReentrantLock());
    }

    private void addPendingOperation(int clientId, PendingOperation.OperationType type,
            Object request, StreamObserver<?> responseObserver, int[] delays) {
        List<PendingOperation> operations = pendingOperations.computeIfAbsent(clientId, k -> new ArrayList<>());
        operations.add(new PendingOperation(type, request, responseObserver, delays));
        debug("Added pending " + type + " operation for client " + clientId + " (total pending: " + operations.size()
                + ")");
    }

    private boolean hasPendingOperations(int clientId) {
        List<PendingOperation> operations = pendingOperations.get(clientId);
        return operations != null && !operations.isEmpty();
    }

    private void processPendingOperations(int clientId) {
        List<PendingOperation> operations = pendingOperations.get(clientId);
        if (operations == null || operations.isEmpty()) {
            debug("No pending operations for client " + clientId);
            return;
        }

        // Processar a primeira operação pendente
        PendingOperation operation = operations.remove(0);
        debug("Processing pending " + operation.type + " operation for client " + clientId);

        try {
            switch (operation.type) {
                case PUT:
                    executePut((PutRequest) operation.request,
                            (StreamObserver<PutResponse>) operation.responseObserver,
                            operation.delays);
                    break;
                case TAKE:
                    // Marcar que temos um take em andamento novamente
                    takeInProgress.put(clientId, true);
                    executeTake((TakeRequest) operation.request,
                            (StreamObserver<TakeResponse>) operation.responseObserver,
                            operation.delays);
                    break;
                case READ:
                    executeRead((ReadRequest) operation.request,
                            (StreamObserver<ReadResponse>) operation.responseObserver,
                            operation.delays);
                    break;
                default:
                    debug("Unknown operation type: " + operation.type);
            }
        } catch (Exception e) {
            debug("Error processing pending operation: " + e.getMessage());
            e.printStackTrace();
        }

        // Se não for um TAKE e houver mais operações, continue processando
        if (operation.type != PendingOperation.OperationType.TAKE && !operations.isEmpty()) {
            debug("Processing next pending operation for client " + clientId);
            processPendingOperations(clientId);
        }
    }

    private int extractClientId(Metadata metadata) {
        try {
            String clientIdStr = metadata.get(Metadata.Key.of("client-id", Metadata.ASCII_STRING_MARSHALLER));
            return clientIdStr != null ? Integer.parseInt(clientIdStr) : 0;
        } catch (NumberFormatException e) {
            debug("Error extracting clientId from metadata: " + e.getMessage());
            return 0;
        }
    }

    private int[] getDelaysFromMetadata(Metadata metadata) {
        int[] delays = new int[3];
        for (int i = 0; i < 3; i++) {
            try {
                String key = "delay-replica-" + i;
                String delayStr = metadata.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
                delays[i] = delayStr != null ? Integer.parseInt(delayStr) : 0;
            } catch (NumberFormatException e) {
                delays[i] = 0;
            }
        }
        return delays;
    }

    // Método para limpeza ao encerrar o serviço
    public void shutdown() {
        try {
            backgroundExecutor.shutdown();
            if (!backgroundExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            backgroundExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}