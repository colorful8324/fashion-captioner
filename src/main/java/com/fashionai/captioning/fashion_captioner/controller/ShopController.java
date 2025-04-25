package com.fashionai.captioning.fashion_captioner.controller;

import com.fashionai.captioning.fashion_captioner.model.Caption;
import com.fashionai.captioning.fashion_captioner.repository.CaptionRepository;
import com.fashionai.captioning.fashion_captioner.utils.MultipartInputStreamFileResource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
public class ShopController {

    private final CaptionRepository captionRepository;
    private final RestTemplate restTemplate;
    @Value("${ai.caption.url}")
    private String aiCaptionUrl;
    @Value("") // TODO: them vao sau
    private String aiAdviceUrl;
    @Value("") // TODO: them vao sau
    private String aiQueryUrl;

    public ShopController(CaptionRepository captionRepository, RestTemplate restTemplate) {
        this.captionRepository = captionRepository;
        this.restTemplate = restTemplate;
    }

    @GetMapping({"/", "index"})
    public String home() {
        return "shop/index";
    }

    @GetMapping("/shop")
    public String shop() {
        return "shop/shop";
    }

    @GetMapping("/about")
    public String about() {
        return "shop/about";
    }

    @GetMapping("/blog")
    public String blog() {
        return "shop/blog";
    }

    @GetMapping("/caption")
    public String caption() {
        return "shop/caption";
    }

    @GetMapping("/cart")
    public String cart() {
        return "shop/cart";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "shop/checkout";
    }

    @GetMapping("/contact")
    public String contact() {
        return "shop/contact";
    }

    @GetMapping("/services")
    public String services() {
        return "shop/services";
    }

    private HttpEntity<MultiValueMap<String, Object>> buildMultipartRequest(List<MultipartFile> files) throws Exception {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (MultipartFile file : files) {
            body.add("files", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return new HttpEntity<>(body, headers);
    }

    @PostMapping("/images/gen-cap")
    public ResponseEntity<?> generateCaptions(@RequestParam("images") List<MultipartFile> images) {
        if (images.size() > 100) {
            return ResponseEntity.badRequest().body(List.of("Chỉ được phép upload tối đa 100 ảnh."));
        }

        try {
            HttpEntity<MultiValueMap<String, Object>> requestEntity = buildMultipartRequest(images);

            ResponseEntity<Map> response = restTemplate.exchange(
                aiCaptionUrl, HttpMethod.POST, requestEntity, Map.class);

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");

            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append("Filename,Caption\n");

            for (Map<String, Object> result : results) {
                String filename = (String) result.get("filename");
                if (result.containsKey("caption")) {
                    List<String> captionList = (List<String>) result.get("caption");
                    String caption = captionList.get(0).replaceAll("\"", "\"\""); // Escape dấu nháy kép

                    // Lưu vào DB
                    captionRepository.save(new Caption(filename, "test.com", caption));

                    csvBuilder.append("\"").append(filename).append("\",\"").append(caption).append("\"\n");
                } else {
                    csvBuilder.append("\"").append(filename).append("\",\"Lỗi khi sinh caption\"\n");
                }
            }

            byte[] csvBytes = csvBuilder.toString().getBytes(StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=captions.csv");
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentLength(csvBytes.length);

            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Lỗi khi gọi AI server: " + e.getMessage()));
        }
    }


//
//    @PostMapping("/images/advise")
//    public ResponseEntity<String> getAdviceFromImage(@RequestParam("image") MultipartFile image) throws Exception {
//        HttpEntity<MultiValueMap<String, Object>> request = buildMultipartRequest(image);
//        ResponseEntity<Map> response = restTemplate.exchange(aiAdviceUrl, HttpMethod.POST, request, Map.class);
//        String advice = (String) response.getBody().get("advice");
//        return ResponseEntity.ok(advice);
//    }

    @PostMapping("/query/advise")
    public ResponseEntity<Map<String, Object>> getAdviceFromQuery(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        ResponseEntity<Map> response = restTemplate.postForEntity(aiQueryUrl, request, Map.class);
        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/image-url")
    public void proxyImage(@RequestParam("url") String url, HttpServletResponse response) throws IOException {
        InputStream imageStream = new URL(url).openStream();
        response.setContentType("image/jpeg"); // hoặc tự detect từ URL
        StreamUtils.copy(imageStream, response.getOutputStream());
    }

}
