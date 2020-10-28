import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * @description:
 * @author: Xu Shiwei
 * @create: 2020-10-28
 **/
public class HttpDemo {
    public static void main(String[] args) {

        String res = doGet("http://www.baidu.com");
        System.out.println(res);
    }

    private static String doGet(String url) {

        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpRequestBase reqBase = new HttpGet(url);

        // 响应对象
        CloseableHttpResponse res = null;
        // 响应内容
        String resCtx = null;

        try {
            res = httpClient.execute(reqBase);

            if (res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new Exception("HTTP访问异常:" + res.getStatusLine() + "url-->>>" + url);
            }

            HttpEntity httpEntity = res.getEntity();
            if (httpEntity != null) {
                resCtx = EntityUtils.toString(httpEntity, "utf-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (res != null) {
                try {
                    res.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return resCtx;
    }
}