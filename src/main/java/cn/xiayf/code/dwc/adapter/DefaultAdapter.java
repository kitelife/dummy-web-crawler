package cn.xiayf.code.dwc.adapter;

import cn.xiayf.code.dwc.adapter.bean.Task;

public class DefaultAdapter implements InputAdapter {

    @Override
    public Task adapt(String line) {
        return new Task(line, line);
    }
}
