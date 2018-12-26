import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by yueweihang on 2016/4/1.
 */
public class HttpUtil {
    private static Logger logger = LogManager.getLogger("App");

    public static String get(String url, String queryString) throws Exception {
        String response = null;
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(url);
        try {
            if (StringUtils.isNotBlank(queryString))
                method.setQueryString(URIUtil.encodeQuery(queryString));
            client.executeMethod(method);
            if (method.getStatusCode() == HttpStatus.SC_OK) {
                response = method.getResponseBodyAsString();
            }
        } catch (URIException e) {
            logger.error("执行HTTP Get请求时，编码查询字符串“" + queryString + "”发生异常！", e);
        } catch (IOException e) {
            logger.error("执行HTTP Get请求" + url + "时，发生异常！", e);
        } finally {
            method.releaseConnection();
        }
        return response;
    }

    public static String post(String path, Map<String, String> map) throws Exception {
        // 注意Post地址中是不带参数的，所以newURL的时候要注意不能加上后面的参数
        URL Url = new URL(path);
        // Post方式提交的时候参数和URL是分开提交的，参数形式是这样子的：name=y&age=6
        StringBuilder sb = new StringBuilder();
        // sb.append("?");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            sb.append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        String str = sb.toString();
        HttpURLConnection httpConn = null;
        try {
            httpConn = (HttpURLConnection) Url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setReadTimeout(5000);
            httpConn.setDoOutput(true);
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setRequestProperty("Content-Length", String.valueOf(str.getBytes().length));
            OutputStream os = httpConn.getOutputStream();
            os.write(str.getBytes());
            if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String result = "";
                BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "utf-8"));
                String line;
                while ((line = in.readLine()) != null) {
                    result += line;
                }
                in.close();
                return result;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
        return null;
    }
}
