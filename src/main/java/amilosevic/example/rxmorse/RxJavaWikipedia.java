package amilosevic.example.rxmorse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.net.URI;

import java.util.concurrent.Future;

/**
 * Created by aca on 4/24/16.
 */
public class RxJavaWikipedia {

    private static HttpHost host = HttpHost.create("https://en.wikipedia.org");

    static boolean done = false;

    public static class WikipediaSearchResult {

        public final String title;

        public final String snippet;

        public WikipediaSearchResult(String title, String snippet) {
            this.title = title;
            this.snippet = snippet;
        }

        @Override
        public String toString() {
            return title + " -> " + snippet;
        }
    }

    public static void main(String[] args) throws Exception {
        final CloseableHttpAsyncClient httpclient = HttpAsyncClients.createMinimal();

        try {
            httpclient.start();

            final String[] search = {"John Coltrane", "Wayne Shorter", "Miles Davis", "Herbie Hancock", "Sonny Rollins", "Thelonious Monk" };

            rx.Observable<WikipediaSearchResult> results =
                rx.Observable.from(search)
                .map(new Func1<String, Future<HttpResponse>>() {
                    @Override
                    public Future<HttpResponse> call(String s) {
                        try {
                            URI uri = new URIBuilder()
                                    .setPath("/w/api.php")
                                    .setParameter("action", "query")
                                    .setParameter("list", "search")
                                    .setParameter("srsearch", s)
                                    .setParameter("srlimit", "3")
                                    .setParameter("format", "json")
                                    .build();

                            HttpGet request = new HttpGet(uri);
                            return httpclient.execute(host, request, null);

                        } catch (Exception e) {
                            return null;
                        }
                    }
                })
                .flatMap(new Func1<Future<HttpResponse>, rx.Observable<HttpResponse>>() {
                    @Override
                    public rx.Observable<HttpResponse> call(Future<HttpResponse> httpResponseFuture) {
                        return rx.Observable.from(httpResponseFuture, Schedulers.io());
                    }
                })
                .flatMap(new Func1<HttpResponse, rx.Observable<WikipediaSearchResult>>() {
                    @Override
                    public rx.Observable<WikipediaSearchResult> call(HttpResponse httpResponse) {
                        try {
                            HttpEntity entity = httpResponse.getEntity();

                            System.out.println("Content-Type: " + httpResponse.getFirstHeader("Content-Type").getValue());
                            //System.out.println("Entity: " + EntityUtils.toString(entity));

                            JSONParser parser = new JSONParser();
                            JSONObject jsonObject = (JSONObject) parser.parse(EntityUtils.toString(entity));

                            JSONObject queryJsonObject = (JSONObject) jsonObject.get("query");
                            JSONArray searchJsonArray = (JSONArray) queryJsonObject.get("search");


                            final rx.Observable<JSONObject> from = rx.Observable.from(searchJsonArray);

                            return from.map(new Func1<JSONObject, WikipediaSearchResult>() {
                                @Override
                                public WikipediaSearchResult call(JSONObject searchJsonItem) {

                                    final String title = (String) searchJsonItem.get("title");
                                    final String snippet = (String) searchJsonItem.get("snippet");

                                    return new WikipediaSearchResult(title, snippet);
                                }
                            });
                        } catch (Exception e) {
                            return rx.Observable.error(e);
                        }
                    }
                });


            // subscribe

            results.subscribe(new Action1<WikipediaSearchResult>() {
                @Override
                public void call(WikipediaSearchResult wikipediaSearchResult) {
                    System.out.println(wikipediaSearchResult.toString());
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    throwable.printStackTrace();
                    done();
                }
            }, new Action0() {
                @Override
                public void call() {
                    System.out.println("Done!");
                    done();
                }
            });



            while (true) {
                Thread.sleep(10_000);
                if (done) {
                    break;
                }
            }

            System.out.println("Exit!");

        } finally {
            httpclient.close();
        }

    }

    private static void done() {
        done = true;
    }

}
