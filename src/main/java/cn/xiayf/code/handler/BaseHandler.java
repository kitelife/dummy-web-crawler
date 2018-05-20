package cn.xiayf.code.handler;

import java.util.HashSet;
import java.util.Set;

import cn.xiayf.code.adapter.bean.Task;
import cn.xiayf.code.service.ConfigService;
import com.gargoylesoftware.htmlunit.Page;

public abstract class BaseHandler {

    protected Set<Thread> inRunningCallerThreads = new HashSet<>();

    protected ConfigService cs;

    protected BaseHandler(ConfigService cs) {
        this.cs = cs;
    }

    public synchronized void addCallerThread(Thread thread) {
        inRunningCallerThreads.add(thread);
    }

    public synchronized void removeCallerThread(Thread thread) {
        inRunningCallerThreads.remove(thread);
    }

    abstract void handle(Task task, Page page);
}
