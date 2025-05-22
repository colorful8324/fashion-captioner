package com.fashionai.captioning.fashion_captioner.controller;

import com.fashionai.captioning.fashion_captioner.model.mysql.Advice;
import com.fashionai.captioning.fashion_captioner.model.mysql.Image;
import com.fashionai.captioning.fashion_captioner.model.mysql.LlmResponse;
import com.fashionai.captioning.fashion_captioner.model.mysql.Search;
import com.fashionai.captioning.fashion_captioner.repository.mysql.AdviceRepository;
import com.fashionai.captioning.fashion_captioner.repository.mysql.ImageRepository;
import com.fashionai.captioning.fashion_captioner.repository.mysql.SearchRepository;
import com.fashionai.captioning.fashion_captioner.repository.mysql.LlmResponseRepository;
import com.fashionai.captioning.fashion_captioner.service.MinioService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
    private final LlmResponseRepository llmResponseRepository;
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

    @PostMapping("/recommendation/advise")
    public String advise() {
        return "shop/advise";
    }

    @GetMapping("/recommendation/advise")
    public String advises() {
        return "shop/advise";
    }


    @PostMapping("/recommendation/query")
    public String query() {
        return "shop/query";
    }

    @PostMapping("/images/gen-cap")
    public String generateCaptionsView(@RequestParam("images") List<MultipartFile> images, Model model) {
        log.info("=== BẮT ĐẦU XỬ LÝ ===");

        if (images.size() > 100) {
            model.addAttribute("error", "Chỉ được phép upload tối đa 100 ảnh.");
            return "error";
        }

        log.info("Số lượng ảnh nhận được: {}", images.size());
        images.forEach(img -> log.info("Ảnh: {}", img.getOriginalFilename()));

        try {
            Map<String, MultipartFile> uuidToFileMap = new LinkedHashMap<>();
            for (MultipartFile image : images) {
                String uuidFilename = UUID.randomUUID() + "-" + image.getOriginalFilename();
                uuidToFileMap.put(uuidFilename, image);
            }

            log.info("Tên file đã gắn UUID:");
            uuidToFileMap.keySet().forEach(key -> log.info(key));

            List<Map<String, Object>> captionResults = generateCaptionsFromServer(uuidToFileMap);
            log.info("Caption kết quả trả về:");
            captionResults.forEach(result -> log.info("{}", result));

            Map<String, String> uploadedFiles = uploadImagesToMinio(uuidToFileMap);
            log.info("Mapping filename → stored filename (MinIO):");
            uploadedFiles.forEach((k, v) -> log.info("{} → {}", k, v));

            List<Map<String, String>> displayResults = new ArrayList<>();
            for (Map<String, Object> result : captionResults) {
                String originalFilename = (String) result.get("filename");

                String storedFilename = uploadedFiles.get(originalFilename);
                if (storedFilename == null) {
                    log.info("⚠️ Không tìm thấy '{}' trong uploadedFiles", originalFilename);
                    continue; // bỏ qua nếu không khớp
                }

                String fileUrl = minioService.getObjectUrl(storedFilename);
                log.info("URL của ảnh: {}", fileUrl);

                String caption = result.containsKey("caption") ?
                        (String) result.get("caption") :
                        "Lỗi khi sinh caption";

                imageRepository.save(new Image(storedFilename, fileUrl, caption));
                log.info("Đã lưu ảnh vào DB: {}", storedFilename);

                displayResults.add(Map.of(
                        "filename", originalFilename,
                        "url", fileUrl,
                        "caption", caption
                ));
            }

            model.addAttribute("results", displayResults);
            model.addAttribute("uploadedFiles", uploadedFiles);
            log.info("=== XỬ LÝ HOÀN TẤT ===");
            return "shop/caption";
        } catch (Exception e) {
            log.error("❌ LỖI TRONG QUÁ TRÌNH XỬ LÝ", e);
            model.addAttribute("error", "Lỗi khi xử lý ảnh: " + e.getMessage());
            return "error";
        }
    }




    @PostMapping("/images/advise")
    public String getAdviceFromImagesView(@RequestParam("images") List<MultipartFile> images,
                                          @RequestParam("question") String question,
                                          Model model) {

        log.info("=== BẮT ĐẦU XỬ LÝ /images/advise ===");
        log.info("Question received: '{}'", question);
        log.info("Number of images received: {}", images.size());
        images.forEach(img -> log.info("Image: {}", img.getOriginalFilename()));

        if (images.isEmpty() || question.isEmpty()) {
            log.warn("Missing required parameters - images: {}, question: {}", images.isEmpty(), question.isEmpty());
            model.addAttribute("error", "Please upload a question and at least one image.");
            return "error";
        }

        try {
            Map<String, MultipartFile> uuidToFileMap = new LinkedHashMap<>();
            for (MultipartFile image : images) {
                String uniqueName = UUID.randomUUID() + "-" + image.getOriginalFilename();
                uuidToFileMap.put(uniqueName, image);
                log.info("Generated unique name for image: {} -> {}", image.getOriginalFilename(), uniqueName);
            }

            log.info("Calling caption server...");
            List<Map<String, Object>> captionResults = generateCaptionsFromServer(uuidToFileMap);
            List<String> captions = extractCaptions(captionResults);
            log.info("Received {} captions from server", captions.size());
            captions.forEach(caption -> log.info("Caption: {}", caption));

            Map<String, String> uploadedFiles = uploadImagesToMinio(uuidToFileMap);
            log.info("Uploaded {} files to MinIO", uploadedFiles.size());

            for (Map<String, Object> result : captionResults) {
                String originalFilename = (String) result.get("filename");
                String storedFilename = uploadedFiles.get(originalFilename);
                String fileUrl = minioService.getObjectUrl(storedFilename);
                log.info("Processing image: {} -> {}", originalFilename, fileUrl);

                String caption = result.containsKey("caption") ?
                        (String) result.get("caption") :
                        "Error generating caption";

                Image savedImage = imageRepository.save(new Image(storedFilename, fileUrl, caption));
                searchRepository.save(new Search(savedImage.getRecordId(), question));
                log.info("Saved image and search record to database - record_id: {}", savedImage.getRecordId());
            }

            Map<String, Object> advicePayload = Map.of("captions", captions, "question", question);
            log.info("Calling advice server with payload: {}", advicePayload);
            ResponseEntity<Map> aiResponse = requestAdviceFromCaptions(advicePayload);
            Map<String, Object> responseBody = aiResponse.getBody();
            log.info("Received response from advice server: {}", responseBody);

            Advice savedAdvice = saveAdviceToDb(question, aiResponse);
            log.info("Saved advice to database - id: {}", savedAdvice.getId());
            
            String caption = captions.get(0);
            String firstImageUrl = minioService.getObjectUrl(uploadedFiles.get(captionResults.get(0).get("filename")));
            
            model.addAttribute("images", firstImageUrl);
            model.addAttribute("caption", caption);
            model.addAttribute("answer", responseBody.get("answer"));
            log.info("=== XỬ LÝ HOÀN TẤT /images/advise ===");
        } catch (Exception e) {
            log.error("❌ LỖI TRONG QUÁ TRÌNH XỬ LÝ /images/advise", e);
            model.addAttribute("error", "Error occurred when processing request: " + e.getMessage());
        }

        return "shop/advise";
    }


    private Advice saveAdviceToDb(String question, ResponseEntity<Map> aiResponse) {
        assert aiResponse.getBody() != null;
        String response = (String) aiResponse.getBody().get("answer");
        
        try {
            llmResponseRepository.save(new LlmResponse(question, response));
            
            return adviceRepository.save(
                    new Advice(
                            question,
                            truncateResponse(response)
                    )
            );
        } catch (Exception e) {
            log.error("Error saving response to database: {}", e.getMessage());
            return new Advice(question, truncateResponse(response));
        }
    }

    private String truncateResponse(String text) {
        final int MAX_LENGTH = 1000;
        if (text == null || text.length() <= MAX_LENGTH) {
            return text;
        }
        log.info("Truncating response from {} to {} characters", text.length(), MAX_LENGTH);
        return text.substring(0, MAX_LENGTH - 3) + "...";
    }

    @PostMapping("/query/advise")
    public String getAdviceFromQuery(@RequestParam("question") String question, Model model) {
        try {
            log.info("Processing question: '{}'", question);
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("question", question);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            log.info("Calling advice server...");
            ResponseEntity<Map> response = restTemplate.exchange(
                    adviseFromQueryUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null) {
                model.addAttribute("error", "LLM server didn't response.");
                return "shop/query";
            }
            log.info("Advice server responded: {}", responseBody);

            model.addAttribute("question", question);
            model.addAttribute("answer", responseBody.get("answer"));
            model.addAttribute("image_urls", responseBody.get("image_urls"));
            log.info("Finished giving advices for question: {}", question);
        } catch (Exception e) {
            log.error("Error calling API from query/advise", e);
            model.addAttribute("error", "Error occurred when processing request: " + e.getMessage());
        }

        return "shop/query";
    }

    @PostMapping("/images/gen-cap/download")
    public ResponseEntity<byte[]> downloadCsv(@RequestParam("captions") String captionsJson,
                                              @RequestParam("files") String filesJson) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, String>> results = objectMapper.readValue(captionsJson, 
            new TypeReference<List<Map<String, String>>>() {});

        StringBuilder csv = new StringBuilder();
        csv.append('\ufeff');
        csv.append("Filename,URL,Caption\n");
        
        for (Map<String, String> result : results) {
            String filename = result.get("filename");
            String url = result.get("url");
            String caption = result.get("caption").replaceAll("\"", "\"\"");
            csv.append(String.format("\"%s\",\"%s\",\"%s\"\n", filename, url, caption));
        }

        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        return buildDownloadCsvResponse(csvBytes);
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
                        return (String) result.get("caption");
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
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentLength(csvBytes.length);
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }
}