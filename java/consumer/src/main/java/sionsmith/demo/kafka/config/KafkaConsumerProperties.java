package sionsmith.demo.kafka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Kafka consumer config parameters
 */
@ConfigurationProperties(prefix = "kafka.consumer")
@Getter
@Setter
public class KafkaConsumerProperties {
    private String topicName;
    private String groupId;
    private Class<?> keySerializer;
    private Class<?> valueSerializer;
    private String bootstrapServers;
    private String maxPollRecords;
    private String springJsonTrustedPackages;
}
