package server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class MonitorServer {

    private static final Logger logger = LogManager.getLogger(MonitorServer.class);

    public static void main(String[] args) throws IOException {
        // 创建HttpServer并绑定到指定端口
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // 设置处理器和线程池
        server.createContext("/monitor", new MonitorHandler());
        server.setExecutor(Executors.newCachedThreadPool()); // 使用线程池处理并发请求

        server.start();
        logger.info("服务器启动，监听端口：8080");
    }

    static class MonitorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // 获取客户端IP地址
                InetSocketAddress remoteAddress = exchange.getRemoteAddress();
                String clientIp = remoteAddress.getAddress().getHostAddress();

                // 尝试获取客户端主机名
                String clientHostName = null;
                try {
                    InetAddress inetAddress = InetAddress.getByName(clientIp);
                    clientHostName = inetAddress.getHostName(); // 可能会抛出异常
                } catch (IOException e) {
                    logger.warn("无法解析客户端主机名: {}", e.getMessage());
                    clientHostName = "未知";
                }

                try (InputStream inputStream = exchange.getRequestBody()) {
                    StringBuilder textBuilder = new StringBuilder();
                    try (Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        int c;
                        while ((c = reader.read()) != -1) {
                            textBuilder.append((char) c);
                        }
                    }

                    String jsonString = textBuilder.toString();

                    // 使用 Log4j 记录日志，包含客户端IP和主机名
                    logger.info("来自 [{} ({})] 的数据:\n{}", clientIp, clientHostName, jsonString);

                    exchange.sendResponseHeaders(200, 0); // OK
                } catch (Exception e) {
                    logger.error("处理请求时出错", e);
                    exchange.sendResponseHeaders(500, 0); // Internal Server Error
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                logger.warn("不允许的方法: {}", exchange.getRequestMethod());
            }

            exchange.close();
        }
    }
}