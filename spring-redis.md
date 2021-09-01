### springboot 集成redis

 maven引入redis，以及工具类

```java
      <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- JSON工具类 -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <!-- https://mvnrepository.com/artifact/org.apache.commons/commons-pool2 -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
            <version>2.6.2</version>
        </dependency>

        <!-- 阿里JSON解析器 -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.66</version>
        </dependency>

```



由于 Spring Boot 2.X 默认集成了 Lettuce ，所以无需导入。



配置文件配置

```yaml
#spring 配置
spring:

  #redis配置
 redis:
   #数据库索引
   database: 0
   #redis 服务器地址
   host: localhost
   #redis 端口
   port: 6379
   #redis 密码 默认为空
   password: 123456
   # 链接超时时间
   connect-timeout: 10s

   #lettuce连接池配置
   lettuce:
     pool:
       # 链接池中最小的空闲链接 默认为0
       min-idle: 0
       # 链接池中最大的空闲连接 默认为 8
       max-idle: 8
       #连接池中最大数据库链接数 默认为8
       max-active: 8
       #连接池最大阻塞等待时间 负值表示没有限制
       max-wait: -1ms
```

### 定制redis序列化

redis正常存储数据使用了默认的jdk 存储二进制方式，不利用我们观看，我们可以使用官方提供的json序列化方式 也可以使用自己定制的.



```java
/**
 * redis 使用fastjson 序列化
 *
 *
 */
public class FastJsonRedis<T> implements RedisSerializer<T> {
    private ObjectMapper objectMapper = new ObjectMapper();

     public static final Charset  DEFAULT_CHARSET = Charset.forName("UTF-8");

     private Class<T> clazz;

     public FastJsonRedis(Class<T> clazz){
        this.clazz = clazz;
    }

    static{
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if(t==null){
            return new byte[0];
        }
        return JSON.toJSONString(t, SerializerFeature.WriteClassName).getBytes(DEFAULT_CHARSET);
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if(bytes == null|| bytes.length<=0){
            return null;
        }
        String str= new String(bytes,DEFAULT_CHARSET);
        return  JSON.parseObject(str,clazz);
    }

    public void setObjectMapper(ObjectMapper objectMapper){
         this.objectMapper=objectMapper;
    }
}

```

配置redistemplate 的序列化方式

```java
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
          //设置ObjectMapper
          ObjectMapper mapper = new ObjectMapper();
          mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
          mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
          fastJsonRedis.setObjectMapper(mapper);
         // 设置value序列化
          template.setValueSerializer(fastJsonRedis);
          template.setHashValueSerializer(fastJsonRedis);

          template.afterPropertiesSet();
          return  template;
   }


}

```

#### 创建redis工具类 ，为我们更好的使用



里面操作的参数都是 **final类型**，是因为缓存时，不能在修改缓存数据了。

```java
@Component
public class RedisCache {

    @Autowired
   public  RedisTemplate redisTemplate;
    /**
     * 缓存的基本对象，Integer，String，实体类
     *
     */
    public  <T> void setCacheObject(String key,final T value){
        redisTemplate.opsForValue().set(key,value);
    }
    /**
     * 设置有效时间
     * 时间单位：秒
     */
     public boolean expire(final String key,final long timeout){
         return  expire(key,timeout, TimeUnit.SECONDS);
     }

    /**
     * 设置有效时间
     * @param key
     * @param timeout
     * @param unit
     * @return
     */
     public boolean expire(final String key,final long timeout,final TimeUnit unit){
          return redisTemplate.expire(key,timeout,unit);
     }

    /**
     * 获得缓存的基本对象
     */
    public <T> T getCacheObject(final String key){
        ValueOperations<String,T> operations=redisTemplate.opsForValue();
        return  operations.get(key);
    }
    /**
     * 删除单个对象
     *
     */
    public boolean deleteObject(final String key){
        return  redisTemplate.delete(key);
    }
    /**
     * 删除集合对象
      */
     public long deleteObject(final Collection collection){
         return  redisTemplate.delete(collection);
     }

    /**
     * 缓存list对象
     * @param key
     * @param dataList
     * @param <T>
     * @return
     */
     public <T> long setCacheList(final String key,final List<T> dataList){
         //Long 类型
           Long count = redisTemplate.opsForList().rightPushAll(key,dataList);
           return  count==null?0:count;
     }

    /**
     * 获得缓存的list对象
     * @param key
     * @param <T>
     * @return
     */
     public <T> List<T> getCacheList(final String key){
       return   redisTemplate.opsForList().range(key,0,-1);
     }

    /**
     * 缓存 map
     * @param key
     * @param dataMap
     * @param <T>
     */
     public <T> void setCacheMap(final String key ,final Map<String,T> dataMap){
         if(dataMap != null){
             redisTemplate.opsForHash().putAll(key,dataMap);
         }
     }

    /**
     *   entries
     *   用于以Map的格式获取一个Hash键的所有值。
     * 获取缓存的map
     * @param key
     * @param <T>
     * @return
     */
     public <T> Map<String,T> getCacheMap(final String key){
         return  redisTemplate.opsForHash().entries(key);
     }
    /**
     * 缓存Set
     *
     * @param key 缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet)
    {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext())
        {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key)
    {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern)
    {
        return redisTemplate.keys(pattern);
    }
}

```

