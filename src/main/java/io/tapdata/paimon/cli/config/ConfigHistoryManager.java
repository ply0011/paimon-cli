package io.tapdata.paimon.cli.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置历史管理器，负责保存和加载最近使用的配置
 */
public class ConfigHistoryManager {
    
    private static final int MAX_HISTORY_SIZE = 3;
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.paimon-cli";
    private static final String CONFIG_FILE = "config-history.txt";
    
    private final Path configFilePath;
    
    public ConfigHistoryManager() {
        this.configFilePath = Paths.get(CONFIG_DIR, CONFIG_FILE);
        ensureConfigDirExists();
    }
    
    /**
     * 确保配置目录存在
     */
    private void ensureConfigDirExists() {
        try {
            Path configDir = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
        } catch (IOException e) {
            System.err.println("Failed to create config directory: " + e.getMessage());
        }
    }
    
    /**
     * 获取配置文件的完整路径
     */
    public String getConfigFilePath() {
        return configFilePath.toAbsolutePath().toString();
    }
    
    /**
     * 保存配置到历史记录
     */
    public void saveConfig(StorageConfig config) {
        try {
            List<StorageConfig> history = loadHistory();
            
            // 移除相同的配置（如果存在）
            history.removeIf(c -> isSameConfig(c, config));
            
            // 添加新配置到列表开头
            history.add(0, config);
            
            // 保留最新的3个配置
            if (history.size() > MAX_HISTORY_SIZE) {
                history = history.subList(0, MAX_HISTORY_SIZE);
            }
            
            // 写入文件
            writeHistory(history);
            
        } catch (Exception e) {
            System.err.println("Failed to save config history: " + e.getMessage());
        }
    }
    
    /**
     * 加载历史配置
     */
    public List<StorageConfig> loadHistory() {
        List<StorageConfig> history = new ArrayList<>();
        
        if (!Files.exists(configFilePath)) {
            return history;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(configFilePath.toFile()))) {
            String line;
            StorageConfig.StorageType currentType = null;
            String warehouse = null;
            List<String> options = new ArrayList<>();
            
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                if (line.equals("---")) {
                    // 配置分隔符，保存当前配置
                    if (currentType != null && warehouse != null) {
                        StorageConfig config = parseConfig(currentType, warehouse, options);
                        if (config != null) {
                            history.add(config);
                        }
                    }
                    // 重置状态
                    currentType = null;
                    warehouse = null;
                    options = new ArrayList<>();
                } else if (line.startsWith("type=")) {
                    String typeStr = line.substring(5);
                    currentType = StorageConfig.StorageType.valueOf(typeStr);
                } else if (line.startsWith("warehouse=")) {
                    warehouse = line.substring(10);
                } else if (line.startsWith("option.")) {
                    options.add(line.substring(7));
                }
            }
            
            // 处理最后一个配置
            if (currentType != null && warehouse != null) {
                StorageConfig config = parseConfig(currentType, warehouse, options);
                if (config != null) {
                    history.add(config);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to load config history: " + e.getMessage());
        }
        
        return history;
    }
    
    /**
     * 解析配置
     */
    private StorageConfig parseConfig(StorageConfig.StorageType type, String warehouse, List<String> options) {
        try {
            if (type == StorageConfig.StorageType.LOCAL) {
                return StorageConfig.createLocal(warehouse);
            } else if (type == StorageConfig.StorageType.S3) {
                String accessKey = "";
                String secretKey = "";
                String endpoint = "";
                String region = "";
                
                for (String option : options) {
                    String[] parts = option.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0];
                        String value = parts[1];
                        
                        if ("fs.s3a.access.key".equals(key)) {
                            accessKey = value;
                        } else if ("fs.s3a.secret.key".equals(key)) {
                            secretKey = value;
                        } else if ("fs.s3a.endpoint".equals(key)) {
                            endpoint = value;
                        } else if ("fs.s3a.endpoint.region".equals(key)) {
                            region = value;
                        }
                    }
                }
                
                return StorageConfig.createS3(warehouse, accessKey, secretKey, endpoint, region);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse config: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 写入历史记录到文件
     */
    private void writeHistory(List<StorageConfig> history) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(configFilePath.toFile()))) {
            writer.write("# Paimon CLI Configuration History\n");
            writer.write("# This file stores the last " + MAX_HISTORY_SIZE + " configurations\n");
            writer.write("# Auto-generated, do not edit manually\n");
            writer.write("\n");
            
            for (int i = 0; i < history.size(); i++) {
                StorageConfig config = history.get(i);
                
                writer.write("type=" + config.getType() + "\n");
                writer.write("warehouse=" + config.getWarehouse() + "\n");
                
                for (String key : config.getOptions().keySet()) {
                    String value = config.getOptions().get(key);
                    writer.write("option." + key + "=" + value + "\n");
                }
                
                if (i < history.size() - 1) {
                    writer.write("---\n");
                }
            }
        }
    }
    
    /**
     * 判断两个配置是否相同
     */
    private boolean isSameConfig(StorageConfig c1, StorageConfig c2) {
        if (c1.getType() != c2.getType()) {
            return false;
        }
        
        if (!c1.getWarehouse().equals(c2.getWarehouse())) {
            return false;
        }
        
        // 对于 S3 配置，比较关键选项
        if (c1.getType() == StorageConfig.StorageType.S3) {
            String ak1 = c1.getOptions().getOrDefault("fs.s3a.access.key", "");
            String ak2 = c2.getOptions().getOrDefault("fs.s3a.access.key", "");
            
            String ep1 = c1.getOptions().getOrDefault("fs.s3a.endpoint", "");
            String ep2 = c2.getOptions().getOrDefault("fs.s3a.endpoint", "");
            
            return ak1.equals(ak2) && ep1.equals(ep2);
        }
        
        return true;
    }
    
    /**
     * 格式化配置用于显示
     */
    public String formatConfigForDisplay(int index, StorageConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(index).append(". ");
        
        if (config.getType() == StorageConfig.StorageType.LOCAL) {
            sb.append("Local: ").append(config.getWarehouse());
        } else if (config.getType() == StorageConfig.StorageType.S3) {
            sb.append("S3: ").append(config.getWarehouse());
            String endpoint = config.getOptions().get("fs.s3a.endpoint");
            if (endpoint != null && !endpoint.isEmpty()) {
                sb.append(" (endpoint: ").append(endpoint).append(")");
            }
        }
        
        return sb.toString();
    }
}

