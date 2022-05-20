package se.irori.sandbox.hellokafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {
  @Autowired
  KafkaProperties kafkaProperties;

  @Bean
  public KafkaConsumer<String, String> consumerFactory() {
    return new KafkaConsumer<>(kafkaProperties.buildConsumerProperties());
  }
}