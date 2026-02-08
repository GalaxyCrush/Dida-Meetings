package didameetings.server.paxos;

import didameetings.DidaMeetingsPaxos;
import didameetings.server.core.DidaMeetingsServerState;
import didameetings.server.paxos.messages.InstanceInfo;
import didameetings.server.paxos.messages.PhaseOneResult;
import didameetings.server.paxos.messages.PhaseTwoResult;
import didameetings.util.CollectorStreamObserver;
import didameetings.util.GenericResponseCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Proposer {

    private static final int NOOP = -1;
    private final DidaMeetingsServerState serverState;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private boolean leader_phase1_done = false;

    public Proposer(DidaMeetingsServerState serverState) {
        this.serverState = serverState;
    }

    public boolean getLeaderPhase1Done() {
        return leader_phase1_done;
    }

    public void setLeaderPhase1Done(boolean leaderPhase1Done) {
        this.leader_phase1_done = leaderPhase1Done;
    }

    public synchronized void startPhaseOne() {
        int ballot = serverState.getCurrentBallot();
        int completed_ballot = serverState.getCompletedBallot();

        if (completed_ballot < 0) {
            System.out.println("[Proposer] No prevBal activated (completed_ballot < 0). Phase1 trivially succeeds.");
            this.serverState.setActive(true);
            leader_phase1_done = true;
            return;
        }

        List<Integer> prevConfigAcceptors = serverState.getScheduler().acceptors(completed_ballot);

        PhaseOneProcessor phase_one_processor =
                new PhaseOneProcessor(serverState.getScheduler());

        ArrayList<DidaMeetingsPaxos.PhaseOneReply> responses = new ArrayList<>();
        GenericResponseCollector<DidaMeetingsPaxos.PhaseOneReply> collector =
                new GenericResponseCollector<>(responses, prevConfigAcceptors.size(), phase_one_processor);

        DidaMeetingsPaxos.PhaseOneRequest request = DidaMeetingsPaxos.PhaseOneRequest.newBuilder()
                .setInstance(0)
                .setRequestballot(ballot)
                .build();

        for (int acceptor : prevConfigAcceptors) {
            serverState.getAsync_stubs()[acceptor].phaseone(request, new CollectorStreamObserver<>(collector));
        }

        collector.waitUntilDone();
        PhaseOneResult phase_one_result = phase_one_processor.getResult();

        if (!phase_one_result.isAccepted()) {
            leader_phase1_done = false;
            int maxballot = phase_one_result.getMaxballot();
            if (maxballot > this.serverState.getCurrentBallot())
                this.serverState.setCurrentBallot(maxballot);
            return;
        }

        leader_phase1_done = true;

        System.out.println("[Proposer] Phase1 successful for ballot " + ballot + ", instanceMap: " + phase_one_result.getInstanceMap());

        List<Future<?>> futures = new ArrayList<>();

        // Fill gaps or propagate previously accepted values
        TreeMap<Integer, InstanceInfo> orderedMap = new TreeMap<>(phase_one_result.getInstanceMap());
        if (!orderedMap.isEmpty()) {
            int minKey = orderedMap.keySet().iterator().next();
            int maxKey = orderedMap.lastKey();

            for (int i = minKey; i <= maxKey; i++) {
                System.out.println("Proposer starting phase two for instance " + i);

                PaxosInstance entry = this.serverState.getPaxos_log().testAndSetEntry(i, ballot);

                if (entry.isDecided()) {
                    int finalI = i;
                    System.out.print(">> Instance " + i + " is already decided, re-proposing decided value " + entry.getCommand_id());
                    futures.add(executor.submit(() -> startPhaseTwo(finalI, ballot, entry.getCommand_id())));
                    continue;
                }

                if (!orderedMap.containsKey(i)) {
                    int finalI = i;
                    System.out.print(">> Gap at instance " + i + ", proposing NOOP");
                    futures.add(executor.submit(() -> startPhaseTwo(finalI, ballot, NOOP)));
                } else {
                    InstanceInfo info = orderedMap.get(i);
                    int valueToPropose = (info.valBallot > -1) ? info.value : NOOP;
                    int finalI = i;
                    System.out.print(">> Instance " + i + " already has accepted value " + info.value + " with ballot " + info.valBallot + ", re-proposing it");
                    futures.add(executor.submit(() -> startPhaseTwo(finalI, ballot, valueToPropose)));
                }
            }
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Calling updateCompletedBallot with " + ballot);
        this.serverState.setCompletedBallot(ballot);
    }

    public void startPhaseTwo(int entry_number, int ballot, int value) {
        List<Integer> acceptors = serverState.getScheduler().acceptors(ballot);
        int quorum = serverState.getScheduler().quorum(ballot);

        PhaseTwoProcessor processor = new PhaseTwoProcessor(quorum);
        ArrayList<DidaMeetingsPaxos.PhaseTwoReply> responses = new ArrayList<>();
        GenericResponseCollector<DidaMeetingsPaxos.PhaseTwoReply> collector =
                new GenericResponseCollector<>(responses, acceptors.size(), processor);

        DidaMeetingsPaxos.PhaseTwoRequest phase_two_request = DidaMeetingsPaxos.PhaseTwoRequest.newBuilder()
                .setInstance(entry_number)
                .setRequestballot(ballot)
                .setValue(value)
                .build();

        for (int acceptor : acceptors) {
            serverState.getAsync_stubs()[acceptor].phasetwo(phase_two_request, new CollectorStreamObserver<>(collector));
        }

        collector.waitUntilDone();
        PhaseTwoResult phase_two_result = processor.getResult();
        if (!phase_two_result.isAccepted()) {
            leader_phase1_done = false;
            serverState.setCurrentBallot(phase_two_result.getMaxballot());
            this.serverState.getReq_history().rollbackInProgress(value);
        } else {
            serverState.setCompletedBallot(ballot);
        }
    }

}
