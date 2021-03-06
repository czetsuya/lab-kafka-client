package com.czetsuyatech.kafka.service.consumer.impl;

import com.czetsuyatech.kafka.admin.client.KafkaAdminClient;
import com.czetsuyatech.kafka.admin.config.KafkaConfigData;
import com.czetsuyatech.kafka.avro.model.RandomTempAvroModel;
import com.czetsuyatech.kafka.service.consumer.KafkaConsumer;
import com.czetsuyatech.kafka.service.transformer.AvroToDtoTransformer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class RandomTempKafkaConsumer implements KafkaConsumer<Long, RandomTempAvroModel> {

  private static final Logger LOG = LoggerFactory.getLogger(RandomTempKafkaConsumer.class);

  private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
  private final KafkaAdminClient kafkaAdminClient;
  private final KafkaConfigData kafkaConfigData;
  private final AvroToDtoTransformer transformer;

  public RandomTempKafkaConsumer(KafkaListenerEndpointRegistry listenerEndpointRegistry,
      KafkaAdminClient adminClient,
      KafkaConfigData configData,
      AvroToDtoTransformer transformer) {
    this.kafkaListenerEndpointRegistry = listenerEndpointRegistry;
    this.kafkaAdminClient = adminClient;
    this.kafkaConfigData = configData;
    this.transformer = transformer;
  }

  @EventListener
  public void onAppStarted(ApplicationStartedEvent event) {

    kafkaAdminClient.checkTopicsCreated();
    LOG.info("Topics with name {} is ready for operations!", kafkaConfigData.getTopicNamesToCreate().toArray());
    kafkaListenerEndpointRegistry.getListenerContainer("randomTempTopicListener").start();
  }

  @Override
  @KafkaListener(id = "randomTempTopicListener", topics = "${kafka-config.topic-name}", groupId = "${kafka-consumer"
      + "-config.consumer-group-id}")
  public void receive(@Payload List<RandomTempAvroModel> messages,
      @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) List<Long> keys,
      @Header(KafkaHeaders.RECEIVED_PARTITION_ID) List<Integer> partitions,
      @Header(KafkaHeaders.OFFSET) List<Long> offsets) {

    LOG.info("[v1] {} number of message received with keys {}, partitions {} and offsets {}, Thread id {}",
        messages.size(),
        keys.toString(),
        partitions.toString(),
        offsets.toString(),
        Thread.currentThread().getId());

    LOG.info("[v1] messages received={}", transformer.getTempModels(messages));
  }
}

