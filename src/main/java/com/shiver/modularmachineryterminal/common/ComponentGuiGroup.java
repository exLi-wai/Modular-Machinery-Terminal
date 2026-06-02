package com.shiver.modularmachineryterminal.common;

public enum ComponentGuiGroup {
    CONTROLLER,
    INPUT,
    OUTPUT,
    PATTERN,
    UPGRADE,
    SMART_INTERFACE;

    /**
     * 根据序号返回对应的组件 GUI 分组。
     * @param id 分组序号
     * @return 方法执行结果
     */
    public static ComponentGuiGroup byId(int id) {
        ComponentGuiGroup[] values = values();
        if (id < 0 || id >= values.length) {
            return INPUT;
        }
        return values[id];
    }
}
