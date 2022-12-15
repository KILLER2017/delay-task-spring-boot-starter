# Delay-task-spring-boot-starter

## 介绍

基于redis的有序集合zset实现的一个通用的延迟任务管理器

### 基本原理

把任务DelayTask作为value，把任务唤醒时间作为score存入zset中，任务分发器TaskDispatcher会轮询这个zset，从中取出唤醒时间小于当前时间的任务DelayTask，然后根据任务类型taskType分发给对应的任务处理器DelayTaskSubscriber去处理

## 使用场景

+ 订单超时自动关闭
+ 活动开始时把数据加入缓存
+ 指定时间启用配置

## 快速开始