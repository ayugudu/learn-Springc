package wfg.learn.springredis.configure;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;

/**
 * redis 使用fastjson 序列化
 *
 *
 */
public class FastJsonRedis<T> implements RedisSerializer<T> {


     public static final Charset  DEFAULT_CHARSET = Charset.forName("UTF-8");

     private Class<T> clazz;

     public FastJsonRedis(Class<T> clazz){
        this.clazz = clazz;
    }

    static{
   // autotype类型默认时被fastjson禁用掉了的，这里进行开启
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


}
