package se.irori.sandbox.hellokafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

@RestController
public class HelloKafka {

  private static final Logger LOG = LoggerFactory.getLogger(HelloKafka.class);

  @Autowired
  KafkaTemplate<String, String> producerTemplate;

  @Autowired
  KafkaConsumer<String, String> kafkaConsumer;

  @GetMapping("/consume/{topic}")
  public String consume(@PathVariable String topic) {
    LOG.info("consume {}", topic);
    kafkaConsumer.subscribe(Collections.singletonList(topic));
    kafkaConsumer.resume(kafkaConsumer.assignment());
    StringBuilder stringBuilder = new StringBuilder();

    for (int i = 0; i < 3; i++) {
      ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(5));

      for (ConsumerRecord<String, String> record : records) {
        stringBuilder.append(String.format("%s-%d:%d, key=%s\n",
            record.topic(), record.partition(), record.offset(), record.key()));
        stringBuilder.append(record.value());
        stringBuilder.append("\n --- \n");
      }
    }

    kafkaConsumer.pause(kafkaConsumer.assignment());

    return stringBuilder.toString();
  }

  @PostMapping("/produce/{topic}")
  public String produce(@PathVariable String topic, @RequestBody String value) {
    LOG.info("produce {}, value {}", topic, value);
    try {
      SendResult<String, String> result = producerTemplate.send(topic, value).get();
      return result.getRecordMetadata().toString();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

}