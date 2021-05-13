package sionsmith.demo.kafka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.avro.producer")
@Getter
@Setter
public class KafkaProducerProperties {
    private String schemaRegistryUrl;
    private Class<?> keySubjectNameStrategy;
    private Class<?> valueSubjectNameStrategy;

    private Boolean autoRegisterSchemas = false;
    private Boolean useLatestVersion = true;

    private String transactionIdPrefix;
}
