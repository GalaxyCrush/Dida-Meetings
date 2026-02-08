package didameetings.server.paxos.messages;

public class PhaseTwoReply {
    private final boolean accepted;
    private final int instance;
    private final int ballot;
    private final int maxballot;

    public PhaseTwoReply(boolean accepted, int instance, int ballot, int maxballot) {
        this.accepted = accepted;
        this.instance = instance;
        this.ballot = ballot;
        this.maxballot = maxballot;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public int getInstance() {
        return instance;
    }

    public int getBallot() {
        return ballot;
    }

    public int getMaxballot() {
        return maxballot;
    }
}
