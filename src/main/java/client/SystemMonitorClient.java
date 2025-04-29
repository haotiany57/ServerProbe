package client;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSFileStore;
import java.util.List;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;
import oshi.util.Util;

public class SystemMonitorClient {

    private static final String SERVER_URL = "http://127.0.0.1:8080/monitor"; // 替换为你的服务器地址

    public static void main(String[] args) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(SystemMonitorClient::monitorAndSend, 0, 10, TimeUnit.SECONDS);
    }

    private static void monitorAndSend() {
        Map<String, Object> data = collectSystemData();
        sendToServer(data);
    }

    private static Map<String, Object> collectSystemData() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();

        Map<String, Object> data = new HashMap<>();

        CentralProcessor processor = hardware.getProcessor();
        double cpuLoad = processor.getSystemCpuLoad(1000) * 100;
        Util.sleep(1000); // 确保CPU负载计算准确
        data.put("cpuUsage", String.format("%.2f%%", cpuLoad));

        GlobalMemory memory = hardware.getMemory();
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        double memoryUsage = (double) (totalMemory - availableMemory) / totalMemory * 100;
        data.put("memoryTotal", String.format("%.2f GB", totalMemory / (1024.0 * 1024.0 * 1024.0)));
        data.put("memoryAvailable", String.format("%.2f GB", availableMemory / (1024.0 * 1024.0 * 1024.0)));
        data.put("memoryUsage", String.format("%.2f%%", memoryUsage));

        List<OSFileStore> fileStores = systemInfo.getOperatingSystem().getFileSystem().getFileStores();
        for (OSFileStore fs : fileStores) {
            long totalSpace = fs.getTotalSpace();
            long usableSpace = fs.getUsableSpace();
            double diskUsage = (double) (totalSpace - usableSpace) / totalSpace * 100;
        }

        return data;
    }

    private static void sendToServer(Map<String, Object> data) {
        try {
            URL url = new URL(SERVER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = new Gson().toJson(data); // 使用Gson库将Map转换为JSON字符串

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("数据成功发送到服务器！");
            } else {
                System.out.println("发送失败，响应码：" + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}