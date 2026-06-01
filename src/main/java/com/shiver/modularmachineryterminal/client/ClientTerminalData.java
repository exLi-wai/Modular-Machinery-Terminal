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

    private ClientTerminalData() {
    }

    public static void setFullList(SummaryInfo newSummary, List<MachineInfo> machines) {
        summary = newSummary;
        MACHINES.clear();
        for (MachineInfo machine : machines) {
            MACHINES.put(machine.key, machine);
        }
    }

    public static void updateDynamic(SummaryInfo newSummary, List<MachineInfo> machines, List<MachineKey> removed) {
        summary = newSummary;
        for (MachineKey key : removed) {
            MACHINES.remove(key);
        }
        for (MachineInfo dynamic : machines) {
            MACHINES.put(dynamic.key, dynamic);
        }
    }

    public static SummaryInfo getSummary() {
        return summary;
    }

    public static List<MachineInfo> getMachines() {
        return new ArrayList<>(MACHINES.values());
    }

    public static MachineInfo getMachine(MachineKey key) {
        return MACHINES.get(key);
    }
}
