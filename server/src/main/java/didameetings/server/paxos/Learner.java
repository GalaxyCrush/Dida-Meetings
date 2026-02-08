package didameetings.server.paxos;

import didameetings.server.core.DidaMeetingsServerState;

public class Learner {
    private final DidaMeetingsServerState serverState;

    public Learner(DidaMeetingsServerState serverState) {
        this.serverState = serverState;
    }

    public void learn(int instance, int ballot, int value) {
        synchronized (this) {
            PaxosInstance entry = this.serverState.getPaxos_log().testAndSetEntry(instance);
            this.serverState.setCurrentBallot(ballot);

            if (entry.getAccept_ballot() == ballot) {
                entry.setN_accepts(entry.getN_accepts() + 1);
                if (entry.getN_accepts() >= this.serverState.getScheduler().quorum(ballot)) {
                    if(!entry.isDecided()) {
                        entry.setDecided(true);
                        System.out.println("Instance " + instance + " decided with value " + value + " at ballot " + ballot + " with n_accepts = " + entry.getN_accepts());
                        this.serverState.getMain_loop().onEntryDecided(instance);
                    }
                }
            } else if (ballot > entry.getAccept_ballot()) {
                entry.setCommand_id(value);
                entry.setAccept_ballot(ballot);
                entry.setN_accepts(1);
            }
        }
    }
}
