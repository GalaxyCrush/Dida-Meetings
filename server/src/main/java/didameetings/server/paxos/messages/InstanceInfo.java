package didameetings.server.paxos.messages;

public class InstanceInfo {
    public final int value;
    public final int valBallot;

    public InstanceInfo(int value, int valBallot) {
        this.value = value;
        this.valBallot = valBallot;
    }
}
