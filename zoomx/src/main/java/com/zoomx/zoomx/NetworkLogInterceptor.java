package com.zoomx.zoomx;

import com.zoomx.zoomx.manager.ZoomX;
import com.zoomx.zoomx.model.HeaderViewModel;
import com.zoomx.zoomx.model.RequestEntity;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * Created by Ahmed Fathallah on 11/19/2017.
 */

public class NetworkLogInterceptor implements Interceptor {

    private static final Charset UTF_8 = Charset.forName("utf-8");

    @Override
    public Response intercept(Chain chain) throws IOException {
        RequestEntity request = new RequestEntity();

        Request retrofitRequest = chain.request();
        RequestBody requestBody = retrofitRequest.body();

        boolean hasRequestBody = requestBody != null;

        request.setMethod(retrofitRequest.method());
        request.setUrl(retrofitRequest.url().toString());
        request.setRequestBody(hasRequestBody ? requestBody.toString() : "");
        request.setRquestHeaders(getHeaders(retrofitRequest.headers()));

        long startNs = System.nanoTime();
        Response response = null;
        try {
            response = chain.proceed(retrofitRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        request.setStartDate(new Date(startNs));
        request.setTookTime(tookMs);
        if (response != null) {
            request.setResponseHeaders(getHeaders(response.headers()));
            request.setCode(response.code());
            request.setResponseBody(responseBody(response));
        }

        ZoomX.getRequestDao().insertRequest(request);
        return response;
    }

    private HeaderViewModel getHeaders(Headers headers) {
        HeaderViewModel model = new HeaderViewModel();
        if (headers != null) {
            for (int i = 0; i < headers.size(); i++) {
                model.addHeader(headers.name(i), headers.value(i));
            }
        }
        return model;
    }

    private String responseBody(Response response) throws IOException {
        ResponseBody responseBody = response.body();
        String body = "";
        if (responseBody != null) {
            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE);
            Buffer buffer = source.buffer();

            Charset charset = UTF_8;
            MediaType contentType = responseBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(UTF_8);
            }
            body = buffer.clone().readString(charset);
        }
        return body;
    }
}