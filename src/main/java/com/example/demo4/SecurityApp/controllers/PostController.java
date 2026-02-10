package com.example.demo4.SecurityApp.controllers;

import com.example.demo4.SecurityApp.dto.PostDTO;
import com.example.demo4.SecurityApp.services.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(path = "/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public List<PostDTO> getAllPosts() {
        return postService.getAllPosts();
    }

    @GetMapping("/{postId}")
    public PostDTO getPostById(@PathVariable Long postId) {
        return postService.getPostById(postId);
    }

    @PostMapping
    public PostDTO createNewPost(@RequestBody PostDTO inputPost) {
        return postService.createNewPost(inputPost);
    }

    @GetMapping("/download/excel")
    public ResponseEntity<byte[]> downloadPostsAsExcel() {
        byte[] excelFile = postService.generatePostsExcelFile();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "posts.xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelFile);
    }

    @PostMapping("/upload/excel")
    public ResponseEntity<String> uploadPostsFromExcel(@RequestParam("file") MultipartFile file) {
        try {
            int uploadedCount = postService.uploadPostsFromExcel(file);
            return ResponseEntity.ok("Successfully uploaded " + uploadedCount + " posts from Excel file");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body("Failed to upload Excel file: " + e.getMessage());
        }
    }
}
