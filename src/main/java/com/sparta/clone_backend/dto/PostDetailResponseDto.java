package com.sparta.clone_backend.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostDetailResponseDto {

    private String postTitle;
    private String postContents;
    private String imageUrl;
    private int price;
    private String location;
    private String createdAt;
    private int likeCount;
    private String nickName;
    private String category;
    private boolean like;

    public PostDetailResponseDto(
            String postTitle,
            String postContents,
            String imageUrl,
            int price,
            String location,
            String createdAt,
            int likeCount,
            String nickName,
            String category, boolean like) {

        this.postTitle = postTitle;
        this.postContents = postContents;
        this.imageUrl = imageUrl;
        this.price = price;
        this.location = location;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
        this.nickName = nickName;
        this.category = category;
        this.like = like;
    }

}
