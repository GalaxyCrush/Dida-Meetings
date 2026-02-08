package didameetings.server.paxos.messages;

import java.util.Map;

public class PhaseOneResult {

    private final boolean accepted;
    private final Map<Integer, InstanceInfo> instanceMap;
    private final int maxballot;
    private final boolean isDone;

    public PhaseOneResult(boolean accepted, Map<Integer, InstanceInfo> instanceMap, int maxballot, boolean isDone) {
        this.accepted = accepted;
        this.instanceMap = instanceMap;
        this.maxballot = maxballot;
        this.isDone = isDone;
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

    public boolean isDone() {
        return isDone;
    }

}
