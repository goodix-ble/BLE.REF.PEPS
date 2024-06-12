package com.goodix.ble.libcomx.task;

import com.goodix.ble.libcomx.event.Event;
import com.goodix.ble.libcomx.event.IEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

public class TaskPipe implements ITaskContext, IEventListener {

    public static final int EVT_BUSY = 761;
    public static final int EVT_TASK_ADDED = 447;
    public static final int EVT_TASK_START = 138;
    public static final int EVT_TASK_PROGRESS = 938;
    public static final int EVT_TASK_REMOVED = 166;

    private final Event<Boolean> eventBusy = new Event<>(this, EVT_BUSY);
    private final Event<TaskItem> eventTaskAdded = new Event<>(this, EVT_TASK_ADDED);
    private final Event<TaskItem> eventTaskStart = new Event<>(this, EVT_TASK_START);
    private final Event<TaskItem> eventTaskProgress = new Event<>(this, EVT_TASK_PROGRESS);
    private final Event<TaskItem> eventTaskRemoved = new Event<>(this, EVT_TASK_REMOVED);

    private LinkedList<TaskItem> taskList = new LinkedList<>();
    private HashMap<String, Object> parameters = new HashMap<>();
    private Executor executor;
    private boolean isBusy;


    public TaskPipe() {
    }

    public Event<Boolean> evtBusy() {
        return eventBusy;
    }

    public Event<TaskItem> evtTaskAdded() {
        return eventTaskAdded;
    }

    public Event<TaskItem> evtTaskStart() {
        return eventTaskStart;
    }

    public Event<TaskItem> evtTaskProgress() {
        return eventTaskProgress;
    }

    public Event<TaskItem> evtTaskRemoved() {
        return eventTaskRemoved;
    }

    public boolean isBusy() {
        return isBusy;
    }

    public synchronized void abortTask() {
        if (!taskList.isEmpty()) {
            TaskItem item = taskList.peekFirst();
            if (item != null) {
                item.task.abort();
            }
        }
    }

    public synchronized void clearTask() {
        if (!taskList.isEmpty()) {
            // 先停止后面的
            while (taskList.size() > 0) {
                TaskItem item = taskList.removeLast();
                item.task.abort();
            }
            if (isBusy) {
                isBusy = false;
                eventBusy.postEvent(false);
            }
        }
    }

    public synchronized int getTaskCount() {
        return taskList.size();
    }

    public synchronized List<ITask> getTaskList(List<ITask> out) {
        if (out == null) {
            out = new ArrayList<>(taskList.size());
        }
        for (TaskItem item : taskList) {
            out.add(item.task);
        }
        return out;
    }

    public synchronized ITask getCurrentTask() {
        final TaskItem item = taskList.peekFirst();
        if (item != null) {
            return item.task;
        }
        return null;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    private void startNextTask(ITask prevTask) {
        TaskItem item = taskList.peekFirst();
        if (item != null) {
            eventTaskStart.postEvent(item);

            item.task.evtStart().register(this);
            item.task.evtProgress().register(this);
            item.task.evtFinished().register(this);

            item.task.start(this, prevTask);
        } else {
            isBusy = false;
            eventBusy.postEvent(false);
        }
    }

    public void addTask(ITask task) {
        addTask(task, null);
    }

    public synchronized void addTask(ITask task, String name) {
        if (task == null) {
            return;
        }

        if (!isBusy) {
            isBusy = true;
            eventBusy.postEvent(true);
        }

        TaskItem item = new TaskItem();
        item.task = task;
        item.name = name == null ? task.getName() : name;

        taskList.add(item);
        eventTaskAdded.postEvent(item);

        if (taskList.size() == 1) {
            startNextTask(null);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getParameter(String key) {
        return (T) parameters.get(key);
    }

    @Override
    public <T> void setParameter(String key, T val) {
        parameters.put(key, val);
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    public void onEvent(Object src, int evtType, Object evtData) {
        if (evtType == ITask.EVT_FINISH) {
            TaskItem item;
            synchronized (this) {
                item = taskList.peekFirst();
            }
            if (item != null && src == item.task) {// 移除监听
                item.task.evtStart().remove(this);
                item.task.evtProgress().remove(this);
                item.task.evtFinished().remove(this);
                item.result = (ITaskResult) evtData;

                eventTaskRemoved.postEvent(item);

                synchronized (this) {
                    // 启动下一个任务
                    startNextTask(taskList.removeFirst().task);
                }
            }
        } else if (evtType == ITask.EVT_PROGRESS) {
            TaskItem item;
            synchronized (this) {
                item = taskList.peekFirst();
            }
            if (item != null && src == item.task) {// 移除监听
                item.percent = (int) evtData;
                eventTaskProgress.postEvent(item);
            }
        }
    }

    public static class TaskItem {
        public ITask task;
        public String name;

        public int percent;
        public ITaskResult result;
    }
}
