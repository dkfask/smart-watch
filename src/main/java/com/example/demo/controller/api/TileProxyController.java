package com.example.demo.controller.api;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * TileProxyController
 * 简单的瓦片代理：把前端 /api/tiles/{provider}/{z}/{x}/{y}.{ext} 请求转发到已知的瓦片提供商。
 * 目的：在网络或访问受限环境中，通过后端代理瓦片请求以避免 CORS 或被阻断问题。
 *
 * 注：此代理仅作轻量级开发/排查用途，生产请考虑缓存与访问控制（防止滥用）。
 */
@RestController
@RequestMapping("/api/tiles")
public class TileProxyController {

    private final HttpClient client;
    private final Map<String, String> providers = new HashMap<>();

    public TileProxyController() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();

        // 可选的 provider 模板；URL 中使用 {z},{x},{y},{ext} 占位
        providers.put("osm", "https://tile.openstreetmap.org/{z}/{x}/{y}.{ext}");
        providers.put("stamenToner", "https://stamen-tiles-a.a.ssl.fastly.net/toner/{z}/{x}/{y}.{ext}");
        providers.put("stamenWatercolor", "https://stamen-tiles-a.a.ssl.fastly.net/watercolor/{z}/{x}/{y}.{ext}");
        providers.put("cartoPositron", "https://a.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png");
        providers.put("esri", "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}");
    }

    @GetMapping("/{provider}/{z}/{x}/{y}.{ext}")
    public ResponseEntity<byte[]> proxyTile(
            @PathVariable String provider,
            @PathVariable String z,
            @PathVariable String x,
            @PathVariable String y,
            @PathVariable String ext
    ) {
        String template = providers.getOrDefault(provider, providers.get("osm"));
        if (template == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        String target = template.replace("{z}", z).replace("{x}", x).replace("{y}", y).replace("{ext}", ext);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(target))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "tile-proxy/1.0")
                    .GET()
                    .build();

            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            int status = resp.statusCode();
            if (status >= 200 && status < 300) {
                byte[] body = resp.body();
                HttpHeaders headers = new HttpHeaders();
                String contentType = resp.headers().firstValue("content-type").orElse("application/octet-stream");
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).getHeaderValue());
                return new ResponseEntity<>(body, headers, HttpStatus.OK);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .header("X-Proxy-Status", String.valueOf(status))
                        .body(null);
            }
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(null);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}

