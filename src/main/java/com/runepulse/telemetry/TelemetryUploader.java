package com.runepulse.telemetry;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.runepulse.telemetry.model.BaselineSnapshot;
import com.runepulse.telemetry.model.BossKcSnapshot;
import com.runepulse.telemetry.model.GearSnapshot;
import com.runepulse.telemetry.model.XpSnapshot;
import java.io.IOException;
import javax.inject.Inject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import net.runelite.client.config.ConfigManager;

public class TelemetryUploader
{
    private static final MediaType JSON = MediaType.parse("application/json");
    private static final String BASE_URL = "https://runepulse-production.up.railway.app";

    @Inject
    private OkHttpClient httpClient;

    @Inject
    private Gson gson;

    @Inject
    private TelemetryConfig config;

    @Inject
    private ConfigManager configManager;

    public void send(XpSnapshot snapshot)
    {
        postJson(buildUrl("/ingest/xp"), gson.toJson(snapshot));
    }

    public void sendBaseline(BaselineSnapshot snapshot)
    {
        postJson(buildUrl("/ingest/baseline"), gson.toJson(snapshot));
    }

    public void sendGear(GearSnapshot snapshot)
    {
        postJson(buildUrl("/ingest/gear"), gson.toJson(snapshot));
    }

    public void sendBossKc(BossKcSnapshot snapshot)
    {
        postJson(buildUrl("/ingest/boss-kc"), gson.toJson(snapshot));
    }

    public void sendVisibility(boolean isPublic)
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("isPublic", isPublic);
        postJson(buildUrl("/profile/visibility"), gson.toJson(obj));
    }

    public String pair()
    {
        String url = buildUrl("/pair");
        if (url == null || url.isBlank())
        {
            return null;
        }

        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(JSON, "{}"))
            .build();

        try (Response response = httpClient.newCall(request).execute())
        {
            if (!response.isSuccessful() || response.body() == null)
            {
                return null;
            }

            JsonObject obj = gson.fromJson(response.body().string(), JsonObject.class);
            if (obj == null || !obj.has("token"))
            {
                return null;
            }

            return obj.get("token").getAsString();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private String buildUrl(String path)
    {
        String trimmed = BASE_URL;
        if (trimmed.endsWith("/"))
        {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed + path;
    }

    private void postJson(String url, String json)
    {
        if (url == null || url.isBlank())
        {
            return;
        }

        RequestBody body = RequestBody.create(JSON, json);

        Request.Builder builder = new Request.Builder()
            .url(url)
            .post(body);

        String token = configManager.getConfiguration("runepulse", "apiToken");
        if (token != null && !token.isBlank())
        {
            builder.header("Authorization", "Bearer " + token.trim());
        }

        httpClient.newCall(builder.build()).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                response.close();
            }
        });
    }
}


