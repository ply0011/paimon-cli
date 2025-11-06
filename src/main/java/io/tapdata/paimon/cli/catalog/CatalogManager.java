package io.tapdata.paimon.cli.catalog;

import io.tapdata.paimon.cli.config.StorageConfig;
import org.apache.hadoop.conf.Configuration;
import org.apache.paimon.catalog.Catalog;
import org.apache.paimon.catalog.CatalogContext;
import org.apache.paimon.catalog.CatalogFactory;
import org.apache.paimon.catalog.Identifier;
import org.apache.paimon.options.Options;
import org.apache.paimon.table.Table;

import java.util.List;

/**
 * Paimon Catalog 管理器
 */
public class CatalogManager implements AutoCloseable {
    
    private final Catalog catalog;
    private final StorageConfig config;
    
    public CatalogManager(StorageConfig config) throws Exception {
        this.config = config;
        this.catalog = createCatalog(config);
    }
    
    /**
     * 根据存储配置创建 Catalog
     */
    private Catalog createCatalog(StorageConfig config) throws Exception {
        Options options = new Options();
        options.set("warehouse", config.getWarehouse());

        // 创建 Hadoop Configuration 并设置 S3 配置
        Configuration hadoopConf = new Configuration();
        config.getOptions().forEach(hadoopConf::set);

        CatalogContext context = CatalogContext.create(options, hadoopConf);
        return CatalogFactory.createCatalog(context);
    }
    
    /**
     * 获取所有数据库列表
     */
    public List<String> listDatabases() throws Exception {
        return catalog.listDatabases();
    }
    
    /**
     * 获取指定数据库的所有表
     */
    public List<String> listTables(String database) throws Exception {
        return catalog.listTables(database);
    }
    
    /**
     * 获取表对象
     */
    public Table getTable(String database, String tableName) throws Exception {
        Identifier identifier = Identifier.create(database, tableName);
        return catalog.getTable(identifier);
    }
    
    /**
     * 检查数据库是否存在
     */
    public boolean databaseExists(String database) {
        try {
            catalog.getDatabase(database);
            return true;
        } catch (Catalog.DatabaseNotExistException e) {
            return false;
        }
    }

    /**
     * 检查表是否存在
     */
    public boolean tableExists(String database, String tableName) {
        try {
            Identifier identifier = Identifier.create(database, tableName);
            catalog.getTable(identifier);
            return true;
        } catch (Catalog.TableNotExistException e) {
            return false;
        }
    }
    
    public Catalog getCatalog() {
        return catalog;
    }
    
    public StorageConfig getConfig() {
        return config;
    }
    
    @Override
    public void close() throws Exception {
        if (catalog != null) {
            catalog.close();
        }
    }
}

