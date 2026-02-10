package com.example.demo4.SecurityApp.services;

import com.example.demo4.SecurityApp.dto.PostDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostService {

    List<PostDTO> getAllPosts();

    PostDTO createNewPost(PostDTO inputPost);

    PostDTO getPostById(Long postId);

    byte[] generatePostsExcelFile();

    int uploadPostsFromExcel(MultipartFile file);
}
