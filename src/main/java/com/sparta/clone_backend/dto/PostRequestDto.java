package com.sparta.clone_backend.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Data
public class PostRequestDto {
    private String postTitle;
    private String postContents;
    private String imageUrl;
    private int price;
    private String category;
    private String createdAt;

    // 게시글 등록 및 수정
    public PostRequestDto(String postTitle, String postContents, String imageUrl, int price, String category) {
        this.postTitle = postTitle;
        this.postContents = postContents;
        this.imageUrl = imageUrl;
        this.price = price;
        this.category = category;
    }

    // 게시글 수정
    public PostRequestDto(String postTitle, String postContents,  int price, String category) {
        this.postTitle = postTitle;
        this.postContents = postContents;
        this.price = price;
        this.category = category;
    }
}

