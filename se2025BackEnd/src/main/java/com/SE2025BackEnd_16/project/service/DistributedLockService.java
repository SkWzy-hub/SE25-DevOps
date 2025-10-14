package com.SE2025BackEnd_16.project.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnBean(RedissonClient.class)
public class DistributedLockService {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 获取分布式锁
     * @param lockKey 锁的key
     * @return RLock对象
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }

    /**
     * 尝试获取锁（带超时时间）
     * @param lockKey 锁的key
     * @param waitTime 等待时间
     * @param leaseTime 持有锁的时间
     * @param timeUnit 时间单位
     * @return 是否成功获取锁
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        RLock lock = getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, timeUnit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 尝试获取锁（默认配置）
     * @param lockKey 锁的key
     * @return 是否成功获取锁
     */
    public boolean tryLock(String lockKey) {
        // 默认等待3秒，持有锁30秒
        return tryLock(lockKey, 3, 30, TimeUnit.SECONDS);
    }

    /**
     * 释放锁
     * @param lockKey 锁的key
     */
    public void unlock(String lockKey) {
        RLock lock = getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    /**
     * 强制释放锁（不检查持有者）
     * @param lockKey 锁的key
     */
    public void forceUnlock(String lockKey) {
        RLock lock = getLock(lockKey);
        lock.forceUnlock();
    }

    /**
     * 检查锁是否被当前线程持有
     * @param lockKey 锁的key
     * @return 是否被当前线程持有
     */
    public boolean isHeldByCurrentThread(String lockKey) {
        RLock lock = getLock(lockKey);
        return lock.isHeldByCurrentThread();
    }

    /**
     * 检查锁是否存在
     * @param lockKey 锁的key
     * @return 锁是否存在
     */
    public boolean isLocked(String lockKey) {
        RLock lock = getLock(lockKey);
        return lock.isLocked();
    }
} 