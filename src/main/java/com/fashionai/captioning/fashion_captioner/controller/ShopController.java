package com.fashionai.captioning.fashion_captioner.controller;

import com.fashionai.captioning.fashion_captioner.model.Caption;
import com.fashionai.captioning.fashion_captioner.repository.CaptionRepository;
import com.fashionai.captioning.fashion_captioner.utils.MultipartInputStreamFileResource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
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

    private HttpEntity<MultiValueMap<String, Object>> buildMultipartRequest(MultipartFile file) throws Exception {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new MultipartInputStreamFileResource(file.getInputStream(), file.getOriginalFilename()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return new HttpEntity<>(body, headers);
    }
    @PostMapping("/images/gen-cap")
    public ResponseEntity<List<String>> generateCaptions(@RequestParam("images") List<MultipartFile> images) {
        if (images.size() > 5) {
            return ResponseEntity.badRequest().body(List.of("Chỉ được phép upload tối đa 5 ảnh."));
        }

        List<String> captions = new ArrayList<>();

        for (MultipartFile image : images) {
            try {
                // Gọi API Python server
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                HttpEntity<MultiValueMap<String, Object>> requestEntity = buildMultipartRequest(image);
                ResponseEntity<Map> response = restTemplate.exchange(
                        aiCaptionUrl, HttpMethod.POST, requestEntity, Map.class);

                String caption = (String) response.getBody().get("caption");

                // Lưu vào DB
                Caption captionRecord = new Caption(
                        image.getOriginalFilename(),
                        "test.com",  // TODO: lam 1 cai db rieng de luu anh
                        caption
                );
                captionRepository.save(captionRecord);

                captions.add(caption);

            } catch (Exception e) {
                captions.add("Error generating caption for image: " + image.getOriginalFilename());
            }
        }

        return ResponseEntity.ok(captions);
    }

    @PostMapping("/images/advise")
    public ResponseEntity<String> getAdviceFromImage(@RequestParam("image") MultipartFile image) throws Exception {
        HttpEntity<MultiValueMap<String, Object>> request = buildMultipartRequest(image);
        ResponseEntity<Map> response = restTemplate.exchange(aiAdviceUrl, HttpMethod.POST, request, Map.class);
        String advice = (String) response.getBody().get("advice");
    return ResponseEntity.ok(advice);
    }

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
