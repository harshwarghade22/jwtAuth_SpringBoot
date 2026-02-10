package com.example.demo4.SecurityApp.services;

import com.example.demo4.SecurityApp.dto.PostDTO;
import com.example.demo4.SecurityApp.entities.PostEntity;
import com.example.demo4.SecurityApp.entities.User;
import com.example.demo4.SecurityApp.exceptions.ResourceNotFoundException;
import com.example.demo4.SecurityApp.repositories.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service @RequiredArgsConstructor
public class PostServiceImpl implements PostService{

    private final PostRepository postRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<PostDTO> getAllPosts() {
        return postRepository
                .findAll()
                .stream()
                .map(postEntity -> modelMapper.map(postEntity, PostDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public PostDTO createNewPost(PostDTO inputPost) {
        PostEntity postEntity = modelMapper.map(inputPost, PostEntity.class);
        return modelMapper.map(postRepository.save(postEntity), PostDTO.class);
    }

    @Override
    public PostDTO getPostById(Long postId) {
        PostEntity postEntity = postRepository
                .findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id "+postId));
        return modelMapper.map(postEntity, PostDTO.class);
    }

    @Override
    public byte[] generatePostsExcelFile() {
        List<PostDTO> posts = getAllPosts();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Posts");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Title");
            headerRow.createCell(2).setCellValue("Description");
            
            // Add data rows
            int rowNum = 1;
            for (PostDTO post : posts) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(post.getId());
                row.createCell(1).setCellValue(post.getTitle());
                row.createCell(2).setCellValue(post.getDescription());
            }
            
            // Auto-size columns
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
            
            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Error generating Excel file", e);
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    @Override
    public int uploadPostsFromExcel(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
            throw new IllegalArgumentException("File must be an Excel file (.xlsx or .xls)");
        }

        List<PostDTO> postsToSave = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Start from row 1 to skip header
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                
                if (row == null) continue;
                
                String title = getCellValueAsString(row.getCell(0));
                String description = getCellValueAsString(row.getCell(1));
                
                if (title == null || title.trim().isEmpty()) {
                    log.warn("Skipping row {} due to missing title", i);
                    continue;
                }
                
                PostDTO postDTO = new PostDTO();
                postDTO.setTitle(title);
                postDTO.setDescription(description != null ? description : "");
                
                postsToSave.add(postDTO);
            }
            
            if (postsToSave.isEmpty()) {
                throw new IllegalArgumentException("No valid posts found in Excel file");
            }
            
            // Save all posts
            for (PostDTO postDTO : postsToSave) {
                createNewPost(postDTO);
            }
            
            log.info("Successfully uploaded {} posts from Excel file", postsToSave.size());
            return postsToSave.size();
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            log.error("Error reading Excel file", e);
            throw new RuntimeException("Failed to read Excel file: " + e.getMessage(), e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long)cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return null;
            default:
                return null;
        }
    }
}
