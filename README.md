# Delay-task-spring-boot-starter

## 介绍

基于redis的有序集合zset实现的一个通用的延迟任务管理器

### 基本原理

把任务`DelayTask`作为`value`，把任务唤醒时间作为`score`存入`zset`中，任务分发器`TaskDispatcher`会轮询这个`zset`，从中取出唤醒时间小于当前时间的任务`DelayTask`，然后根据任务类型`taskType`分发给对应的任务处理器`DelayTaskSubscriber`去处理

项目依赖`redisson`提供分布式锁支持

## 使用场景

+ 订单超时自动关闭
+ 活动开始时把数据加入缓存
+ 指定时间启用配置

## 快速开始

添加依赖，在pom.xml文件上加入下面内容

```xml
<dependency>
    <groupId>io.github.KILLER2017</groupId>
    <artifactId>delay-task-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

在application.yml文件上添加redis配置

```yaml
spring:
  redis:
    cluster:
      nodes: 10.177.75.20:6379,10.177.18.11:6379
    database: 0
```

定义延迟任务类

```java
package ltd.loveacg.demospringboot;

import delaytask.DelayTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;


/**
 * @author ALVIN
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HelloWorldDelayTask implements DelayTask, Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    @Override
    public String getTaskType() {
        return "helloWorld";
    }
}

```

定义延迟任务处理器

```java
package ltd.loveacg.demospringboot;

import delaytask.DelayTask;
import delaytask.DelayTaskHandleResult;
import delaytask.DelayTaskSubscriber;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @author ALVIN
 */
@Slf4j
@Component
public class HelloWorldDelayTaskSubscriber implements DelayTaskSubscriber {
    @Override
    public boolean isSupport(DelayTask task) {
        return Objects.equals(task.getTaskType(), "helloWorld");
    }

    @Override
    public DelayTaskHandleResult handleTask(@NonNull DelayTask task) {
        HelloWorldDelayTask instance = (HelloWorldDelayTask) task;
        log.info("hello, {}", instance.getName());
        return DelayTaskHandleResult.SUCCESS;
    }
}
```

注入延迟任务管理器，进行任务发布，实现项目启动5秒后打印“hello, world”
```java
package ltd.loveacg.demospringboot;

import delaytask.DelayTaskChannel;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author ALVIN
 */
@SpringBootApplication
public class DemoSpringBootApplication implements CommandLineRunner {

    @Resource
    private DelayTaskChannel delayTaskChannel;

    public static void main(String[] args) {
        SpringApplication.run(DemoSpringBootApplication.class, args);
    }

    @Override
    public void run(String... args) {
        delayTaskChannel.publish(new HelloWorldDelayTask("world"), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5));
    }
}
```

5秒后输出“hello, world”