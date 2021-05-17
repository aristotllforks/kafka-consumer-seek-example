package sionsmith.demo.kafka.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sionsmith.demo.kafka.config.KafkaConsumerProperties;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

import javax.annotation.PostConstruct;
import java.util.Properties;

@Service
@Slf4j
public class ShipmentEventService {
    @Value("${lambda.region}")
    private String awsRegion;

    @Value("${lambda.function-name}")
    private String lambdaFunctionName;

    private LambdaClient client;

    @Autowired
    private KafkaConsumerProperties kafkaConsumerProperties;

    @Value("${kafka.consumer.topic-name}")
    private String sourceTopic;

    private Properties shipmentTopicProperties;
    @PostConstruct
    private void init() {
        client = LambdaClient.builder()
                .region(Region.of(awsRegion))
                .build();
        shipmentTopicProperties = new Properties();
        shipmentTopicProperties.put("bootstrap.servers", kafkaConsumerProperties.getBootstrapServers());
        shipmentTopicProperties.put("key.deserializer", kafkaConsumerProperties.getKeySerializer());
        shipmentTopicProperties.put("value.deserializer", kafkaConsumerProperties.getValueSerializer());
        shipmentTopicProperties.put("spring.json.trusted.packages", kafkaConsumerProperties.getSpringJsonTrustedPackages());
        shipmentTopicProperties.put("max.poll.records", kafkaConsumerProperties.getMaxPollRecords());

    }

    public void reProcessFailedEvent(Long offset, Integer partition) throws Exception {
        try (KafkaPicker kafkaPicker = new KafkaPicker(sourceTopic, shipmentTopicProperties)) {
            JsonNode payload = (JsonNode) kafkaPicker.pick(offset, partition);
            log.info("Retrived payload from offset: " + " Payload: " + payload.toPrettyString());
            for (int retries = 0; ; retries++) {
                try {
                    //Invoke the Lambda function
                    InvokeResponse response = client.invoke(InvokeRequest.builder()
                            .functionName(lambdaFunctionName)
                            .invocationType("RequestResponse")
                            .payload(SdkBytes.fromUtf8String(payload.toString()))
                            .build());

                    log.debug("Lambda Response: " + response.statusCode());
                    break;
                } catch (LambdaException e) {
                    if (retries < 3) {
                        continue; // try calling the lambda again
                    } else {
                        log.error("Failed to process payload from offset: " + offset + " partition: " + partition + "caused by:", e);
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to read message from offset: " + offset + " partition: " + partition + " Caused by: ", e);
            throw e;
        }
    }
}
