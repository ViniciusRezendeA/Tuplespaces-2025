package pt.ulisboa.tecnico.tuplespaces.client;

import java.util.List;
import java.util.Scanner;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class CommandProcessor {

    private static final String SPACE = " ";
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";
    private static final String PUT = "put";
    private static final String READ = "read";
    private static final String TAKE = "take";
    private static final String SLEEP = "sleep";
    private static final String EXIT = "exit";
    private static final String GET_TUPLE_SPACES_STATE = "getTupleSpacesState";

    private final ClientService clientService;

    public CommandProcessor(ClientService clientService) {
        this.clientService = clientService;
    }

    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String[] split = line.split(SPACE);
            switch (split[0]) {
                case PUT:
                    this.put(split);
                    break;

                case READ:
                    this.read(split);
                    break;

                case TAKE:
                    this.take(split);
                    break;

                case GET_TUPLE_SPACES_STATE:
                    this.getTupleSpacesState();
                    break;

                case SLEEP:
                    this.sleep(split);
                    break;

                case EXIT:
                    exit = true;
                    break;

                default:
                    this.printUsage();
                    break;
            }
        }
        scanner.close();
    }

    private void put(String[] split) {
        if (!this.inputIsValid(split)) {
            System.out.println("Invalid input");
            this.printUsage();
            return;
        }

        String tuple = split[1];
        int[] delays = extractDelays(split);

        String result = clientService.put(tuple, delays);
        System.out.println(result + "\n");
        }

    private void read(String[] split) {
        if (!this.inputIsValid(split)) {
            System.out.println("Invalid input");
            this.printUsage();
            return;
        }

        String tuple = split[1];
        int[] delays = extractDelays(split);

        String result = clientService.read(tuple, delays);
        System.out.println(result + "\n");
    }

    private void take(String[] split) {
        // check if input is valid
        if (!this.inputIsValid(split)) {
            System.out.println("Invalid input");
            this.printUsage();
            return;
        }

        // get the tuple
        String tuple = split[1];

        // Call ClientService.take
        int[] delays = extractDelays(split);
        String result = clientService.take(tuple, delays);

        System.out.println(result + "\n");
    }

    private void getTupleSpacesState() {
        List<String> tupleSpacesState = clientService.getTupleSpacesState();
        System.out.println(tupleSpacesState + "\n");

    }

    private void sleep(String[] split) {
        if (split.length != 2) {
            this.printUsage();
            return;
        }
        Integer time;

        // checks if input String can be parsed as an Integer
        try {
            time = Integer.parseInt(split[1]);
        } catch (NumberFormatException e) {
            this.printUsage();
            return;
        }

        try {
            Thread.sleep(time * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extrai os valores de delay do comando inserido pelo utilizador.
     */
    private int[] extractDelays(String[] split) {
        int[] delays = {0, 0, 0}; // Default: 0 para todas as réplicas
        int delayCount = Math.min(3, split.length - 2);

        for (int i = 0; i < delayCount; i++) {
            try {
                delays[i] = Integer.parseInt(split[2 + i]);
            } catch (NumberFormatException e) {
                delays[i] = 0;
            }
        }
        return delays;
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                "- put <element[,more_elements]>\n" +
                "- read <element[,more_elements]>\n" +
                "- take <element[,more_elements]>\n" +
                "- getTupleSpacesState <server>\n" +
                "- sleep <integer>\n" +
                "- exit\n");
    }

    private boolean inputIsValid(String[] input) {
        // Tem que ter pelo menos o comando e o tuplo
        if (input.length < 2 || input.length > 5) {
            return false;
        }

        String tuple = input[1];
        if (!tuple.startsWith(BGN_TUPLE) || !tuple.endsWith(END_TUPLE)) {
            return false;
        }

        // Verificar se os argumentos adicionais (delays) são inteiros >= 0
        for (int i = 2; i < input.length; i++) {
            try {
                int delay = Integer.parseInt(input[i]);
                if (delay < 0) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

}
