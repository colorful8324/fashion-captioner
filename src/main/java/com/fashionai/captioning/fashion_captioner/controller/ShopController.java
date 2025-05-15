package com.fashionai.captioning.fashion_captioner.controller;

import com.fashionai.captioning.fashion_captioner.model.mysql.Advice;
import com.fashionai.captioning.fashion_captioner.model.mysql.Image;
import com.fashionai.captioning.fashion_captioner.model.mysql.Search;
import com.fashionai.captioning.fashion_captioner.repository.mysql.AdviceRepository;
import com.fashionai.captioning.fashion_captioner.repository.mysql.ImageRepository;
import com.fashionai.captioning.fashion_captioner.repository.mysql.SearchRepository;
import com.fashionai.captioning.fashion_captioner.service.MinioService;
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
    public String generateCaptionsView(@RequestParam("images") List<MultipartFile> images, Model model) {
        if (images.size() > 100) {
            model.addAttribute("error", "Chỉ được phép upload tối đa 100 ảnh.");
            return "error";
        }

        try {
            Map<String, MultipartFile> uuidToFileMap = new LinkedHashMap<>();
            for (MultipartFile image : images) {
                String uuidFilename = UUID.randomUUID() + "-" + image.getOriginalFilename();
                uuidToFileMap.put(uuidFilename, image);
            }

            List<Map<String, Object>> captionResults = generateCaptionsFromServer(uuidToFileMap);
            Map<String, String> uploadedFiles = uploadImagesToMinio(uuidToFileMap);

            List<Map<String, String>> displayResults = new ArrayList<>();
            for (Map<String, Object> result : captionResults) {
                String originalFilename = (String) result.get("filename");
                String storedFilename = uploadedFiles.get(originalFilename);
                String fileUrl = minioService.getObjectUrl(storedFilename);

                String caption = result.containsKey("caption") ?
                        ((List<String>) result.get("caption")).get(0) :
                        "Lỗi khi sinh caption";

                imageRepository.save(new Image(storedFilename, fileUrl, caption));

                displayResults.add(Map.of(
                        "filename", originalFilename,
                        "url", fileUrl,
                        "caption", caption
                ));
            }

            model.addAttribute("results", displayResults);
            return "shop/caption-result";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi xử lý ảnh: " + e.getMessage());
            return "error";
        }
    }



    @PostMapping("/images/advise")
    public String getAdviceFromImagesView(@RequestParam("images") List<MultipartFile> images,
                                          @RequestParam("question") String question,
                                          Model model) {

        if (images.isEmpty() || question.isEmpty()) {
            model.addAttribute("error", "Vui lòng upload câu hỏi và ít nhất một ảnh.");
            return "error";
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

                Image savedImage = imageRepository.save(new Image(storedFilename, fileUrl, caption));
                searchRepository.save(new Search(savedImage.getRecordId(), question));
            }

            Map<String, Object> advicePayload = Map.of("captions", captions, "question", question);
            ResponseEntity<Map> aiResponse = requestAdviceFromCaptions(advicePayload);
            Map<String, Object> responseBody = aiResponse.getBody();

            Advice savedAdvice = saveAdviceToDb(question, aiResponse);

            model.addAttribute("question", question);
            model.addAttribute("answer", responseBody.get("answer"));
            return "shop/advise-result";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi xử lý request: " + e.getMessage());
            return "error";
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
    public String getAdviceFromQuery(@RequestParam("question") String question, Model model) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("question", question);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            // Gọi Flask server
            ResponseEntity<Map> response = restTemplate.exchange(
                    adviseFromQueryUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null) {
                model.addAttribute("error", "Không nhận được phản hồi từ server.");
                return "shop/advise-result";
            }

            model.addAttribute("question", question);
            model.addAttribute("answer", responseBody.get("answer"));
            model.addAttribute("images", responseBody.get("images")); // là list image objects

        } catch (Exception e) {
            log.error("Lỗi khi gọi API từ query/advise", e);
            model.addAttribute("error", "Đã xảy ra lỗi khi xử lý yêu cầu: " + e.getMessage());
        }

        return "shop/advise-result";
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