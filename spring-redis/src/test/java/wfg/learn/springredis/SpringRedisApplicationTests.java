package wfg.learn.springredis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import wfg.learn.springredis.redis.RedisCache;
import wfg.learn.springredis.Bean.User;

import javax.annotation.Resource;

@SpringBootTest
class SpringRedisApplicationTests {
    @Resource
    RedisCache redisCache;
    @Test
    void contextLoads() {

        redisCache.setCacheObject("user1",new User(19));
    }

}
