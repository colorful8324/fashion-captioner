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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequiredArgsConstructor
public class ShopController {

    private final CaptionRepository captionRepository;
    private final RestTemplate restTemplate;
    private final MinioService minioService;

    @Value("${ai.caption.url}")
    private String captionServerUrl;

    @Value("${ai.advise-from-images.url}")
    private String adviseFromImagesUrl;

    @Value("${ai.advise-from-query.url}")
    private String adviseFromQueryUrl;

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

    @GetMapping("/recommendation")
    public String blog() {
        return "shop/recommendation";
    }

    @PostMapping("/recommendation/caption")
    public String caption() {
        return "shop/caption";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "shop/checkout";
    }

    @GetMapping("/cart")
    public String cart() {
        return "shop/cart";
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
            Map<String, String> uploadedFiles = uploadImagesToMinio(images);
            log.info("Upload MinIO thành công");

            List<Map<String, Object>> captionResults = generateCaptionsFromServer(images);
            log.info("AI server sinh captions thành công");

            byte[] csvBytes = buildCsvFromCaptions(captionResults, uploadedFiles);
            log.info("Tạo CSV thành công");

            return buildDownloadCsvResponse(csvBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(List.of("Lỗi khi xử lý ảnh: " + e.getMessage()));
        }
    }

    @PostMapping("/images/advise")
    public ResponseEntity<?> getAdviceFromImages(
            @RequestParam("images") List<MultipartFile> images,
            @RequestParam("question") String question) {

        if (images.isEmpty() || question.isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng upload câu hỏi và ít nhất một ảnh.");
        }

        try {
            Map<String, String> uploadedFiles = uploadImagesToMinio(images);
            List<Map<String, Object>> captionResults = generateCaptionsFromServer(images);
            List<String> captions = extractCaptions(captionResults);

            Map<String, Object> advicePayload = Map.of(
                    "captions", captions,
                    "question", question
            );

            ResponseEntity<Map> aiResponse = requestAdviceFromCaptions(advicePayload);
            return ResponseEntity.ok(aiResponse.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi xử lý request: " + e.getMessage());
        }
    }

    @PostMapping("/query/advise")
    public ResponseEntity<?> getAdviceFromQuery(@RequestParam("question") String question) {
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập câu hỏi.");
        }

        try {
            Map<String, Object> payload = Map.of("question", question);
            ResponseEntity<Map> llmResponse = requestAdviceFromQuery(payload);
            return ResponseEntity.ok(llmResponse.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi xử lý request: " + e.getMessage());
        }
    }

    private Map<String, String> uploadImagesToMinio(List<MultipartFile> images) throws Exception {
        Map<String, String> uploadedFiles = new HashMap<>();
        for (MultipartFile image : images) {
            String storedName = UUID.randomUUID() + "-" + image.getOriginalFilename();
            minioService.uploadFile(storedName, image.getInputStream(), image.getContentType());
            uploadedFiles.put(image.getOriginalFilename(), storedName);
        }
        return uploadedFiles;
    }

    private List<Map<String, Object>> generateCaptionsFromServer(List<MultipartFile> images) throws IOException {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        for (MultipartFile file : images) {
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @NotNull
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            formData.add("images", new HttpEntity<>(fileResource, createMultipartHeaders(file.getOriginalFilename())));
        }

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(formData);
        ResponseEntity<Map> response = restTemplate.exchange(captionServerUrl, HttpMethod.POST, request, Map.class);
        return (List<Map<String, Object>>) response.getBody().get("results");
    }

    private List<String> extractCaptions(List<Map<String, Object>> captionResults) {
        return captionResults.stream()
                .map(result -> {
                    if (result.containsKey("caption")) {
                        List<String> captions = (List<String>) result.get("caption");
                        return captions.isEmpty() ? "" : captions.get(0);
                    }
                    return "";
                })
                .collect(Collectors.toList());
    }

    private ResponseEntity<Map> requestAdviceFromCaptions(Map<String, Object> advicePayload) {
        return restTemplate.postForEntity(adviseFromImagesUrl, buildJsonRequest(advicePayload), Map.class);
    }

    private ResponseEntity<Map> requestAdviceFromQuery(Map<String, Object> queryPayload) {
        return restTemplate.postForEntity(adviseFromQueryUrl, buildJsonRequest(queryPayload), Map.class);
    }

    private HttpEntity<Map<String, Object>> buildJsonRequest(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpHeaders createMultipartHeaders(String filename){
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setContentDispositionFormData("files", filename);
            return headers;
        }

    private byte[] buildCsvFromCaptions(List<Map<String, Object>> results, Map<String, String> uploadedFiles) throws Exception {
        StringBuilder csv = new StringBuilder("Filename,URL,Caption\n");

        for (Map<String, Object> result : results) {
            String originalFilename = (String) result.get("filename");
            String storedFilename = uploadedFiles.get(originalFilename);
            String fileUrl = minioService.getObjectUrl(storedFilename);

            String caption = result.containsKey("caption") ?
                    ((List<String>) result.get("caption")).get(0).replaceAll("\"", "\"\"") :
                    "Lỗi khi sinh caption";

            captionRepository.save(new Caption(storedFilename, fileUrl, caption));
            csv.append(String.format("\"%s\",\"%s\",\"%s\"\n", originalFilename, fileUrl, caption));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private ResponseEntity<byte[]> buildDownloadCsvResponse(byte[] csvBytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=captions.csv");
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentLength(csvBytes.length);
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }
}