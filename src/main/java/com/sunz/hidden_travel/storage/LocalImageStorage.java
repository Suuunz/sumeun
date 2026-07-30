package com.sunz.hidden_travel.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 로컬 파일 저장 구현. 기본 위치는 ./uploads/reviews (개발용 H2 파일 DB 와 같은 결).
 * 웹 노출 경로 매핑은 {@link com.sunz.hidden_travel.config.WebConfig} 가 담당한다.
 */
@Service
public class LocalImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalImageStorage.class);

    /** 업로드 허용 확장자 — 실행 가능한 파일이 올라가지 않도록 화이트리스트로 제한 */
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /** 장당 최대 크기 (10MB) */
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    /** 후기 1건당 최대 장수 */
    private static final int MAX_FILES = 5;

    private final Path rootDir;
    private final Path baseDir;
    private final String urlPrefix;

    public LocalImageStorage(@Value("${app.upload.dir:./uploads}") String uploadDir,
                             @Value("${app.upload.url-prefix:/uploads}") String urlPrefix) {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseDir = rootDir.resolve("reviews");
        this.urlPrefix = urlPrefix;
    }

    /** 이미지 1장을 folder 아래에 저장하고 웹 경로를 반환. 실패 시 null */
    @Override
    public String saveOne(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_BYTES) {
            log.warn("[Upload] 용량 초과({}바이트): {}", file.getSize(), file.getOriginalFilename());
            return null;
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (ext == null) {
            log.warn("[Upload] 허용되지 않는 형식: {}", file.getOriginalFilename());
            return null;
        }
        // folder 는 호출부가 정하는 고정 문자열이지만, 경로 이탈은 원천 차단한다
        Path dir = rootDir.resolve(folder).normalize();
        if (!dir.startsWith(rootDir)) {
            log.warn("[Upload] 잘못된 저장 위치: {}", folder);
            return null;
        }
        try {
            Files.createDirectories(dir);
            String stored = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Files.copy(file.getInputStream(), dir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
            return urlPrefix + "/" + folder + "/" + stored;
        } catch (IOException e) {
            log.error("[Upload] 저장 실패: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    @Override
    public List<String> saveAll(List<MultipartFile> files) {
        List<String> paths = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return paths;
        }
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            log.error("[Upload] 저장 디렉터리 생성 실패: {}", baseDir, e);
            return paths;
        }

        for (MultipartFile file : files) {
            if (paths.size() >= MAX_FILES) {
                log.warn("[Upload] 최대 {}장 초과 — 나머지는 무시합니다.", MAX_FILES);
                break;
            }
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > MAX_BYTES) {
                log.warn("[Upload] 용량 초과({}바이트) — 건너뜁니다: {}", file.getSize(), file.getOriginalFilename());
                continue;
            }
            String ext = extensionOf(file.getOriginalFilename());
            if (ext == null) {
                log.warn("[Upload] 허용되지 않는 형식 — 건너뜁니다: {}", file.getOriginalFilename());
                continue;
            }
            // 원본 파일명은 쓰지 않는다(경로 조작·중복 방지)
            String stored = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            try {
                Files.copy(file.getInputStream(), baseDir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
                paths.add(urlPrefix + "/reviews/" + stored);
            } catch (IOException e) {
                log.error("[Upload] 저장 실패: {}", file.getOriginalFilename(), e);
            }
        }
        return paths;
    }

    /** 허용 확장자면 소문자로 반환, 아니면 null */
    private String extensionOf(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXT.contains(ext) ? ext : null;
    }
}
