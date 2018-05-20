package cn.xiayf.code.dwc.handler;

import java.util.ArrayList;
import java.util.List;

import cn.xiayf.code.dwc.adapter.bean.Task;
import com.gargoylesoftware.htmlunit.Page;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HandlerGroup {

    private static List<BaseHandler> handlers = new ArrayList<>();

    public synchronized static void registerHandler(BaseHandler handler) {
        handlers.add(handler);
    }

    public static void trigger(Task task, Page page) {
        for (BaseHandler handler : handlers) {
            try {
                handler.handle(task, page);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public static void callerThreadStart(Thread thread) {
        for (BaseHandler handler : handlers) {
            handler.addCallerThread(thread);
        }
    }

    public static void callerThreadFinish(Thread thread) {
        for (BaseHandler handler : handlers) {
            handler.removeCallerThread(thread);
        }
    }
}
