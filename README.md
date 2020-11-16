我这里设计的秒杀系统暂时还有几个问题尚未解决，我会在文末将这几个问题列出。



##  秒杀系统需要考虑的问题

###  限流

- Nginx负载均衡
- 限流
  - 网关限流（Nginx、HAProxy、Lvs、F5等）
- 应用层限流（阿里sentinel、redis、Guava RateLimiter等）
- 前端限制
  - 提交后按钮事件取消，减少用户的无用请求
  - 验证码校验（数学公式）

###  超卖问题

- SQL判断数据非负
- 加唯一索引防止用户重复购买


### 缓存

- redis预减库存减少数据库访问 
- 内存标记减少redis访问　
- 消息队列
  - 请求先入队缓冲，异步下单，增强用户体验
  - 请求出队，生成订单，减少库存
  - 客户端定时轮询检查是否秒杀成功 

### 分布式锁

- redisson
- redis + Lua
- redis的原子操作(decrement、increment)

### 服务降级、熔断

- 阿里sentinel
- dubbo + zk
- hystrix、ribbon
- ......

### 安全问题

这部分是为了防止一些用户使用程序去调接口凭空增加服务器的压力，前端的限制就对这部分人不生效了，需要在后端做控制。

- 接口防刷(通用 注解，拦截器方式)
- 接口隐藏



###  秒杀流程

![秒杀系统流程](/Users/bennett/Desktop/秒杀系统流程.png)



##  数据库表结构

![speed-kill-db](/Users/bennett/Desktop/speed-kill-db.jpg)



##  代码实现

###  controller

```java
package com.bennett.speedkill.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bennett.speedkill.annotation.RequestLimit;
import com.bennett.speedkill.entity.Goods;
import com.bennett.speedkill.service.IGoodsService;
import com.bennett.speedkill.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀API
 *
 * @author bennett
 * @date 2020/11/2
 */

@Slf4j
@RestController
@RequestMapping("/api/speedKill")
public class SpeedKillController {

    @Autowired
    private IGoodsService goodsService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 库存卖完标志
     */
    private static final Map<String, Boolean> IS_SALE_OVER = new ConcurrentHashMap<>(16);

    @RequestLimit
    @SentinelResource(value = "speed-kill", fallback = "秒杀失败，请重试!")
    @PostMapping
    public String killAction(@RequestBody Goods goods) {
        // todo 设置秒杀活动的结束标志（可放到redis中）

        if (IS_SALE_OVER.getOrDefault(Constant.GOODS_STOCK_PREFIX + goods.getId(), false)) {
            log.error("SaleOver is true, Thread is [{}]", Thread.currentThread().getName());
            return "秒杀失败";
        }

        // redis预减库存
        Long stock = redisTemplate.opsForValue().decrement(Constant.GOODS_STOCK_PREFIX + goods.getId());
        if (stock == null || stock.compareTo(0L) < 0) {
            redisTemplate.opsForValue().increment(Constant.GOODS_STOCK_PREFIX + goods.getId());
            IS_SALE_OVER.put(Constant.GOODS_STOCK_PREFIX + goods.getId(), true);
            // todo 发消息给集群上的其他机器，通知它们修改变量 isSaleOver=true
            log.error("秒杀失败, redis stock is [{}], Thread is [{}]", stock, Thread.currentThread().getName());
            return "秒杀失败";
        }

        // 数据库扣库存，生成订单
        // todo 存在分布式事务问题，消息失败后还需要redis库存回退，jvm标志重置
        goodsService.decStock(goods.getId());

        log.info("秒杀成功! Thread is [{}]", Thread.currentThread().getName());
        return "秒杀成功!";
    }

    @PostConstruct
    public void init() {
        // controller初始化时初始化redis中的库存
        Goods goods = goodsService.getOne(Wrappers.<Goods>lambdaQuery().eq(Goods::getId, 1326718786789318658L));
        redisTemplate.opsForValue().set(Constant.GOODS_STOCK_PREFIX + goods.getId(), String.valueOf(goods.getStock() - goods.getSale()), 1, TimeUnit.DAYS);
        log.info("init redis stock: [{}: {}]", Constant.GOODS_STOCK_PREFIX + goods.getId(), goods.getStock() - goods.getSale());
    }
}
```

###  Service

```java
package com.bennett.speedkill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bennett.speedkill.entity.Goods;
import com.bennett.speedkill.entity.OrderInfo;
import com.bennett.speedkill.mapper.GoodsMapper;
import com.bennett.speedkill.service.IGoodsService;
import com.bennett.speedkill.service.MessageQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 商品表 服务实现类
 * </p>
 *
 * @author bennett
 * @since 2020-11-04
 */
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements IGoodsService {

    @Autowired
    MessageQueueService messageQueueService;

    @Override
    public void decStock(Long id) {
        // 发消息-减库存
        messageQueueService.decrStock(id);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setGoodsId(id).setNumber(1).setUserId(1L).setCreateTime(LocalDateTime.now()).setStatus((byte)0);
        // 发消息-创建订单
        messageQueueService.createOrder(orderInfo);
    }
}
```

###  MQ-send

```java
package com.bennett.speedkill.service.impl;

import com.bennett.speedkill.entity.OrderInfo;
import com.bennett.speedkill.service.MessageQueueService;
import com.bennett.speedkill.util.Constant;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 消息队列发送服务的实现类-由rabbitMQ实现
 *
 * @author bennett
 * @date 2020/11/12
 */

@Service
public class MessageQueueServiceImpl implements MessageQueueService {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    public void decrStock(Long goodsId) {
        amqpTemplate.convertAndSend(Constant.GOODS_STOCK, goodsId);
    }

    @Override
    public void createOrder(OrderInfo orderInfo) {
        amqpTemplate.convertAndSend(Constant.CREATE_ORDER, orderInfo);
    }
}
```

###  MQ-receiver

```java
package com.bennett.speedkill.service.impl;

import com.bennett.speedkill.entity.OrderInfo;
import com.bennett.speedkill.mapper.GoodsMapper;
import com.bennett.speedkill.mapper.OrderInfoMapper;
import com.bennett.speedkill.service.MessageReceiveService;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.RabbitListeners;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MQ接收消息的实现
 *
 * @author bennett
 * @date 2020/11/12
 */

@Service
@RabbitListeners({@RabbitListener(queues = "GOODS_STOCK"), @RabbitListener(queues = "CREATE_ORDER")})
public class MessageReceiveServiceImpl implements MessageReceiveService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Override
    @RabbitHandler
    public void decrStock(Long goodsId) {
      	// sql: update goods set sale = sale + 1 where id = #{id} and sale < stock
        goodsMapper.updateSale(goodsId);
    }

    @Override
    @RabbitHandler
    public void createOrder(OrderInfo orderInfo) {
        orderInfoMapper.insert(orderInfo);
    }
}
```

###  配置类

####  MQ Config

```java
package com.bennett.speedkill.config;

import com.bennett.speedkill.util.Constant;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQ的队列配置
 *
 * @author bennett
 * @date 2020/11/12
 */

@Configuration
public class MessageQueueConfig {

    @Bean
    public Queue decrStockQueue() {
        return new Queue(Constant.GOODS_STOCK);
    }

    @Bean
    public Queue createOrderQueue() {
        return new Queue(Constant.CREATE_ORDER);
    }
}
```

####  MVC Config

```java
package com.bennett.speedkill.config;

import com.bennett.speedkill.interceptor.SpeedKillInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author bennett
 * @date 2020/11/11
 */

@Component
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private SpeedKillInterceptor speedKillInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(speedKillInterceptor)
        .addPathPatterns("/api/speedKill/**");
    }
}
```

####  RedisTemplate Config

```java
package com.bennett.speedkill.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.UnknownHostException;

/**
 * redisTemplate的配置文件，重新配置object类型的序列化方式
 *
 * @author bennett
 * @date 2020/11/9
 */

@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) throws UnknownHostException {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> jsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        // ObjectMapper将redis获取的LinkedList转换成Object
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL
                , JsonTypeInfo.As.WRAPPER_ARRAY);
        jsonRedisSerializer.setObjectMapper(objectMapper);

        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
```

###  接口防刷

####  自定义注解

```java
package com.bennett.speedkill.annotation;

import java.lang.annotation.*;

/**
 * 接口防刷的注解, 通过设置间隔时间、最大请求次数来防刷
 *
 * @author bennett
 * @date 2020/11/11
 */

@Documented
@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestLimit {

    /**
     * 请求间隔时间，单位：秒. 默认为1秒
     *
     * @return 请求间隔时间
     */
    int time() default 1;

    /**
     * 单位间隔时间请求最大次数, 默认为1次
     *
     * @return 最大请求次数
     */
    int maxCount() default 1;
}
```

####  拦截器

```java
package com.bennett.speedkill.interceptor;

import com.bennett.speedkill.annotation.RequestLimit;
import com.bennett.speedkill.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 拦截统一处理秒杀系统的接口安全问题
 *
 * @author bennett
 * @date 2020/11/11
 */

@Slf4j
@Component
public class SpeedKillInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //HandlerMethod 封装方法定义相关的信息,如类,方法,参数等
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        RequestLimit methodAnnotation = method.getAnnotation(RequestLimit.class);
        RequestLimit classAnnotation = method.getClass().getAnnotation(RequestLimit.class);

        // 如果注解没在方法上找到就使用类上的注解
        RequestLimit requestLimit = methodAnnotation == null ? classAnnotation : methodAnnotation;
        if (requestLimit == null) {
            return true;
        }

        if (isLimit(request, requestLimit)) {
            responseOut(response, "访问太频繁，累死宝宝了~");
            log.info("访问太频繁了...");
            return false;
        }
        return true;
    }

    /**
     * 把响应信息输出
     *
     * @param response {@link HttpServletResponse}
     * @param respStr  响应的字符串
     * @throws IOException io流异常
     */
    private void responseOut(HttpServletResponse response, String respStr) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        PrintWriter out = response.getWriter();
        out.append(respStr);
    }

    /**
     * 判断接口是否受限
     *
     * @param request      {@link HttpServletRequest}
     * @param requestLimit {@link RequestLimit}
     * @return {@link Boolean boolean value}
     */
    private boolean isLimit(HttpServletRequest request, RequestLimit requestLimit) {
        // sessionId可以换成token等唯一值
        String limitKey = request.getServletPath() + request.getSession().getId();
        Integer redisCount = (Integer) redisTemplate.opsForValue().get(Constant.LIMIT_PREFIX + limitKey);
        if (redisCount == null) {
            //初始 次数
            redisTemplate.opsForValue().set(Constant.LIMIT_PREFIX + limitKey, 1, requestLimit.time(), TimeUnit.SECONDS);
            return false;
        }

        // 次数超限
        if (redisCount >= requestLimit.maxCount()) {
            return true;
        }

        // 次数自增
        redisTemplate.opsForValue().increment(Constant.LIMIT_PREFIX + limitKey);
        return false;
    }
}
```

###  网关nginx限流

####  IP限流

```nginx
# 根据IP地址限制速度
# $binary_remote_addr 获得远程IP地址
# myiplimit 可以⾃自定义名，就是⼀一个内存区域
# rate=1r/s 每秒1次，rate=100r/m
limit_req_zone $binary_remote_addr zone=myiplimit:10m rate=1r/s;
server {
  server_name limit.icodingedu.com;
  location /access-limit/ {
    proxy_pass http://localhost:8088/;
    # 1.zone=myiplimit 引⽤用zone的变量量
    # 2.burst=2，请求缓冲区⼤大⼩小
    # 3.nodelay:缓存区满了了，直接503
    limit_req zone=myiplimit burst=2 nodelay;
  } 
}
```

####  服务器限流

```nginx
limit_req_zone $binary_remote_addr zone=myiplimit:10m rate=20r/s; # 根据服务器器进⾏行行限流
limit_req_zone $server_name zone=serverlimit:10m rate=1r/s;
server {
  server_name limit.icodingedu.com;
  location /access-limit/ {
    proxy_pass http://localhost:8088/;
    # 1.zone=myiplimit 引⽤用zone的变量量
    # 2.burst=2，请求缓冲区⼤大⼩小
    # 3.nodelay:缓存区满了了，直接503
    limit_req zone=myiplimit burst=2 nodelay; 
    limit_req zone=serverlimit burst=1 nodelay; 
    limit_req_status 504;
  }
  error_page 504 /504;
  location /504 {
    default_type application/json;
    add_header Content-Type 'text/html;charset=utf-8';
    return 200 '{"code":"666","msg":"访问⾼高峰期，请稍后重试..."}';
	} 
}
```

##  测试结果

经Jmeter测试单机10万并发时QPS在3000左右，因为redis可单机10万并发，所以并发多于10万就会有redis的异常出现。



##  尚未处理的部分

- 设置秒杀活动的结束标志（可放到redis中）
- 发消息给集群上的其他机器，通知它们修改变量 `IS_SALE_OVER`里的value为true
- 存在分布式事务问题，消息失败后还需要redis库存回退，jvm标志重置



##  参考文章

- [Github 秒杀系统](https://github.com/qiurunze123/miaosha)
- [API接口防刷](https://rstyro.github.io/blog/2019/04/15/api%E6%8E%A5%E5%8F%A3%E9%98%B2%E5%88%B7/)