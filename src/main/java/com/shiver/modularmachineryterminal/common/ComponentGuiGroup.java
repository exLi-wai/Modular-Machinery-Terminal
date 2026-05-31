package com.shiver.modularmachineryterminal.common;

public enum ComponentGuiGroup {
    INPUT,
    OUTPUT,
    PATTERN,
    UPGRADE;

    public static ComponentGuiGroup byId(int id) {
        ComponentGuiGroup[] values = values();
        if (id < 0 || id >= values.length) {
            return INPUT;
        }
        return values[id];
    }
}
