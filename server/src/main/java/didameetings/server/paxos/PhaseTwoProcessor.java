package didameetings.server.paxos;

import didameetings.DidaMeetingsPaxos;
import didameetings.server.paxos.messages.PhaseTwoResult;
import didameetings.util.GenericResponseProcessor;

import java.util.ArrayList;

public class PhaseTwoProcessor extends GenericResponseProcessor<DidaMeetingsPaxos.PhaseTwoReply> {

    private final int quorum;
    private boolean accepted = true;
    private int maxballot = 0;
    private int responses = 0;

    public PhaseTwoProcessor(int quorum) {
        this.quorum = quorum;
    }

    @Override
    public boolean onNext(ArrayList<DidaMeetingsPaxos.PhaseTwoReply> all_responses, DidaMeetingsPaxos.PhaseTwoReply lastResponse) {
        this.responses++;
        if (!lastResponse.getAccepted()) {
            accepted = false;
            if (lastResponse.getMaxballot() > this.maxballot) {
                this.maxballot = lastResponse.getMaxballot();
            }
            return true;
        }
        return this.responses >= quorum;
    }

    public PhaseTwoResult getResult() {
        return new PhaseTwoResult(accepted, maxballot, true);
    }
}
