package didameetings.server.paxos.messages;

import java.util.Map;

public class PhaseOneReply {
    private final int instance;
    private final boolean accepted;
    private final int maxballot;
    private final Map<Integer, InstanceInfo> instanceMap;

    public PhaseOneReply(int instance, boolean accepted, Map<Integer, InstanceInfo> instanceMap, int maxballot) {
        this.instance = instance;
        this.accepted = accepted;
        this.instanceMap = instanceMap;
        this.maxballot = maxballot;
    }

    public int getInstance() {
        return instance;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public Map<Integer, InstanceInfo> getInstanceMap() {
        return instanceMap;
    }

    public int getMaxballot() {
        return maxballot;
    }
}
