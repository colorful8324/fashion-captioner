package com.fashionai.captioning.fashion_captioner.controller;

import com.fashionai.captioning.fashion_captioner.model.Advice;
import com.fashionai.captioning.fashion_captioner.model.Image;
import com.fashionai.captioning.fashion_captioner.model.Search;
import com.fashionai.captioning.fashion_captioner.repository.AdviceRepository;
import com.fashionai.captioning.fashion_captioner.repository.ImageRepository;
import com.fashionai.captioning.fashion_captioner.repository.SearchRepository;
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

    private final ImageRepository imageRepository;
    private final AdviceRepository adviceRepository;
    private final SearchRepository searchRepository;
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

    @PostMapping("/caption")
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
            Map<String, MultipartFile> uuidToFileMap = new LinkedHashMap<>();
            for (MultipartFile image : images) {
                String uuidFilename = UUID.randomUUID() + "-" + image.getOriginalFilename();
                uuidToFileMap.put(uuidFilename, image);
            }

            List<Map<String, Object>> captionResults = generateCaptionsFromServer(uuidToFileMap);
            log.info("AI server sinh captions thành công");

            Map<String, String> uploadedFiles = uploadImagesToMinio(uuidToFileMap);
            log.info("Upload MinIO thành công");

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
            Map<String, MultipartFile> uuidToFileMap = new LinkedHashMap<>();
            for (MultipartFile image : images) {
                String uniqueName = UUID.randomUUID() + "-" + image.getOriginalFilename();
                uuidToFileMap.put(uniqueName, image);
            }
            List<Map<String, Object>> captionResults = generateCaptionsFromServer(uuidToFileMap);
            List<String> captions = extractCaptions(captionResults);

            Map<String, String> uploadedFiles = uploadImagesToMinio(uuidToFileMap);
            
            for (Map<String, Object> result : captionResults) {
                String originalFilename = (String) result.get("filename");
                String storedFilename = uploadedFiles.get(originalFilename);
                String fileUrl = minioService.getObjectUrl(storedFilename);

                String caption = result.containsKey("caption") ?
                        ((List<String>) result.get("caption")).get(0) :
                        "Lỗi khi sinh caption";

                Image savedImage = imageRepository.save(
                        new Image(storedFilename, fileUrl, caption)
                );

                searchRepository.save(new Search(savedImage.getRecordId(), question));
            }

            Map<String, Object> advicePayload = Map.of(
                    "captions", captions,
                    "question", question
            );

            ResponseEntity<Map> aiResponse = requestAdviceFromCaptions(advicePayload);
            Advice serverAdvice = saveAdviceToDb(question, aiResponse);
            return ResponseEntity.ok(aiResponse.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi xử lý request: " + e.getMessage());
        }
    }

    private Advice saveAdviceToDb(String question, ResponseEntity<Map> aiResponse) {
        assert aiResponse.getBody() != null;
        return adviceRepository.save(
                new Advice(
                        question,
                        (String) aiResponse.getBody().get("answer")
                )
        );
    }

    @PostMapping("/query/advise")
    public ResponseEntity<?> getAdviceFromQuery(@RequestParam("question") String question) {
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng nhập câu hỏi.");
        }

        try {
            Map<String, Object> payload = Map.of("question", question);
            ResponseEntity<Map> llmResponse = requestAdviceFromQuery(payload);

            Map<String, Object> body = llmResponse.getBody();
            if (body == null || !body.containsKey("answer") || !body.containsKey("images")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Phản hồi từ AI không hợp lệ.");
            }

            String answer = (String) body.get("answer");
            Advice serverAdvice = adviceRepository.save(new Advice(question, answer));

            List<Map<String, Object>> images = (List<Map<String, Object>>) body.get("images");
            for (Map<String, Object> image : images) {
                Integer imageId = (Integer) image.get("record_id");
                searchRepository.save(new Search(imageId, question));
            }

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi xử lý request: " + e.getMessage());
        }
    }


    private Map<String, String> uploadImagesToMinio(Map<String, MultipartFile> uuidToFileMap) throws Exception {
        Map<String, String> uploadedFiles = new HashMap<>();
        for (Map.Entry<String, MultipartFile> entry : uuidToFileMap.entrySet()) {
            String uniqueName = entry.getKey();
            MultipartFile image = entry.getValue();
            minioService.uploadFile(uniqueName, image.getInputStream(), image.getContentType());
            uploadedFiles.put(uniqueName, uniqueName);
        }
        return uploadedFiles;
    }


    private List<Map<String, Object>> generateCaptionsFromServer(Map<String, MultipartFile> uuidToFileMap) throws IOException {
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        for (Map.Entry<String, MultipartFile> entry : uuidToFileMap.entrySet()) {
            String uniqueName = entry.getKey();
            MultipartFile file = entry.getValue();

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @NotNull
                @Override
                public String getFilename() {
                    return uniqueName;
                }
            };

            formData.add("images", new HttpEntity<>(fileResource, createMultipartHeaders(uniqueName)));
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

            imageRepository.save(new Image(storedFilename, fileUrl, caption));
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