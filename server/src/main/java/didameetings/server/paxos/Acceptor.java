package didameetings.server.paxos;

import didameetings.server.core.DidaMeetingsServerState;
import didameetings.server.paxos.messages.InstanceInfo;
import didameetings.server.paxos.messages.PhaseOneReply;
import didameetings.server.paxos.messages.PhaseTwoReply;

import java.util.HashMap;
import java.util.Map;

public class Acceptor {

    private final DidaMeetingsServerState serverState;

    public Acceptor(DidaMeetingsServerState serverState) {
        this.serverState = serverState;
    }

    public synchronized PhaseOneReply handlePhaseOne(int startInstance, int ballot) {

        boolean accepted = false;

        // Update current_ballot atomically
        if (ballot >= this.serverState.getCurrentBallot()) {
            this.serverState.setCurrentBallot(ballot);
            accepted = true;
        }

        Map<Integer, InstanceInfo> instanceMap = new HashMap<>();
        for (Map.Entry<Integer, PaxosInstance> e : this.serverState.getPaxos_log().getLog().entrySet()) {
            int instance = e.getKey();
            PaxosInstance entry = e.getValue();
            synchronized (entry) {
                if (accepted) {
                    entry.setRead_ballot(ballot);
                }
            }
            System.out.println(" >>> Acceptor phase one reply for instance " + instance + ": value = " + entry.getCommand_id() + ", valBallot = " + entry.getWrite_ballot());
            instanceMap.put(instance, new InstanceInfo(entry.getCommand_id(), entry.getWrite_ballot()));
        }

        return new PhaseOneReply(startInstance, accepted, instanceMap, this.serverState.getCurrentBallot());

    }

    public synchronized PhaseTwoReply handlePhaseTwo(int instance, int ballot, int value) {
        PaxosInstance entry = this.serverState.getPaxos_log().testAndSetEntry(instance);
        boolean accepted = false;
        int maxballot = ballot;
        if (ballot >= this.serverState.getCurrentBallot()) {
            accepted = true;
            entry.setCommand_id(value);
            entry.setWrite_ballot(ballot);
            this.serverState.setCurrentBallot(ballot);
        } else {
            maxballot = this.serverState.getCurrentBallot();
        }
        return new PhaseTwoReply(accepted, instance, ballot, maxballot);
    }
}
