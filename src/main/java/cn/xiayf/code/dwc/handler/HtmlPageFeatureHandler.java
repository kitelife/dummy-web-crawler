package cn.xiayf.code.dwc.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentLinkedQueue;

import cn.xiayf.code.dwc.adapter.bean.Task;
import cn.xiayf.code.dwc.helper.CommonHelper;
import cn.xiayf.code.dwc.service.ConfigService;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HtmlPageFeatureHandler extends BaseHandler {

    private final ConcurrentLinkedQueue<String> outputQ = new ConcurrentLinkedQueue<>();

    private String myDataPath;

    public HtmlPageFeatureHandler(ConfigService cs) {
        super(cs);

        this.myDataPath = Paths.get(cs.getDataPath(), "html_page_feature_handler").toString();
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
            while (inRunningCallerThreads.size() == 0) {
                CommonHelper.sleep(1000);
            }
            File resultFile = Paths.get(this.myDataPath, "result.txt").toFile();
            try (FileOutputStream fos = new FileOutputStream(resultFile)) {
                String result;
                while (true) {
                    result = outputQ.poll();
                    if (result == null) {
                        if (inRunningCallerThreads.size() == 0) {
                            break;
                        }
                        CommonHelper.sleep(5000);
                        continue;
                    }
                    fos.write((result + "\n").getBytes(CommonHelper.Charset_UTF8));
                }
                fos.flush();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }).start();
    }

    public void handle(Task task, Page page) {
        if (!page.isHtmlPage() || page.getWebResponse().getStatusCode() != 200) {
            return;
        }
        HtmlPage hp = (HtmlPage) page;
        //
        int aNum = hp.getAnchors().size();
        int imgNum = hp.getElementsByTagName("img").size();
        //
        String title = extractTitle(hp);
        String keywords = extractKeywords(hp);
        String description = extractDescription(hp);

        String result = String.format("%s\t%s\t%s\t\"%s\"\t\"%s\"\t\"%s\"", task.getKey(),
                aNum, imgNum, title, keywords, description);
        outputQ.offer(result);
    }

    private String extractTitle(HtmlPage hp) {
        String title = "";
        try {
            DomNodeList<DomElement> dnl = hp.getElementsByTagName("title");
            if (dnl.size() > 0) {
                DomElement ele = dnl.get(0);
                title = ele.getTextContent();
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        }

        return title.trim().replace("\t", " ").replace("\n", " ");
    }

    private String extractKeywords(HtmlPage hp) {
        String keywords = "";
        try {
            DomElement ele = hp.getElementByName("keywords");
            keywords = ele.getAttribute("content");
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return keywords.trim().replace("\t", " ").replace("\n", " ");
    }

    private String extractDescription(HtmlPage hp) {
        String description = "";
        try {
            DomElement ele = hp.getElementByName("description");
            description = ele.getAttribute("content");
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
        return description.trim().replace("\t", " ").replace("\n", " ");
    }
}
