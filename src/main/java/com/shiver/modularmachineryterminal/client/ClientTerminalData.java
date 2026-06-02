package com.shiver.modularmachineryterminal.client;

import com.shiver.modularmachineryterminal.common.MachineInfo;
import com.shiver.modularmachineryterminal.common.MachineKey;
import com.shiver.modularmachineryterminal.common.SummaryInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClientTerminalData {

    private static final Map<MachineKey, MachineInfo> MACHINES = new LinkedHashMap<>();
    private static SummaryInfo summary = new SummaryInfo();

    /**
     * 创建 ClientTerminalData 实例。
     */
    private ClientTerminalData() {
    }

    /**
     * 执行 set full list 相关逻辑。
     * @param newSummary newSummary 参数
     * @param machines 机器列表
     */
    public static void setFullList(SummaryInfo newSummary, List<MachineInfo> machines) {
        summary = newSummary;
        MACHINES.clear();
        for (MachineInfo machine : machines) {
            MACHINES.put(machine.key, machine);
        }
    }

    /**
     * 执行 update dynamic 相关逻辑。
     * @param newSummary newSummary 参数
     * @param machines 机器列表
     * @param removed 已移除机器键列表
     */
    public static void updateDynamic(SummaryInfo newSummary, List<MachineInfo> machines, List<MachineKey> removed) {
        summary = newSummary;
        for (MachineKey key : removed) {
            MACHINES.remove(key);
        }
        for (MachineInfo dynamic : machines) {
            MACHINES.put(dynamic.key, dynamic);
        }
    }

    /**
     * 执行 get summary 相关逻辑。
     * @return 方法执行结果
     */
    public static SummaryInfo getSummary() {
        return summary;
    }

    /**
     * 执行 get machines 相关逻辑。
     * @return 符合条件的列表
     */
    public static List<MachineInfo> getMachines() {
        return new ArrayList<>(MACHINES.values());
    }

    /**
     * 执行 get machine 相关逻辑。
     * @param key 目标机器键
     * @return 方法执行结果
     */
    public static MachineInfo getMachine(MachineKey key) {
        return MACHINES.get(key);
    }
}
