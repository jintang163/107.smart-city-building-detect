package com.ucm.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    @ConfigurationProperties(prefix = "minio")
    public MinioProperties minioProperties() {
        return new MinioProperties();
    }

    @Bean
    public MinioClient minioClient(MinioProperties props) {
        return MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }

    public static class MinioProperties {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private Bucket bucket = new Bucket();

        public static class Bucket {
            private String imagery;
            private String evidence;

            public String getImagery() { return imagery; }
            public void setImagery(String imagery) { this.imagery = imagery; }
            public String getEvidence() { return evidence; }
            public void setEvidence(String evidence) { this.evidence = evidence; }
        }

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public Bucket getBucket() { return bucket; }
        public void setBucket(Bucket bucket) { this.bucket = bucket; }
    }
}
