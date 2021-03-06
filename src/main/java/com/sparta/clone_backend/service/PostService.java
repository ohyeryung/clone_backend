package com.sparta.clone_backend.service;


import com.amazonaws.services.s3.AmazonS3Client;
import com.sparta.clone_backend.dto.PostDetailResponseDto;
import com.sparta.clone_backend.dto.PostListDto;
import com.sparta.clone_backend.dto.PostRequestDto;
import com.sparta.clone_backend.dto.PostResponseDto;
import com.sparta.clone_backend.model.Image;
import com.sparta.clone_backend.model.Post;
import com.sparta.clone_backend.model.PostLike;
import com.sparta.clone_backend.model.User;
import com.sparta.clone_backend.repository.ImageRepository;
import com.sparta.clone_backend.repository.PostLikeRepository;
import com.sparta.clone_backend.repository.PostRepository;
import com.sparta.clone_backend.repository.UserRepository;
import com.sparta.clone_backend.security.UserDetailsImpl;
import com.sparta.clone_backend.validator.UserInfoValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final AmazonS3Client amazonS3Client;
    private final S3Uploader s3Uploader;
    private final UserInfoValidator validator;

//    @Autowired
//    public PostService(PostRepository postRepository) {
//        this.postRepository = postRepository;
//    };

    // ????????? ??????
    public PostResponseDto createPost(PostRequestDto postRequestDto, User user) {

        Post post = Post.builder()
                        .user(user)
                        .postTitle(postRequestDto.getPostTitle())
                        .postContents(postRequestDto.getPostContents())
                        .imageUrl(postRequestDto.getImageUrl())
                        .price(postRequestDto.getPrice())
                        .location(user.getLocation())
                        .nickName(user.getNickName())
                        .category(postRequestDto.getCategory())
                        .createdAt(LocalDateTime.now())
                        .modifiedAt(LocalDateTime.now())
                        .build();

        postRepository.save(post);

        return PostResponseDto.builder()

                .userName(user.getUserName())
                .build();
    }

    // ?????? ????????? ?????? - ????????? ?????? ??????
    public Page<PostListDto> showAllPost(int pageno, UserDetailsImpl userDetails) {
        String username = userDetails.getUser().getUserName();

        List<Post> post = postRepository.findAllByOrderByCreatedAtDesc();

        Pageable pageable = getPageable(pageno);
        List<PostListDto> postListDto = new ArrayList<>();
        forpostList(post,username, postListDto);

        int start = pageno * 10;
        int end = Math.min((start + 10), post.size());

        return validator.overPages(postListDto, start, end, pageable, pageno);
    }

    @Value("${cloud.aws.s3.bucket}")
    public String bucket;  // S3 ?????? ??????


    // ????????? ??????
    @Transactional
    public Object deletePost(Long postId, User user) {

        Post post = postRepository.findByIdAndUserId(postId,user.getId()).orElseThrow(
                () -> new IllegalArgumentException("???????????? ?????? ???????????????.")
        );

//         S3 ????????? ??????
        String temp = post.getImageUrl();
        Image image = imageRepository.findByImageUrl(temp);
        String fileName = image.getFilename();
        s3Uploader.deleteImage(fileName);

        postLikeRepository.deleteAllByPostId(postId);
        postRepository.deleteById(post.getId());

        return null;
    }

    //?????? ????????? ??????
    public PostDetailResponseDto getPostDetail(Long postId, UserDetailsImpl userDetails) {
        String username = userDetails.getUsername();
        Post post = postRepository.findById(postId).get();

        return new PostDetailResponseDto(
                post.getPostTitle(),
                post.getPostContents(),
                post.getImageUrl(),
                post.getPrice(),
                post.getUser().getLocation(),
                convertLocaldatetimeToTime(post.getCreatedAt()),
                postLikeRepository.countByPost(post),
                post.getNickName(),
                post.getCategory(),
                postLikeRepository.findByUserNameAndPost(username,post).isPresent()

        );
    }

    // ?????? ?????????,???????????? ??????
    public Page<PostListDto> getUserPage(UserDetailsImpl userDetails, int pageno) {
        String userName = userDetails.getUser().getUserName();

        List<PostLike> postLikes = postLikeRepository.findAllByUserName(userName);

        Pageable pageable = getPageable(pageno);

        List<PostListDto> userLikeList = new ArrayList<>();

        for (PostLike postLikeObject : postLikes) {
            Post likedPost = postLikeObject.getPost();

            PostListDto postsResponseDto = new PostListDto(
                    likedPost,
                    convertLocaldatetimeToTime(likedPost.getCreatedAt()),
                    convertLocaldatetimeToTime(likedPost.getModifiedAt()),
                    postLikeRepository.countByPost(likedPost)
            );
            userLikeList.add(postsResponseDto);

        }
        int start = pageno * 10;
        int end = Math.min((start + 10), userLikeList.size());

        return validator.overPages(userLikeList, start, end, pageable, pageno);
    }

    // ????????? ?????? (????????? ????????? ?????? ??????)
    @Transactional
    public PostResponseDto editPost(Long postId, PostRequestDto requestDto, User user) {

        Post post = postRepository.findByIdAndUserId(postId,user.getId()).orElseThrow(
                () -> new IllegalArgumentException("???????????? ?????? ???????????????.")
        );
        post.update(postId, requestDto);

        PostResponseDto responseDto = new PostResponseDto(postId, post.getPostContents());
        return responseDto;
    }

    // ?????? ??????
    public static String convertLocaldatetimeToTime(LocalDateTime localDateTime) {
        LocalDateTime now = LocalDateTime.now();

        long diffTime = localDateTime.until(now, ChronoUnit.SECONDS); // now?????? ????????? +, ????????? -

        int SEC = 60;
        int MIN = 60;
        int HOUR = 24;
        int DAY = 30;
        int MONTH = 12;

        String msg = null;
        if (diffTime < SEC){
            return diffTime + "??????";
        }
        diffTime = diffTime / SEC;
        if (diffTime < MIN) {
            return diffTime + "??? ???";
        }
        diffTime = diffTime / MIN;
        if (diffTime < HOUR) {
            return diffTime + "?????? ???";
        }
        diffTime = diffTime / HOUR;
        if (diffTime < DAY) {
            return diffTime + "??? ???";
        }
        diffTime = diffTime / DAY;
        if (diffTime < MONTH) {
            return diffTime + "?????? ???";
        }

        diffTime = diffTime / MONTH;
        return diffTime + "??? ???";
    }



    private void forpostList(List<Post> postList, String username, List<PostListDto> postListDto) {
        for (Post post : postList) {
            int likeCount = postLikeRepository.countAllByPostId(post.getId());

            PostListDto postDto = new PostListDto(post, convertLocaldatetimeToTime(post.getCreatedAt()), convertLocaldatetimeToTime(post.getModifiedAt()), likeCount,
                    postLikeRepository.findByUserNameAndPost(username,post).isPresent());

            postListDto.add(postDto);
        }
    }


    //????????? ????????? ?????? ??????
    @Transactional
    public Page<PostListDto> getSearchPost(String keyword, UserDetailsImpl userDetails, int pageno) throws UnsupportedEncodingException {
        List<Post> searchedPosts = new ArrayList<>();

        String decodeVal = URLDecoder.decode(keyword, "utf-8");
        String username = userDetails.getUsername();

        searchedPosts = postRepository.searchByKeyword(decodeVal);
        List<PostListDto> postListDtos = new ArrayList<>();
        for (Post searchedPost : searchedPosts) {

            PostListDto postListDto = new PostListDto(
                    searchedPost.getId(),
                    searchedPost.getPostTitle(),
                    searchedPost.getImageUrl(),
                    searchedPost.getPrice(),
                    searchedPost.getLocation(),
                    convertLocaldatetimeToTime(searchedPost.getCreatedAt()),
                    convertLocaldatetimeToTime(searchedPost.getModifiedAt()),
                    postLikeRepository.countByPost(searchedPost),
                    searchedPost.getCategory(),
                    postLikeRepository.findByUserNameAndPost(username, searchedPost).isPresent()
            );
            postListDtos.add(postListDto);

        }
        Pageable pageable = getPageable(pageno);

        forpostList(searchedPosts, username, postListDtos);

        int start = pageno * 10;
        int end = Math.min((start + 10), searchedPosts.size());

        return validator.overPages(postListDtos, start, end, pageable, pageno);
    }

    // ????????? ??????
    private Pageable getPageable(int page) {
        Sort.Direction direction = Sort.Direction.DESC;
        Sort sort = Sort.by(direction, "id");
        return PageRequest.of(page, 10, sort);
    }

    //??????????????? ????????? ?????? ??????
    @Transactional
    public Page<PostListDto> getCategoryPost(String category, UserDetailsImpl userDetails, int pageno) throws UnsupportedEncodingException {
        List<Post> searchedPosts = new ArrayList<>();

        String decodeVal = URLDecoder.decode(category, "utf-8");
        searchedPosts = postRepository.searchByCategory(decodeVal);
        String username = userDetails.getUsername();

        List<PostListDto> postListDtos = new ArrayList<>();
        for (Post searchedPost : searchedPosts) {

            PostListDto postListDto = new PostListDto(
                    searchedPost.getId(),
                    searchedPost.getPostTitle(),
                    searchedPost.getImageUrl(),
                    searchedPost.getPrice(),
                    searchedPost.getLocation(),
                    convertLocaldatetimeToTime(searchedPost.getCreatedAt()),
                    convertLocaldatetimeToTime(searchedPost.getModifiedAt()),
                    postLikeRepository.countByPost(searchedPost),
                    searchedPost.getCategory(),
                    postLikeRepository.findByUserNameAndPost(username, searchedPost).isPresent()
            );
            postListDtos.add(postListDto);

        }
        Pageable pageable = getPageable(pageno);

        forpostList(searchedPosts, username, postListDtos);

        int start = pageno * 10;
        int end = Math.min((start + 10), searchedPosts.size());

        return validator.overPages(postListDtos, start, end, pageable, pageno);
    }
}

