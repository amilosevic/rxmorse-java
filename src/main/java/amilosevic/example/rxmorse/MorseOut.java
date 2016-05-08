package amilosevic.example.rxmorse;

import java.util.HashMap;

public interface MorseOut {
    /**
     *
     * @param key
     * @return
     */
    boolean in(String key);

    /**
     *
     * @param key
     * @return
     */
    String[] code(String key);
}

/**
 * Created by aca on 4/17/16.
 */
class MorseOutImpl implements MorseConst, MorseOut {

    private final HashMap<String, String[]> map = new HashMap<>();

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public MorseOutImpl() {
        super();

        out("A", dit, dah);
        out("B", dah, dit, dit, dit);
        out("C", dah, dit, dah, dit);
        out("D", dah, dit, dit);
        out("E", dit);
        out("F", dit, dit, dah, dit);
        out("G", dah, dah, dit);
        out("H", dit, dit, dit, dit);
        out("I", dit, dit);
        out("J", dit, dah, dah, dah);
        out("K", dah, dit, dah);
        out("L", dit, dah, dit, dit);
        out("M", dah, dah);
        out("N", dah, dit);
        out("O", dah, dah, dah);
        out("P", dit, dah, dah, dit);
        out("Q", dah, dah, dit, dah);
        out("R", dit, dah, dit);
        out("S", dit, dit, dit);
        out("T", dah);
        out("U", dit, dit, dah);
        out("V", dit, dit, dit, dah);
        out("W", dit, dah, dah);
        out("X", dah, dit, dit, dah);
        out("Y", dah, dit, dah, dah);
        out("Z", dah, dah, dit, dit);

        out("1", dit, dah, dah, dah, dah);
        out("2", dit, dit, dah, dah, dah);
        out("3", dit, dit, dit, dah, dah);
        out("4", dit, dit, dit, dit, dah);
        out("5", dit, dit, dit, dit, dit);
        out("6", dah, dit, dit, dit, dit);
        out("7", dah, dah, dit, dit, dit);
        out("8", dah, dah, dah, dit, dit);
        out("9", dah, dah, dah, dah, dit);
        out("0", dah, dah, dah, dah, dah);

        out("?", dit, dit, dah, dah, dit, dah);


    }

    private void out(String key, String... code) {
        map.put(key, code);
    }

    @Override
    public boolean in(String key) {
        return map.containsKey(key);
    }

    @Override
    public String[] code(String key) {
        return map.get(key);
    }

    /**
     * Created by aca on 4/17/16.
     */

}

