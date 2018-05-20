package cn.xiayf.code.service;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RocksDBService {

    private final ConfigService cs;

    private Map<String, RocksDB> dbM = new HashMap<>();

    public RocksDBService(ConfigService cs) {
        this.cs = cs;
    }

    @PostConstruct
    public void init() {
        //
        RocksDB.loadLibrary();
        //
        File dbDir = new File(cs.getRocksDBDirPath());
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
    }

    public synchronized RocksDB openDB(String dbName) {
        RocksDB db = dbM.getOrDefault(dbName, null);
        if (db != null) {
            return db;
        }
        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            String dbPath = Paths.get(cs.getRocksDBDirPath(), dbName).toString();
            db = RocksDB.open(options, dbPath);
            dbM.put(dbName, db);
            return db;
        } catch (RocksDBException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public synchronized void closeDB(String dbName) {
        RocksDB db = dbM.getOrDefault(dbName, null);
        if (db == null) {
            return;
        }
        db.close();
        dbM.remove(dbName);
    }
}
