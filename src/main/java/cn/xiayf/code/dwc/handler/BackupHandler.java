package cn.xiayf.code.dwc.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.io.IOUtils;

import cn.xiayf.code.dwc.adapter.bean.Task;
import cn.xiayf.code.dwc.helper.CommonHelper;
import cn.xiayf.code.dwc.helper.ContentTypeHelper;
import cn.xiayf.code.dwc.service.ConfigService;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BackupHandler extends BaseHandler {

    private final ConcurrentLinkedQueue<String> indexQ = new ConcurrentLinkedQueue<>();

    private String myDataPath;

    public BackupHandler(ConfigService cs) {
        super(cs);

        this.myDataPath = Paths.get(cs.getDataPath(), "backup_handler").toString();
        //
        File f = new File(myDataPath);
        if (!f.exists()) {
            f.mkdirs();
        }
        //
        init();
    }

    private void init() {
        new Thread(() -> {
            //
            File idxFile = Paths.get(myDataPath, "INDEX").toFile();
            try (FileOutputStream fos = new FileOutputStream(idxFile, true)) {
                //
                while (inRunningCallerThreads.size() == 0) {
                    CommonHelper.sleep(1000);
                }
                String idx;
                while (true) {
                    idx = indexQ.poll();
                    if (idx == null) {
                        if (inRunningCallerThreads.size() == 0) {
                            break;
                        }
                        CommonHelper.sleep(1000);
                        continue;
                    }
                    fos.write((idx + "\n").getBytes(CommonHelper.Charset_UTF8));
                    fos.flush();
                }
                fos.close();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }).start();
    }

    public void handle(Task task, Page page) {
        if (page.getWebResponse().getStatusCode() != 200) {
            return;
        }

        String url = page.getUrl().toString();
        String fp = CommonHelper.md5Hash(url);
        WebResponse resp = page.getWebResponse();
        //
        String fileSuffix = ContentTypeHelper.suffix(resp.getContentType());
        String dirName = fp.substring(0, 10);
        String fileName = String.format("%s%s", fp.substring(10), fileSuffix);
        String filePath = Paths.get(myDataPath, dirName, fileName).toString();
        //
        File f = new File(filePath);
        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(f)) {
            IOUtils.copy(resp.getContentAsStream(), fos);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        resp.cleanUp();
        //
        indexQ.add(String.format("%s\t%s", url, Paths.get(dirName, fileName).toString()));
    }
}
