package cn.xiayf.code.dwc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.rocksdb.RocksDB;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;

import cn.xiayf.code.dwc.adapter.InputAdapter;
import cn.xiayf.code.dwc.adapter.bean.Task;
import cn.xiayf.code.dwc.handler.BaseHandler;
import cn.xiayf.code.dwc.handler.HandlerGroup;
import cn.xiayf.code.dwc.helper.CommonHelper;
import cn.xiayf.code.dwc.service.ConfigService;
import cn.xiayf.code.dwc.service.RocksDBService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {

    private final ConfigService cs;
    private final String recordFilePath;
    private final InputAdapter ia;
    private final RocksDBService rocksDBService;

    private final BlockingQueue<Task> taskQ = new ArrayBlockingQueue<>(1000);

    public App(ConfigService cs, String recordFilePath, InputAdapter ia) {
        this.cs = cs;
        this.recordFilePath = recordFilePath;
        this.ia = ia;
        this.rocksDBService = new RocksDBService(cs);
    }

    private void consumer() {
        Thread currentThread = Thread.currentThread();
        HandlerGroup.callerThreadStart(currentThread);

        WebClient client = prepareWebClient();
        Task task;
        //
        while (true) {
            try {
                task = taskQ.take();
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
                break;
            }
            if (task.getKey() == null && task.getUrl() == null) {
                break;
            }
            try {
                HandlerGroup.trigger(task, fetch(client, task.getUrl()));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        client.close();
        //
        HandlerGroup.callerThreadFinish(currentThread);
    }

    private void producer() {
        String rockdbName = "url-processed";
        RocksDB rocksDB = initRockDBCache(rockdbName);
        //
        String[] rfPaths = recordFilePath.split(",");
        for (String p : rfPaths) {
            try (BufferedReader br = new BufferedReader(new FileReader(p))) {
                String line;
                while ((line = br.readLine()) != null) {
                    Task t = ia.adapt(line);
                    //
                    if (t == null) {
                        log.warn("Failed to adapt: {}", line);
                        continue;
                    }
                    //
                    byte[] v = rocksDB.get(t.getKey().getBytes());
                    if (v != null) {
                        continue;
                    }
                    rocksDB.put(t.getKey().getBytes(), "".getBytes());
                    //
                    taskQ.put(t);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        //
        for (int count = 0; count < cs.getConcurrency(); count++) {
            try {
                taskQ.put(new Task(null, null));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        //
        rocksDBService.closeDB(rockdbName);
    }

    private void run() {
        //
        loadHandler();
        //
        Set<Thread> threads = new HashSet<>();
        // consumers
        for (int count = 0; count < cs.getConcurrency(); count++) {
            Thread t = new Thread(this::consumer);
            threads.add(t);
            t.start();
        }
        // producer
        producer();
        //
        for (Thread t : threads) {
            try {
                t.join();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        //
        CommonHelper.sleep(5000);
        //
        log.info("Crawler App finished!");
        System.exit(0);
    }

    private RocksDB initRockDBCache(String rockdbName) {
        File rockDBDataDir = new File(cs.getRocksDBDirPath());
        if (rockDBDataDir.exists()) {
            rockDBDataDir.delete();
        }
        rocksDBService.init();
        return rocksDBService.openDB(rockdbName);
    }

    private Page fetch(WebClient client, String record) throws Exception {
        return client.getPage(record);
    }

    private WebClient prepareWebClient() {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        // 设置webClient的相关参数
        webClient.getCookieManager().setCookiesEnabled(true);// 开启cookie管理
        webClient.getOptions().setJavaScriptEnabled(true);// 开启js解析
        webClient.getOptions().setCssEnabled(false);
        // 当出现Http error时，程序不抛异常继续执行
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        // 防止js语法错误抛出异常
        webClient.getOptions().setThrowExceptionOnScriptError(false); // js运行错误时，是否抛出异常
        webClient.getOptions().setTimeout(10000);
        // 默认是false, 设置为true的话不让你的浏览行为被记录
        webClient.getOptions().setDoNotTrackEnabled(false);
        //
        webClient.getOptions().setPrintContentOnFailingStatusCode(false);
        // 设置Ajax异步处理控制器即启用Ajax支持
        // webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        return webClient;
    }

    private static CommandLine parseCommandLine(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("c", "conf", true, "config file");
        options.addOption("f", "url_file", true, "url file path");
        options.addOption("a", "adapter", true, "which input adapter");
        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    private void loadHandler() {
        String[] handlers = cs.getHandlers().split(",");
        for (String handler : handlers) {
            try {
                Class<?> ht = Class.forName(String.format("cn.xiayf.code.dwc.handler.%s", handler));
                Constructor<?> constructor = ht.getConstructor(ConfigService.class);
                BaseHandler h = (BaseHandler) constructor.newInstance(cs);
                HandlerGroup.registerHandler(h);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private static InputAdapter loadAdapter(String whichAdapter) {
        InputAdapter ia = null;
        try {
            Class<?> ht = Class.forName(String.format("cn.xiayf.code.dwc.adapter.%sAdapter", whichAdapter));
            ia = (InputAdapter) ht.newInstance();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return ia;
    }

    private static ConfigService parseAppConf(String confFilePath) {
        Properties pps = new Properties();
        try (FileInputStream fis = new FileInputStream(confFilePath)) {
            pps.load(fis);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        //
        ConfigService cs = new ConfigService();
        Field[] fields = cs.getClass().getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            field.setAccessible(true);
            try {
                String v = pps.getProperty(fieldName, null);
                if (v == null) {
                    continue;
                }
                Class<?> fieldType = field.getType();
                if (fieldType.equals(Integer.class)) {
                    field.set(cs, Integer.parseInt(v));
                } else if (fieldType.equals(Long.class)) {
                    field.set(cs, Long.parseLong(v));
                } else if (fieldType.equals(Boolean.class)) {
                    field.set(cs, Boolean.parseBoolean(v));
                } else {
                    field.set(cs, v);
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        //
        return cs;
    }

    public static void main(String[] args) throws ParseException {
        CommandLine cli = parseCommandLine(args);
        //
        String confFilePath = cli.getOptionValue("c");
        ConfigService cs = parseAppConf(confFilePath);
        //
        String recordFilePath = cli.getOptionValue("f");
        //
        InputAdapter ia = loadAdapter(cli.getOptionValue("a", "Default"));
        //
        App app = new App(cs, recordFilePath, ia);
        app.run();
    }
}
