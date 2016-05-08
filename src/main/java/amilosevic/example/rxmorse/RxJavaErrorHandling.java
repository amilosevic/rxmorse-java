package amilosevic.example.rxmorse;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;

import java.util.concurrent.TimeUnit;

/**
 * Created by aca on 5/8/16.
 */
public class RxJavaErrorHandling {

    public static void main(String[] args) {

        final ConnectableObservable<Long> hot = rx.Observable.interval(1, TimeUnit.SECONDS).publish();
        hot.connect();

        rx.Observable<String> sample = rx.Observable.defer(new Func0<Observable<String>>() {
            @Override
            public Observable<String> call() {
                return hot.map(new Func1<Long, String>() {
                    @Override
                    public String call(Long aLong) {
                        return aLong.toString();
                    }
                }).mergeWith(Observable.concat(
                        Observable.<String>empty().delay(5, TimeUnit.SECONDS),
                        Observable.<String>error(new UnsupportedOperationException("e"))
                ));
            }
        }).doOnSubscribe(new Action0() {
            @Override
            public void call() {
                System.out.println("Subscription!");
            }
        }).doOnNext(new Action1<String>() {
            @Override
            public void call(String s) {
                System.out.println("Do On Next" + s);
            }
        });


        sample.retry(3).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                System.out.println("OnCompleted!");
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("OnError!");
            }

            @Override
            public void onNext(String s) {
                System.out.println(s);
            }
        });


        sample.retry(2).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                System.out.println("(2) OnCompleted!");
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("(2) OnError!");
            }

            @Override
            public void onNext(String s) {
                System.out.println("(2)" + s);
            }
        });


        //publish.connect();
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }

    }
}
