package amilosevic.example.rxmorse;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.ConnectableObservable;
import rx.observables.SwingObservable;
import rx.schedulers.Schedulers;
import rx.schedulers.SwingScheduler;
import rx.schedulers.TimeInterval;
import rx.schedulers.Timestamped;
import rx.subjects.PublishSubject;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by aleksandar on 2/21/16.
 */
public class RxMorse extends JFrame {

    // window dimensions
    public static final int WIDTH = 780;
    public static final int HEIGHT = 600;

    // window initial position
    private static final int X = 60;
    private static final int Y = 60;

    // title
    public static final String TITLE = "RxMorse";

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

        final MorseOut morseOut = new MorseOutImpl();
        final MorseIn morseIn = new MorseInImpl();

        //super();
        setSize(new Dimension(WIDTH, HEIGHT));
        add(new Morse(morseIn, morseOut));

        setLocationRelativeTo(null);
        setResizable(false);
        setTitle(TITLE);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new RxMorse().setVisible(true);
            }
        });
    }
}

class Morse extends JPanel implements ActionListener {

    public static final String qbf = "THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG 0123456789";
    public static final String sos = "SOS";

    public static final String DOWN = "down";
    public static final String UP = "up";

    private Color background = Color.WHITE;

    /**
     * Creates a new <code>JPanel</code> with a double buffer
     * and a flow layout.
     */
    public Morse(final MorseIn morseIn, final MorseOut morseOut) {
        super();

        final JTextArea ticker = new JTextArea();
        final JScrollPane pane = new JScrollPane(ticker);
        ticker.setLineWrap(true);
        ticker.setFocusable(false);
        ticker.setEditable(false);

        final JTextField text = new JTextField();

        final JButton button = new JButton("Send SOS!");

        final JPanel image = new JPanel();
        image.add(new JLabel(new ImageIcon(image())));
        image.setBackground(Color.WHITE);

        final JPanel pad = new JPanel() {

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(background);
                g.fillRect(0,0,250,250);
                Toolkit.getDefaultToolkit().sync();
            }
        };
        pad.setOpaque(true);
        pad.setBackground(background);
        pad.setFocusable(true);


        setLayout(null);

        add(pad);
        add(pane);
        add(image);
        add(text);
        add(button);

        pad.setBounds(10, 10, 250, 250);
        image.setBounds(260 + 10, 10, 500, 520);
        text.setBounds(10, 10 + 520 + 10, 600, 20);
        button.setBounds(10 + 600 + 10, 10 + 520 + 10, 150, 20);
        pane.setBounds(10, 260 + 10, 250, 260);

        // mechanics
        final int unit = 150;

        final Clip clip = speaker();



        // event gatherer

        final Observable<String> texts = SwingObservable.fromKeyEvents(text).filter(new Func1<KeyEvent, Boolean>() {
            @Override
            public Boolean call(KeyEvent keyEvent) {
                return keyEvent.getID() == KeyEvent.KEY_RELEASED && keyEvent.getKeyChar() == '\n';
            }
        }).map(new Func1<KeyEvent, String>() {
            @Override
            public String call(KeyEvent keyEvent) {
                final String text1 = text.getText();
                text.setText("");
                return text1.toUpperCase();
            }
        });

        final Observable<String> clicks = SwingObservable.fromButtonAction(button).filter(new Func1<ActionEvent, Boolean>() {
            @Override
            public Boolean call(ActionEvent actionEvent) {
                return actionEvent.getID() == ActionEvent.ACTION_PERFORMED;
            }
        }).map(new Func1<ActionEvent, String>() {
            @Override
            public String call(ActionEvent actionEvent) {
                return sos;
            }
        });

        final Observable<String> clicksAndTexts = Observable.merge(clicks, texts);

        final Observable<String> events = Observable.merge(
                SwingObservable.fromKeyEvents(pad).filter(new Func1<KeyEvent, Boolean>() {
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
                SwingObservable.fromMouseEvents(pad).filter(new Func1<MouseEvent, Boolean>() {
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
        ).distinctUntilChanged();




        // encoder

        final Observable<String> flatten = clicksAndTexts.delay(500, TimeUnit.MILLISECONDS)
                .concatMap(new Func1<String, Observable<? extends String>>() {
                    @Override
                    public Observable<? extends String> call(String s) {
                        return Observable.concat(
                                Observable.from(s.split("")),
                                Observable.just("\n")
                            );
                    }
                });

        final Observable<String> robot =
                flatten
                        .concatMap(new Func1<String, Observable<String>>() {
                            @Override
                            public Observable<String> call(String s) {
                                if (morseOut.in(s)) {
                                    return Observable.concat(
                                            Observable.from(morseOut.code(s)),
                                            Observable.just(MorseConst.sx3)
                                    );
                                } else if (" ".equals(s)) {
                                    return Observable.just(MorseConst.sx7);
                                } else if ("\n".equals(s)) {
                                    return Observable.just(MorseConst.sx20);
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

                                    case MorseConst.sx3:
                                        return rx.Observable.<String>empty().delay(3 * unit, TimeUnit.MILLISECONDS);
                                    case MorseConst.sx7:
                                        return rx.Observable.<String>empty().delay(7 * unit, TimeUnit.MILLISECONDS);
                                    case MorseConst.sx20:
                                        return rx.Observable.<String>empty().delay(20 * unit, TimeUnit.MILLISECONDS);

                                    default:
                                        return rx.Observable.error(new Error("*** " + s));

                                }
                            }
                        });


        // decoder

        Observable<String> inputs = Observable.merge(events, robot);

        final ConnectableObservable<String> source = /*subjectivize(inputs, unit) */ inputs.lift(new OperatorCompleteMorse(unit))
                .map(new Func1<String, String>() {

                    @Override
                    public String call(String st) {
                        switch (st) {
                            case "mouseup":
                            case "keyup":
                            case "robotup":
                                return UP;
                            case "robotdown":
                            case "mousedown":
                            case "keydown":
                                return DOWN;
                            case MorseConst.sx3:
                                return MorseConst.ls;
                            case MorseConst.sx7:
                                return MorseConst.ws;
                            case MorseConst.sx20:
                                return MorseConst.cr;
                            default:
                                throw new RuntimeException("!");
                        }
                    }
                }).publish();

        source.subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                if (s.equals(DOWN)) {
                    background = Color.BLACK;
                } else if (s.equals(UP)) {
                    background = Color.WHITE;
                }
            }
        });


        if (clip != null) {
            source.subscribe(new Action1<String>() {
                @Override
                public void call(String s) {
                    if (s.equals(DOWN)) {
                        clip.setMicrosecondPosition(0);
                        clip.start();
                    } else if (s.equals(UP)) {
                        clip.stop();
                    }
                }
            });
        }

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
                            return MorseConst.cr;
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

        out.observeOn(SwingScheduler.getInstance()).subscribe(
                new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        ticker.append(o.toString());
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

        // to smooth animation
        Timer timer = new Timer(25, this);
        timer.start();

        source.connect();

    }

    private BufferedImage image() {
        try {
            return ImageIO.read(getClass().getClassLoader().getResourceAsStream("International_Morse_Code.PNG"));
        } catch (IOException io) {

        }
        return null;
    }

    private Clip speaker() {

        try {
            Clip clip = AudioSystem.getClip();
            final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("wavTones.com.unregistred.sin_500Hz_-20dBFS_3s.wav");
            AudioInputStream stream = AudioSystem.getAudioInputStream(resourceAsStream);

            clip.open(stream);

            return clip;
        } catch (LineUnavailableException e) {

        } catch (UnsupportedAudioFileException ae) {

        } catch (IOException ioe) {

        }

        return null;
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

    
    protected Observable<String> completeMorseUsingSubjects(Observable<String> observable, final int unit) {
        final PublishSubject<String> subject = PublishSubject.create();

        final State s = new State();

        observable.timestamp().subscribe(
                new Action1<Timestamped<String>>() {
                    @Override
                    public void call(Timestamped<String> ts) {
                        s.last = ts.getTimestampMillis();
                        subject.onNext(ts.getValue());

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
                                                subject.onNext(ts1.getValue());
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
                                                subject.onNext(ts1.getValue());
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
                                                subject.onNext(ts1.getValue());
                                                if (s.completed) {
                                                    subject.onCompleted();
                                                }
                                            }

                                        }
                                    });

                        }


                    }
                }
                ,new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        subject.onError(throwable);
                    }
                },
                new Action0() {
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

    private static class OperatorCompleteMorse implements Observable.Operator<String, String> {

        final Scheduler scheduler;
        final int unit;

        public OperatorCompleteMorse(int unit) {
            this(unit, Schedulers.computation());

        }

        public OperatorCompleteMorse(int unit, Scheduler scheduler) {
            this.scheduler = scheduler;
            this.unit = unit;
        }

        @Override
        public Subscriber<? super String> call(final Subscriber<? super String> child) {
            final Scheduler.Worker worker = scheduler.createWorker();
            child.add(worker);

            return new Subscriber<String>(child) {

                boolean completed = false;
                boolean error = false;
                Long last = null;


                @Override
                public void onCompleted() {
                    worker.schedule(new Action0() {
                        @Override
                        public void call() {

                            if (!error && last == null && !completed) {
                                child.onCompleted();
                            }

                            if (!completed && !error) {
                                completed = true;
                            }
                        }
                    });

                }

                @Override
                public void onError(final Throwable e) {
                    worker.schedule(new Action0() {
                        @Override
                        public void call() {
                            if (!completed && !error) {
                                error = true;
                                child.onError(e);
                                worker.unsubscribe();
                            }

                        }
                    });

                }

                @Override
                public void onNext(final String s) {
                    final long time = scheduler.now();
                    last = time;
                    worker.schedule(new Action0() {
                        @Override
                        public void call() {
                            if (!completed && !error) {
                                child.onNext(s);
                            }
                        }
                    });


                    // schedule LS
                    worker.schedule(new Action0() {
                        @Override
                        public void call() {

                            if (!error && last != null && time == last) {
                                child.onNext(MorseConst.sx3);
                            }
                        }
                    }, (long) (3*unit*1.05), TimeUnit.MILLISECONDS);

                    // schedule WS
                    worker.schedule(new Action0() {
                        @Override
                        public void call() {
                            if (!error && last != null && time == last) {
                                child.onNext(MorseConst.sx7);
                            }
                        }
                    }, (long) (7*unit*1.05), TimeUnit.MILLISECONDS);


                    // schedule CR
                    worker.schedule(new Action0() {
                        @Override
                        public void call() {

                            if (!error && last != null && time == last) {
                                child.onNext(MorseConst.sx20);
                                if (completed) {
                                    child.onCompleted();
                                }
                            }
                        }
                    }, (long) (20*unit*1.05), TimeUnit.MILLISECONDS);

                }
            };

        }
    }

    String threadDescription(String whereAreWe) {
        return "[" + Thread.currentThread().getName() + "] " + whereAreWe;
    }
}

final class State {
    Long last = null;
    boolean completed = false;
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
