package io.github.kimmking.gateway.outbound.okhttp;

import io.github.kimmking.gateway.outbound.httpclient4.NamedThreadFactory;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

public class OkhttpOutboundHandler {

    private String url;
    private OkHttpClient httpClient;
    private ExecutorService proxyService;

    public OkhttpOutboundHandler(String url) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5L, TimeUnit.SECONDS)
                .readTimeout(5L, TimeUnit.SECONDS)
                .build();
        int cores = Runtime.getRuntime().availableProcessors() * 2;
        long keepAliveTime = 1000;
        int queueSize = 2048;
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();
        //初始化线程池
        this.proxyService = new ThreadPoolExecutor(cores, cores,
                keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueSize),
                new NamedThreadFactory("proxyService"), handler);
    }

    public void handle(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx) {
        final String url = this.url + fullRequest.uri();
        System.out.println("proxy service:"+url);
        proxyService.submit(() -> httpGet(fullRequest, ctx, url));
    }

    private void httpGet(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx, final String url) {
        Request.Builder builder = new Request.Builder().url(url);

        for (Map.Entry<String, String> header : fullRequest.headers()) {
            builder.addHeader(header.getKey(), header.getValue());
        }
        Request request = builder.get()
                .addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_KEEP_ALIVE)
                .build();
        try (Response response = this.httpClient.newCall(request).execute()) {
            handleResponse(fullRequest, ctx, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleResponse(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx,
                                Response endpointResponse) {
        FullHttpResponse response = null;

        try {
            byte[] body = endpointResponse.body().bytes();
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer(body));
            Headers responseHeaders = endpointResponse.headers();
            response.headers().setInt("Content-Length", Integer.parseInt(responseHeaders.get("Content-Length")));
            //header里塞自定义的key-value
            response.headers().add("nio", fullRequest.headers().get("nio"));
            //将endpoint返回的header信息塞到代理的response对象里
            for (int i = 0; i < responseHeaders.size(); i++) {
                response.headers().set(responseHeaders.name(i), responseHeaders.value(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
            response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
            exceptionCaught(ctx, e);
        } finally {
            if (fullRequest != null) {
                if (!HttpUtil.isKeepAlive(fullRequest)) {
                    ctx.write(response).addListener(ChannelFutureListener.CLOSE);
                } else {
                    ctx.write(response);
                }
            }
            ctx.flush();
        }
    }


    private void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


}
