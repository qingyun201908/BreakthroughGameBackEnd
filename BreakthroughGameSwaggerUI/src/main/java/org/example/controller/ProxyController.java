package org.example.controller;


import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.example.properties.DocsProperties;
import org.springframework.http.*;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/proxy")
public class ProxyController {

    private final Map<String, URI> routes = new LinkedHashMap<>();
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Set<String> HOP_BY_HOP = Set.of(
            "connection","keep-alive","proxy-authenticate","proxy-authorization",
            "te","trailers","transfer-encoding","upgrade","host","content-length"
    );

    private final DocsProperties props;

    public ProxyController(DocsProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        for (DocsProperties.Service s : props.getServices()) {
            routes.put(s.getId(), s.getBaseUrl());
        }
    }

    @GetMapping("/{service}/**")
    public ResponseEntity<byte[]> proxyGet(
            @PathVariable String service,
            HttpServletRequest request,
            @RequestHeader HttpHeaders incomingHeaders
    ) throws IOException, InterruptedException {
        URI base = routes.get(service);
        if (base == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(("Unknown service: " + service).getBytes());
        }

        // 解析通配符剩余路径
        String pathWithinHandler = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern   = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String remainder = new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, pathWithinHandler);

        // 拼 URI（保留查询串）
        String q = request.getQueryString();
        String sep1 = base.getPath().endsWith("/") ? "" : "/";
        String sep2 = (remainder == null || remainder.isEmpty()) ? "" : "";
        String target = base.toString() + sep1 + (remainder == null ? "" : remainder) + (q == null ? "" : "?" + q);

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(target))
                .timeout(Duration.ofSeconds(10))
                .GET();

        // 透传必要的请求头（排除 hop-by-hop）
        incomingHeaders.forEach((k, v) -> {
            String lk = k.toLowerCase(Locale.ROOT);
            if (!HOP_BY_HOP.contains(lk)) {
                for (String vv : v) b.header(k, vv);
            }
        });
        // OpenAPI 通常返回 JSON
        b.header("Accept", String.join(",", List.of(
                "application/json",
                "application/yaml;q=0.8",
                "*/*;q=0.1"
        )));

        HttpResponse<byte[]> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());

        HttpHeaders out = new HttpHeaders();
        resp.headers().map().forEach((k, v) -> {
            String lk = k.toLowerCase(Locale.ROOT);
            if (!HOP_BY_HOP.contains(lk)) {
                out.put(k, v);
            }
        });

        // 确保 Content-Type 可用（某些实现可能未显式设置）
        if (!out.containsKey(HttpHeaders.CONTENT_TYPE)) {
            out.setContentType(MediaType.APPLICATION_JSON);
        }

        return ResponseEntity.status(resp.statusCode()).headers(out).body(resp.body());
    }
}
