package com.sunz.hidden_travel.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 후기 사진 저장소. 지금은 로컬 파일({@link LocalImageStorage})이지만
 * S3 등으로 교체할 자리.
 */
public interface ImageStorage {

    /**
     * 이미지를 저장하고 웹에서 접근 가능한 경로를 반환한다(예: /uploads/reviews/xxx.jpg).
     * 빈 파일·이미지 아닌 파일은 건너뛴다.
     */
    List<String> saveAll(List<MultipartFile> files);
}
