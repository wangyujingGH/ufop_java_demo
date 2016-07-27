import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.sf.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;

public class ufop_test_server {
    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(9100), 0);
            server.createContext("/uop", new MyHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            System.out.println("listening start");
        } catch(IOException e) {
            System.out.println("throw error");
            System.out.println(e.getMessage());
        }
    }

}

class MyHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
        System.out.println("handler starting");

        InputStream is_req = t.getRequestBody();
        byte[] bytes = new byte[1024];
        ByteBuffer buffer = ByteBuffer.allocate(1024 * 4);
        int n = 0;
        while((n = is_req.read(bytes)) != -1) {
            buffer.put(bytes, 0, n);
        }
        is_req.close();

        //parse request
        JSONObject obj = JSONObject.fromObject(new String(buffer.array(), "UTF-8"));
        System.out.println(obj.toString());
        JSONObject src = obj.getJSONObject("src");
        String url = src.get("url").toString();

        // open url connection
        URL Url = new URL(url);
        HttpURLConnection con = (HttpURLConnection) Url.openConnection();
        con.setDoOutput(true);

        // optional default is GET
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        StringBuffer pre = new StringBuffer("request body : ").append(obj.toString()).append("\n");

        long content_length = Long.valueOf(con.getHeaderField("Content-Length"));
        String content_type = con.getHeaderField("Content-Type");

        OutputStream os = t.getResponseBody();
        StringBuffer sb = new StringBuffer(pre);
        sb.append("The Response content-type is ").append(content_type);

        t.sendResponseHeaders(responseCode, sb.toString().getBytes().length);
        os.write(sb.toString().getBytes("UTF-8"));
        os.flush();

        InputStream is_resp = con.getInputStream();
        byte[] tmp = new byte[1024];
        long sum_num = 0;
        while ((n = is_resp.read(tmp)) != -1) {
            sum_num += n;
        }
        if (sum_num != content_length) {
            throw new IOException("Read response body error from the redirect url");
        }
        is_resp.close();
        os.flush();

        System.out.println("done");
        os.close();
    }
}
