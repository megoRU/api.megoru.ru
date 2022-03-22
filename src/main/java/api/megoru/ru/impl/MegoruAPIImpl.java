package api.megoru.ru.impl;

import api.megoru.ru.MegoruAPI;
import api.megoru.ru.entity.*;
import api.megoru.ru.io.DefaultResponseTransformer;
import api.megoru.ru.io.NullResponseException;
import api.megoru.ru.io.ResponseTransformer;
import api.megoru.ru.io.UnsuccessfulHttpException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.HttpUrl;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MegoruAPIImpl implements MegoruAPI {

    private static final HttpUrl baseUrl = new HttpUrl.Builder()
            .scheme("http")
            .host("127.0.0.1") //193.163.203.77
            .port(8085)
            .build();

    private final Gson gson;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private final String token;

    public MegoruAPIImpl(String token) {
        this.token = token;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public Result setListUsers(List<Participants> userList) {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("participants")
                .build();
        JSONArray json = new JSONArray(gson.toJson(userList));
        return post(url, json.toString(), new DefaultResponseTransformer<>(Result.class, gson));
    }

    @Override
    public Participants[] getListUsers(String idUserWhoCreateGiveaway, String giveawayId) {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("get-participants")
                .build();

        JSONObject json = new JSONObject();
        try {
            json.put("idUserWhoCreateGiveaway", idUserWhoCreateGiveaway);
            json.put("giveawayId", giveawayId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return post(url, json.toString(), new DefaultResponseTransformer<>(Participants[].class, gson));
    }

    @Override
    public String[] setWinners(Winners winners) {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("winners")
                .build();

        JSONObject json = new JSONObject();
        try {
            json.put("n", winners.getN());
            json.put("min", winners.getMin());
            json.put("max", winners.getMax());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return post(url, json.toString(), new DefaultResponseTransformer<>(String[].class, gson));
    }

    @Override
    public Word getWord(GameWordLanguage GameWordLanguage) {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("api")
                .addPathSegment("word")
                .build();

        JSONObject json = new JSONObject();
        try {
            json.put("language", GameWordLanguage.getLanguage());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return post(url, json.toString(), new DefaultResponseTransformer<>(Word.class, gson));
    }

    private <E> E get(HttpUrl url, ResponseTransformer<E> responseTransformer) {
        HttpGet request = new HttpGet(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.addHeader(HttpHeaders.AUTHORIZATION, this.token);

        return execute(request, responseTransformer);
    }

    private <E> E post(HttpUrl url, String jsonBody, ResponseTransformer<E> responseTransformer) {
        HttpPost request = new HttpPost(url.uri());
        request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.addHeader(HttpHeaders.AUTHORIZATION, this.token);
        HttpEntity stringEntity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);
        request.setEntity(stringEntity);
        return execute(request, responseTransformer);
    }

    private <E> E execute(HttpRequestBase request, ResponseTransformer<E> responseTransformer) {
        HttpEntity entity;
        String body = null;
        try {
            CloseableHttpResponse response = httpClient.execute(request);

            // Get HttpResponse Status
            System.out.println("Status: " + response.getStatusLine().getStatusCode());

            entity = response.getEntity();

            if (entity == null) {
                throw new NullResponseException();
            }

            body = EntityUtils.toString(entity);

            if (response.getStatusLine().getStatusCode() == 401
                    || response.getStatusLine().getStatusCode() == 404
                    || response.getStatusLine().getStatusCode() == 403) {
                // Get HttpResponse Status
                ErrorResponse result = gson.fromJson(body, ErrorResponse.class);
                throw new UnsuccessfulHttpException(result.getError().getCode(), result.getError().getMessage());
            } else if (response.getStatusLine().getStatusCode() >= 500) {
                throw new Exception("API not work, or connection refused");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseTransformer.transform(body);
    }
}