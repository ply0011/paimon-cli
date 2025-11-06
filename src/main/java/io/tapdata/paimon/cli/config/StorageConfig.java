package io.tapdata.paimon.cli.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 存储配置类，支持 Local 和 S3 两种存储类型
 */
public class StorageConfig {
    
    public enum StorageType {
        LOCAL,
        S3
    }
    
    private final StorageType type;
    private final String warehouse;
    private final Map<String, String> options;
    
    private StorageConfig(StorageType type, String warehouse, Map<String, String> options) {
        this.type = type;
        this.warehouse = warehouse;
        this.options = options;
    }
    
    public StorageType getType() {
        return type;
    }
    
    public String getWarehouse() {
        return warehouse;
    }
    
    public Map<String, String> getOptions() {
        return options;
    }
    
    /**
     * 创建 Local 存储配置
     */
    public static StorageConfig createLocal(String path) {
        Map<String, String> options = new HashMap<>();
        return new StorageConfig(StorageType.LOCAL, path, options);
    }
    
    /**
     * 创建 S3 存储配置
     */
    public static StorageConfig createS3(String s3Path, String accessKey, String secretKey, String endpoint, String region) {
        Map<String, String> options = new HashMap<>();

        // 将 s3:// 转换为 s3a://（Hadoop S3A 文件系统）
        if (s3Path.startsWith("s3://")) {
            s3Path = s3Path.replace("s3://", "s3a://");
        }

        // S3 配置
        if (accessKey != null && !accessKey.isEmpty()) {
            options.put("fs.s3a.access.key", accessKey);
        }
        if (secretKey != null && !secretKey.isEmpty()) {
            options.put("fs.s3a.secret.key", secretKey);
        }
        if (endpoint != null && !endpoint.isEmpty()) {
            options.put("fs.s3a.endpoint", endpoint);
        }
        if (region != null && !region.isEmpty()) {
            options.put("fs.s3a.endpoint.region", region);
        }

        // 其他 S3 配置
        options.put("fs.s3a.path.style.access", "true");
        options.put("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");

        return new StorageConfig(StorageType.S3, s3Path, options);
    }
    
    @Override
    public String toString() {
        return "StorageConfig{" +
                "type=" + type +
                ", warehouse='" + warehouse + '\'' +
                ", options=" + options.size() + " entries" +
                '}';
    }
}

