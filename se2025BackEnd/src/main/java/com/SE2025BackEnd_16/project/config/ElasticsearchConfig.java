package com.SE2025BackEnd_16.project.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch配置类
 * 
 * 只有在配置了 elasticsearch.enabled=true 时才启用
 * 如果不启用，系统将使用数据库搜索作为回退方案
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
@EnableElasticsearchRepositories(basePackages = "com.SE2025BackEnd_16.project.repository")
public class ElasticsearchConfig {

    @Value("${elasticsearch.host:localhost}")
    private String host;

    @Value("${elasticsearch.port:9200}")
    private int port;

    @Value("${elasticsearch.username:}")
    private String username;

    @Value("${elasticsearch.password:}")
    private String password;

    @Bean
    public RestClient elasticsearchClient() {
        try {
            log.info("🔧 正在配置Elasticsearch客户端，连接到: {}:{}", host, port);
            
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            
            // 如果有用户名和密码，则设置认证
            if (!username.isEmpty() && !password.isEmpty()) {
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(username, password));
                log.info("🔐 已配置Elasticsearch认证信息");
            }

            RestClientBuilder builder = RestClient.builder(
                    new HttpHost(host, port, "http"))
                    .setHttpClientConfigCallback(httpClientBuilder -> 
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));

            log.info("✅ Elasticsearch客户端配置成功");
            return builder.build();
            
        } catch (Exception e) {
            log.error("❌ Elasticsearch客户端配置失败: {}", e.getMessage());
            throw new RuntimeException("Elasticsearch配置失败", e);
        }
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport() {
        try {
            log.info("🔧 正在配置Elasticsearch传输层");
            ElasticsearchTransport transport = new RestClientTransport(elasticsearchClient(), new JacksonJsonpMapper());
            log.info("✅ Elasticsearch传输层配置成功");
            return transport;
        } catch (Exception e) {
            log.error("❌ Elasticsearch传输层配置失败: {}", e.getMessage());
            throw new RuntimeException("Elasticsearch传输层配置失败", e);
        }
    }

    @Bean
    public ElasticsearchClient elasticsearchClientNew() {
        try {
            log.info("🔧 正在创建ElasticsearchClient");
            ElasticsearchClient client = new ElasticsearchClient(elasticsearchTransport());
            log.info("✅ ElasticsearchClient创建成功");
            return client;
        } catch (Exception e) {
            log.error("❌ ElasticsearchClient创建失败: {}", e.getMessage());
            throw new RuntimeException("ElasticsearchClient创建失败", e);
        }
    }
} 