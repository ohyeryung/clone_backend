package com.sparta.clone_backend.dto;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PostDetailResponseDto {

    private String postTitle;
    private String postContents;
    private String imageUrl;
    private int price;
    private String location;
    private LocalDateTime createdAt;
    private int likeCount;
    private String nickName;

    public PostDetailResponseDto(
            String postTitle,
            String postContents,
            String imageUrl,
            int price,
            String location,
            LocalDateTime createdAt,
            int likeCount,
            String nickName) {

        this.postTitle = postTitle;
        this.postContents = postContents;
        this.imageUrl = imageUrl;
        this.price = price;
        this.location = location;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
        this.nickName = nickName;
    }

}
