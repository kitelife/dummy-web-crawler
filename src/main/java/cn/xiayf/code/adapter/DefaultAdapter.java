package cn.xiayf.code.adapter;

import cn.xiayf.code.adapter.bean.Task;

public class DefaultAdapter implements InputAdapter {

    @Override
    public Task adapt(String line) {
        return new Task(line, line);
    }
}
