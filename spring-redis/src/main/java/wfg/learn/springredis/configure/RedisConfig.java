package wfg.learn.springredis.configure;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * redis 配置
 */
@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {


    //修改缓存存储的序列化方式
   @Bean
    public RedisTemplate<Object,Object> redisTemplate(RedisConnectionFactory connectionFactory){

           RedisTemplate<Object,Object>  template= new RedisTemplate<>();
           template.setConnectionFactory(connectionFactory);
           // key的序列化采用StringRedisSerializer
           template.setKeySerializer(new StringRedisSerializer());

           template.setHashKeySerializer(new StringRedisSerializer());



          // value的序列化采用fastjson
          FastJsonRedis<Object> fastJsonRedis = new FastJsonRedis<>(Object.class);

         // 设置value序列化
          template.setValueSerializer(fastJsonRedis);
          template.setHashValueSerializer(fastJsonRedis);

          template.afterPropertiesSet();
          return  template;
   }


}
