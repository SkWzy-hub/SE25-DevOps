package com.SE2025BackEnd_16.project.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.config.ClusterServersConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.List;

@Configuration
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedissonConfig {

    @Value("${spring.data.redis.cluster.nodes:localhost:6379}")
    private String clusterNodes;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        try {
            Config config = new Config();
            // 判断是否为单节点
            if (!clusterNodes.contains(",") && (clusterNodes.contains("localhost") || clusterNodes.matches(".*:\\d+"))) {
                System.out.println("Redisson单节点模式: " + clusterNodes);
                SingleServerConfig singleConfig = config.useSingleServer();
                singleConfig.setAddress("redis://" + clusterNodes.trim());
                singleConfig.setDatabase(0);
                singleConfig.setConnectionPoolSize(32);
                singleConfig.setConnectionMinimumIdleSize(5);
                singleConfig.setIdleConnectionTimeout(10000);
                singleConfig.setConnectTimeout(10000);
                singleConfig.setRetryAttempts(3);
                singleConfig.setRetryInterval(1500);
                if (redisPassword != null && !redisPassword.isEmpty()) {
                    singleConfig.setPassword(redisPassword);
                }
            } else {
                // 集群模式
                System.out.println("Redisson集群模式: " + clusterNodes);
                String[] nodeAddresses = Arrays.stream(clusterNodes.split(","))
                        .map(node -> "redis://" + node.trim())
                        .toArray(String[]::new);
                ClusterServersConfig clusterConfig = config.useClusterServers();
                clusterConfig.setNodeAddresses(Arrays.asList(nodeAddresses));
                clusterConfig.setMasterConnectionPoolSize(32);
                clusterConfig.setSlaveConnectionPoolSize(32);
                clusterConfig.setMasterConnectionMinimumIdleSize(5);
                clusterConfig.setSlaveConnectionMinimumIdleSize(5);
                clusterConfig.setIdleConnectionTimeout(10000);
                clusterConfig.setConnectTimeout(10000);
                clusterConfig.setRetryAttempts(3);
                clusterConfig.setRetryInterval(1500);
                clusterConfig.setScanInterval(2000);
                if (redisPassword != null && !redisPassword.isEmpty()) {
                    clusterConfig.setPassword(redisPassword);
                }
            }
            RedissonClient client = Redisson.create(config);
            System.out.println("Redisson客户端创建成功");
            return client;
        } catch (Exception e) {
            System.err.println("Redisson客户端创建失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Redisson客户端创建失败", e);
        }
    }
} 