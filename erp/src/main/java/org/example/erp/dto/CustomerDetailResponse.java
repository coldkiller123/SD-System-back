package org.example.erp.dto;

import lombok.Data;
import java.util.List;

@Data
public class CustomerDetailResponse {
    private Info info;

    @Data
    public static class Info {
        private String id;
        private String name;
        private String type;
        private String region;
        private String industry;
        private String company;
        private String phone;
        private String address;
        private String creditRating;
        private String createdAt;
        private String modifiedAt;
        private String modifiedBy;
        private List<ContactDTO> contacts;
        private String remarks;
        private List<AttachmentDTO> attachments;
    }

    @Data
    public static class ContactDTO {
        private String id;
        private String name;
        private String position;
        private String phone;
        private String email;
    }

    @Data
    public static class AttachmentDTO {
        private String filename;
        private String filepath;
    }
}
