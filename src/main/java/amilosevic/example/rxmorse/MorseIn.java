package amilosevic.example.rxmorse;

import java.util.HashMap;

/**
 * Created by aca on 4/17/16.
 */
public interface MorseIn {

    /**
     *
     */
    public static final String TOP = "__top__";

    /**
     *
     * @param key
     * @param symbol
     * @return
     */
    String down(String key, String symbol);
}


/**
 * Created by aca on 4/17/16.
 */

class MorseInImpl implements MorseConst, MorseIn {


    public static final String ERR = "__err__";
    private final HashMap<String, Node> map = new HashMap<>();

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public MorseInImpl() {
        super();
        // 0 level
        in(TOP, "T", "E");

        // I level
        in("T", "M", "N");
        in("E", "A", "I");

        // II level
        in("M", "O", "G");
        in("N", "K", "D");
        in("A", "W", "R");
        in("I", "U", "S");

        // III level
        in("O", "CH", "Ö");
        in("G", "Q", "Z");
        in("K", "Y", "C");
        in("D", "X", "B");
        in("W", "J", "P");
        in("R", "Ä", "L");
        in("U", "Ü", "F");
        in("S", "V", "H");

        // IV level
        in("CH", "0", "9");
        in("Ö", ERR, "8");
        in("Q", "Ñ", "Ĝ");
        in("Z", ERR, "7");
        in("Y", ERR, "Ĥ");
        in("C", ERR, "Ç");
        in("X", ERR, "/");
        in("B", "=", "6");
        in("J", "1", "Ĵ");
        in("P", "Á", "Þ");
        in("Ä", ERR, "+");
        in("L", "È", ERR);
        in("Ü", "2", "Đ");
        in("F", ERR, "É");
        in("V", "3", "Ŝ");
        in("H", "4", "5");

    }

    private void in(final String node, final String left, final String right) {
        map.put(node, new Node(left, right));
    }

    @Override
    public String down(final String key, final String symbol) {
        if (!map.containsKey(key)) {
            return ERR;
        }

        final Node node = map.get(key);
        if (symbol.equals(MorseConst.dah)) {
            return node.dah;
        } else if (symbol.equals(MorseConst.dit)) {
            return node.dit;
        } else {
            throw new RuntimeException("node");
        }
    }

    private static class Node {
        public final String dah;
        public final String dit;

        public Node(String dah, String dit) {
            this.dah = dah;
            this.dit = dit;
        }
    }
}
