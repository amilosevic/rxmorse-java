package amilosevic.example.rxmorse;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.SwingObservable;
import rx.schedulers.TimeInterval;
import rx.schedulers.Timestamped;
import rx.subjects.PublishSubject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by aleksandar on 2/21/16.
 */
public class RxMorse extends JFrame {

    // window dimensions
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 620;

    // window initial position
    private static final int X = 60;
    private static final int Y = 60;

    // title
    private static final String TITLE = "RxMorse";

    /**
     * Constructs a new frame that is initially invisible.
     * <p/>
     * This constructor sets the component's locale property to the value
     * returned by <code>JComponent.getDefaultLocale</code>.
     *
     * @throws java.awt.HeadlessException if GraphicsEnvironment.isHeadless()
     *                                    returns true.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see java.awt.Component#setSize
     * @see java.awt.Component#setVisible
     * @see javax.swing.JComponent#getDefaultLocale
     */
    public RxMorse() throws HeadlessException {
        super();

        setSize(WIDTH, HEIGHT);
        setLocation(X, Y);
        setResizable(false);
        setTitle(TITLE);

        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        add(new Morse());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new RxMorse();
            }
        });
    }
}

class Morse extends JPanel implements ActionListener {

    public static final String qbf = "THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG 0123456789";
    public static final String sos = "SOS";

    public static final String DOWN = "down";
    public static final String UP = "up";

    public static final String ACTION_WAIT = "wait";
    public static final String ACTION_OUT = "out";

    /**
     * Creates a new <code>JPanel</code> with a double buffer
     * and a flow layout.
     */
    public Morse() {

        final int unit = 130;

        final MorseOut morseOut = new MorseOut();
        final MorseIn morseIn = new MorseIn();

        setFocusable(true);

        // event gatherer

        Observable<String> events = Observable.merge(
                SwingObservable.fromKeyEvents(this).filter(new Func1<KeyEvent, Boolean>() {
                    @Override
                    public Boolean call(KeyEvent keyEvent) {
                        return keyEvent.getID() == Event.KEY_PRESS || keyEvent.getID() == Event.KEY_RELEASE;
                    }
                }).map(new Func1<KeyEvent, String>() {
                    @Override
                    public String call(KeyEvent keyEvent) {
                        switch (keyEvent.getID()){
                            case Event.KEY_PRESS: return "keydown";
                            case Event.KEY_RELEASE: return "keyup";
                            default: throw new RuntimeException("");
                        }
                    }
                }),
                SwingObservable.fromMouseEvents(this).filter(new Func1<MouseEvent, Boolean>() {
                    @Override
                    public Boolean call(MouseEvent mouseEvent) {
                        return mouseEvent.getID() == Event.MOUSE_DOWN || mouseEvent.getID() == Event.MOUSE_UP;
                    }
                }).map(new Func1<MouseEvent, String>() {
                    @Override
                    public String call(MouseEvent mouseEvent) {
                        switch (mouseEvent.getID()){
                            case Event.MOUSE_DOWN: return "mousedown";
                            case Event.MOUSE_UP: return "mouseup";
                            default: throw new RuntimeException("");
                        }
                    }
                })
        );




        // encoder

        final Observable<Timestamped<String>> robot =
                Observable.from(qbf.split(""))
                        .delay(1000, TimeUnit.MILLISECONDS)
                        .concatMap(new Func1<String, Observable<String>>() {
                            @Override
                            public Observable<String> call(String s) {
                                if (morseOut.in(s)) {
                                    return Observable.concat(
                                            Observable.from(morseOut.code(s)),
                                            Observable.just(MorseConst.sss)
                                    );
                                } else {
                                    return Observable.error(new Error("** " + s));
                                }
                            }
                        })
                        .concatMap(new Func1<String, Observable<String>>() {
                            @Override
                            public Observable<String> call(String s) {
                                switch (s) {
                                    case MorseConst.dit:
                                        return Observable.concat(
                                                Observable.just("robotdown"),
                                                Observable.just("robotup").delay(1 * unit, TimeUnit.MILLISECONDS),
                                                Observable.<String>empty().delay(1 * unit, TimeUnit.MILLISECONDS)
                                        );
                                    case MorseConst.dah:
                                        return Observable.concat(
                                                Observable.just("robotdown"),
                                                Observable.just("robotup").delay(3 * unit, TimeUnit.MILLISECONDS),
                                                Observable.<String>empty().delay(1 * unit, TimeUnit.MILLISECONDS)
                                        );

                                    case MorseConst.sss:
                                        return rx.Observable.<String>empty().delay(3 * unit, TimeUnit.MILLISECONDS);
                                    case MorseConst.sssssss:
                                        return rx.Observable.<String>empty().delay(7 * unit, TimeUnit.MILLISECONDS);

                                    default:
                                        return rx.Observable.error(new Error("*** " + s));

                                }
                            }
                        })
                        .timestamp();


        // decoder

        Observable<Timestamped<String>> inputs
                = Observable.merge(events.timestamp(), robot);

        final Observable<String> source = Observable
                .merge(inputs, subjectivize(inputs, unit))
                .map(new Func1<Timestamped<String>, String>() {
                    @Override
                    public String call(Timestamped<String> st) {
                        switch (st.getValue()) {
                            case "mouseup":
                            case "keyup":
                            case "robotup":
                                return UP;
                            case "robotdown":
                            case "mousedown":
                            case "keydown":
                                return DOWN;
                            case MorseConst.ls:
                            case MorseConst.ws:
                            case MorseConst.cr:
                                return st.getValue();
                            default:
                                throw new RuntimeException("!");
                        }
                    }
                });

        final Observable<String> symbols = source
                .timeInterval()
                .map(new Func1<TimeInterval<String>, String>() {
                    @Override
                    public String call(TimeInterval<String> is) {
                        if (is.getValue().equals(UP)) {
                            if (is.getIntervalInMilliseconds() < 1.5 * unit) {
                                return MorseConst.dit;
                            } else {
                                return MorseConst.dah;
                            }
                        } else if (is.getValue().equals(DOWN)) {
                            return null;
                        } else if (is.getValue().equals(MorseConst.ls)) {
                            return MorseConst.ls;
                        } else if (is.getValue().equals(MorseConst.ws)) {
                            return MorseConst.ws;
                        } else if (is.getValue().equals(MorseConst.cr)) {
                            return MorseConst.ws;
                        } else {
                            System.out.println("unhandled: " + is.getValue());
                            return null;
                        }
                    }
                })
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return s != null;
                    }
                });

        final Observable<String> out = symbols
                .scan(
                        new Wait(MorseIn.TOP),
                        new Func2<Action, String, Action>() {
                            @Override
                            public Action call(Action acc, String s) {
                                switch (s) {
                                    case MorseConst.ls:
                                        return new Out(acc.state);
                                    case MorseConst.ws:
                                        return Out.WS;
                                    case MorseConst.cr:
                                        return Out.CR;

                                    case MorseConst.dit:
                                    case MorseConst.dah:
                                        final String key = acc instanceof Out ? MorseIn.TOP : acc.state;

                                        return new Wait(morseIn.down(key, s));

                                    default:
                                        throw new RuntimeException("!!");

                                }
                            }
                        }
                ).filter(new Func1<Action, Boolean>() {
                    @Override
                    public Boolean call(Action action) {
                        return action instanceof Out;
                    }
                }).map(new Func1<Action, String>() {
                    @Override
                    public String call(Action action) {
                        return action.state;
                    }
                });


        out.subscribe(
                new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        System.out.print(o.toString());
                    }
                },
                new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                },
                new Action0() {
                    @Override
                    public void call() {
                        System.out.println("OnComplete");
                    }
                }
        );


    }

    /**
     * Invoked when an action occurs.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    protected Observable<Timestamped<String>> subjectivize(Observable<Timestamped<String>> observable, final int unit) {
        final PublishSubject<Timestamped<String>> subject = PublishSubject.create();

        final State s = new State();

        observable.subscribe(
                new Action1<Timestamped<String>>() {
                    @Override
                    public void call(Timestamped<String> ts) {
                        s.last = ts.getTimestampMillis();

                        if (ts.getValue().endsWith(UP)) {
                            final Timestamped<String> mod3 = new Timestamped<>(ts.getTimestampMillis(), MorseConst.ls);
                            final Timestamped<String> mod7 = new Timestamped<>(ts.getTimestampMillis(), MorseConst.ws);
                            final Timestamped<String> mod20 = new Timestamped<>(ts.getTimestampMillis(), MorseConst.cr);

                            Observable.just(mod3)
                                    .delay((long) (3 * unit * 0.9), TimeUnit.MILLISECONDS)
                                    .subscribe(new Action1<Timestamped<String>>() {
                                        @Override
                                        public void call(Timestamped<String> ts1) {
                                            if (ts1.getTimestampMillis() == s.last) {
                                                subject.onNext(ts1);
                                                if (s.completed) {
                                                    subject.onCompleted();
                                                }
                                            }

                                        }
                                    });

                            Observable.just(mod7)
                                    .delay((long) (7 * unit * 0.9), TimeUnit.MILLISECONDS)
                                    .subscribe(new Action1<Timestamped<String>>() {
                                        @Override
                                        public void call(Timestamped<String> ts1) {
                                            if (ts1.getTimestampMillis() == s.last) {
                                                subject.onNext(ts1);
                                                if (s.completed) {
                                                    subject.onCompleted();
                                                }
                                            }

                                        }
                                    });

                            Observable.just(mod20)
                                    .delay((long) (20 * unit * 0.9), TimeUnit.MILLISECONDS)
                                    .subscribe(new Action1<Timestamped<String>>() {
                                        @Override
                                        public void call(Timestamped<String> ts1) {
                                            if (ts1.getTimestampMillis() == s.last) {
                                                subject.onNext(ts1);
                                                if (s.completed) {
                                                    subject.onCompleted();
                                                }
                                            }

                                        }
                                    });

                        }


                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        subject.onError(throwable);
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        if (!s.completed && s.last == null) {
                            subject.onCompleted();
                        } else {
                            s.completed = true;
                        }

                    }
                });
        return subject;
    }
}

final class State {
    Long last = null;
    boolean completed = false;
}


interface MorseConst {
    static final String dit = "=";
    static final String dah = "===";

    static final String s = "."; // space
    static final String sss = "..."; // letter space
    static final String sssssss = "......."; // word space

    static final String ss = "SS";
    static final String ls = "LS";
    static final String ws = "WS";
    static final String cr = "CR";


}

class MorseIn implements MorseConst {

    public static final String TOP = "__top__";
    public static final String ERR = "__err__";
    private final HashMap<String, Node> map = new HashMap<>();

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public MorseIn() {
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

        //in(ERR, ERR, ERR);

    }

    private void in(final String node, final String left, final String right) {
        map.put(node, new Node(left, right));
    }

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


class MorseOut implements MorseConst {

    private final HashMap<String, String[]> map = new HashMap<>();

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public MorseOut() {
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

        out(" ", sssssss);

    }

    private void out(String key, String... code) {
        map.put(key, code);
    }

    public boolean in(String key) {
        return map.containsKey(key);
    }

    public String[] code(String key) {
        return map.get(key);
    }

}

abstract class Action {

    public final String state;

    protected Action(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Action{" +
                "action='" + this.getClass().getSimpleName() + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}

final class Wait extends Action {

    public Wait(String state) {
        super(state);
    }
}

final class Out extends Action {

    static final Out WS = new Out(" ");
    static final Out CR = new Out("\n");

    public Out(String state) {
        super(state);
    }
}
