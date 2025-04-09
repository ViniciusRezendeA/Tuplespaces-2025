package pt.ulisboa.tecnico.tuplespaces.server.utils;

public class Validator {
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";

    public static boolean checkHasValidTuple(String tuple) {
        return tuple.startsWith(BGN_TUPLE) && tuple.endsWith(END_TUPLE);
    }
}
