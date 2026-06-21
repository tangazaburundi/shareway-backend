package com.shareway.application.port.out;
import org.springframework.web.multipart.MultipartFile;
public interface StoragePort {
    String upload(MultipartFile file, String folder);
    void delete(String url);
    String getPublicUrl(String key);
}
