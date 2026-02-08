package didameetings.server.paxos.messages;

public class PhaseTwoResult {
    private final boolean accepted;
    private final int maxballot;
    private final boolean isDone;

    public PhaseTwoResult(boolean accepted, int maxballot, boolean isDone) {
        this.accepted = accepted;
        this.maxballot = maxballot;
        this.isDone = isDone;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public int getMaxballot() {
        return maxballot;
    }

    public boolean isDone() {
        return isDone;
    }
}

