package didameetings.server.paxos;

import didameetings.DidaMeetingsPaxos;
import didameetings.configs.ConfigurationScheduler;
import didameetings.server.paxos.messages.InstanceInfo;
import didameetings.server.paxos.messages.PhaseOneResult;
import didameetings.util.GenericResponseProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PhaseOneProcessor extends GenericResponseProcessor<DidaMeetingsPaxos.PhaseOneReply> {

    private final ConfigurationScheduler scheduler;

    private boolean accepted = true;
    private int maxballot = -1;
    private int responses = 0;
    private Map<Integer, InstanceInfo> instanceMap;

    public PhaseOneProcessor(ConfigurationScheduler scheduler) {
        this.scheduler = scheduler;
        this.instanceMap = new HashMap<>();
    }

    @Override
    public synchronized boolean onNext(ArrayList<DidaMeetingsPaxos.PhaseOneReply> all_responses, DidaMeetingsPaxos.PhaseOneReply last_response) {
        // Update the overall acceptor-level accepted flag
        this.accepted &= last_response.getAccepted();
        if (!this.accepted) {
            if (last_response.getMaxballot() > this.maxballot)
                this.maxballot = last_response.getMaxballot();
            return true; // stop early if quorum already lost
        }

        this.responses++;

        // Process each instance in the PhaseOneReply
        for (DidaMeetingsPaxos.InstanceState instanceState : last_response.getInstancesList()) {
            int instance = instanceState.getInstance();
            int val = instanceState.getValue();
            int valBallot = instanceState.getValballot();

            // If we haven't seen this instance yet, or the valballot is higher, update it
            if (!instanceMap.containsKey(instance) || valBallot > instanceMap.get(instance).valBallot) {
                instanceMap.put(instance, new InstanceInfo(val, valBallot));
            }
        }

        int requiredQuorum = this.scheduler.quorum(last_response.getRequestballot());
        return responses >= requiredQuorum;
    }

    public PhaseOneResult getResult() {
        return new PhaseOneResult(accepted, instanceMap, maxballot, true);
    }
}
