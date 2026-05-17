package com.flowguard.infrastructure.config;

  import com.fasterxml.jackson.databind.ObjectMapper;
  import com.flowguard.infrastructure.messaging.FlagChangeSubscriber;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.data.redis.connection.RedisConnectionFactory;
  import org.springframework.data.redis.core.RedisTemplate;
  import org.springframework.data.redis.core.StringRedisTemplate;
  import org.springframework.data.redis.listener.PatternTopic;
  import org.springframework.data.redis.listener.RedisMessageListenerContainer;
  import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
  import org.springframework.data.redis.serializer.StringRedisSerializer;

  @Configuration
  public class RedisConfig {

      @Bean
      public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
          RedisTemplate<String, Object> template = new RedisTemplate<>();
          template.setConnectionFactory(connectionFactory);
          template.setKeySerializer(new StringRedisSerializer());
          template.setHashKeySerializer(new StringRedisSerializer());
          template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
          template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
          template.afterPropertiesSet();
          return template;
      }

      @Bean
      public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
          return new StringRedisTemplate(connectionFactory);
      }

      @Bean
      public RedisMessageListenerContainer redisMessageListenerContainer(
              RedisConnectionFactory connectionFactory,
              FlagChangeSubscriber subscriber) {
          RedisMessageListenerContainer container = new RedisMessageListenerContainer();
          container.setConnectionFactory(connectionFactory);
          container.addMessageListener(subscriber, new PatternTopic("flag-changes:*"));
          return container;
      }
  }
  
