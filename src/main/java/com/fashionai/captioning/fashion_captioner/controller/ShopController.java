package com.fashionai.captioning.fashion_captioner.controller;

import com.fashionai.captioning.fashion_captioner.model.Caption;
import com.fashionai.captioning.fashion_captioner.repository.CaptionRepository;
import com.fashionai.captioning.fashion_captioner.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ShopController {

    private final CaptionRepository captionRepository;
    private final RestTemplate restTemplate;
    private final MinioService minioService;
    @Value("${ai.caption.url}")
    private String dlServerUrl;
    @Value("") // TODO: them vao sau
    private String aiAdviceUrl;
    @Value("") // TODO: them vao sau
    private String aiQueryUrl;

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

    @PostMapping("/images/gen-cap")
    public ResponseEntity<?> generateCaptions(@RequestParam("images") List<MultipartFile> images) {
        if (images.size() > 100) {
            return ResponseEntity.badRequest().body(List.of("Chỉ được phép upload tối đa 100 ảnh."));
        }

        try {
            Map<String, String> uploadedFileMap = handleUploads(images);
            log.info("store minio thanh cong");
            List<Map<String, Object>> aiResults = callAiServer(images);
            log.info("ai server xu li thanh cong");
            byte[] csvBytes = saveCaptionsAndBuildCSV(aiResults, uploadedFileMap);
            log.info("build thanh cong csv");
            return buildDownloadResponse(csvBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Lỗi khi xử lý ảnh: " + e.getMessage()));
        }
    }

    private Map<String, String> handleUploads(List<MultipartFile> images) throws Exception {
        Map<String, String> uploadedMap = new HashMap<>();
        for (MultipartFile image : images) {
            String storedName = UUID.randomUUID() + "-" + image.getOriginalFilename();
            minioService.uploadFile(storedName, image.getInputStream(), image.getContentType());
            uploadedMap.put(image.getOriginalFilename(), storedName);
        }
        return uploadedMap;
    }

    private List<Map<String, Object>> callAiServer(List<MultipartFile> images) throws IOException {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        for (MultipartFile file : images) {
            ByteArrayResource fileRes = new ByteArrayResource(file.getBytes()) {
                @NotNull
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            formData.add("images", new HttpEntity<>(fileRes, createFileHeaders(file.getOriginalFilename())));
        }

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(formData);
        ResponseEntity<Map> response = restTemplate.exchange(dlServerUrl, HttpMethod.POST, request, Map.class);
        return (List<Map<String, Object>>) response.getBody().get("results");
    }

    private byte[] saveCaptionsAndBuildCSV(List<Map<String, Object>> results, Map<String, String> fileMap) throws Exception {
        StringBuilder csv = new StringBuilder("Filename,URL,Caption\n");

        for (Map<String, Object> result : results) {
            String original = (String) result.get("filename");
            String stored = fileMap.get(original);
            String url = minioService.getObjectUrl(stored);

            if (result.containsKey("caption")) {
                String caption = ((List<String>) result.get("caption")).get(0).replaceAll("\"", "\"\"");
                captionRepository.save(new Caption(stored, url, caption));
                csv.append("\"").append(original).append("\",\"").append(url).append("\",\"").append(caption).append("\"\n");
            } else {
                csv.append("\"").append(original).append("\",\"").append(url).append("\",\"Lỗi khi sinh caption\"\n");
            }
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    private ResponseEntity<byte[]> buildDownloadResponse(byte[] csvBytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=captions.csv");
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentLength(csvBytes.length);
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    private HttpHeaders createFileHeaders(String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"files\"; filename=\"" + filename + "\"");
        return headers;
    }




//
//    @PostMapping("/images/advise")
//    public ResponseEntity<String> getAdviceFromImage(@RequestParam("image") MultipartFile image) throws Exception {
//        HttpEntity<MultiValueMap<String, Object>> request = buildMultipartRequest(image);
//        ResponseEntity<Map> response = restTemplate.exchange(aiAdviceUrl, HttpMethod.POST, request, Map.class);
//        String advice = (String) response.getBody().get("advice");
//        return ResponseEntity.ok(advice);
//    }

//    @PostMapping("/query/advise")
//    public ResponseEntity<Map<String, Object>> getAdviceFromQuery(@RequestBody Map<String, String> request) {
//        String question = request.get("question");
//        ResponseEntity<Map> response = restTemplate.postForEntity(aiQueryUrl, request, Map.class);
//        return ResponseEntity.ok(response.getBody());
//    }


}
