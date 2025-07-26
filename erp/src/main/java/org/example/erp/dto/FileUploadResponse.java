package org.example.erp.dto;

import lombok.Data;
import java.util.List;

@Data
public class FileUploadResponse {
    private FileUploadData data;

    @Data
    public static class FileUploadData {
        private List<AttachmentDTO> attachments;
    }

    @Data
    public static class AttachmentDTO {
        private String filename;
        private String filepath;
    }
}

