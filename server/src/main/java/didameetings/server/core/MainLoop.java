package didameetings.server.core;

import didameetings.server.commands.DidaMeetingsAction;
import didameetings.server.commands.DidaMeetingsCommand;
import didameetings.server.paxos.EntryDecidedCallback;
import didameetings.server.paxos.PaxosInstance;
import didameetings.server.paxos.Proposer;

import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainLoop implements Runnable, EntryDecidedCallback {

    private final DidaMeetingsServerState serverState;
    private final Proposer proposer;
    private final PriorityQueue<Integer> decidedQueue = new PriorityQueue<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private int next_log_entry = -1;
    private int next_executable_entry = 0;

    public MainLoop(DidaMeetingsServerState state) {
        this.serverState = state;
        this.proposer = new Proposer(state);
    }

    @Override
    public void onEntryDecided(int entryNumber) {
        synchronized (this) {
            System.out.println("Entry " + entryNumber + " decided, next executable entry: " + next_executable_entry);
            int currentEntry = entryNumber;
            do {
                if (currentEntry == next_executable_entry) {
                    System.out.println(" >>> Executing entry " + currentEntry);
                    PaxosInstance entry = serverState.getPaxos_log().getEntry(currentEntry);
                    if (entry.getCommand_id() == -1) {
                        System.out.println(" >>> Entry " + currentEntry + " is a NO-OP");
                    } else {
                        System.out.println(
                                " >>> Entry " + currentEntry + " is a command with id " + entry.getCommand_id());
                        executeCommand(entry);
                    }
                    next_executable_entry++;
                    if (!decidedQueue.isEmpty() && decidedQueue.peek() == next_executable_entry) {
                        currentEntry = decidedQueue.poll();
                    } else {
                        break;
                    }
                } else {
                    System.out.println(" >>> Waiting to execute entry" + currentEntry);
                    decidedQueue.add(currentEntry);
                    break;
                }
            } while (true);
        }
    }

    public void run() {
        while (true) {

            synchronized (this) {
                System.out.println("Main loop waiting for work...");
                if (serverState.getReq_history().getFirstPending() != null) {
                    System.out.println(
                            "First pending request: " + serverState.getReq_history().getFirstPending().getRequest());
                }
                waitForWork();
            }

            System.out.println("Main found work...");

            int ballot = serverState.getCurrentBallot();

            if (proposer.getLeaderPhase1Done() && !isLeader(ballot)) {
                proposer.setLeaderPhase1Done(false);
            }

            // Leader catch-up: run phase 1 once when we become leader
            if (isLeader(ballot)) {

                if (!proposer.getLeaderPhase1Done()) {
                    System.out.println("Starting phase 1 as leader for ballot " + ballot);
                    proposer.startPhaseOne();
                    System.out.println("Ended phase 1 as ballot " + ballot);
                    next_log_entry = this.serverState.getHighestDecidedInstance();
                    System.out.println("Log entry " + next_log_entry);
                }

                RequestRecord request_record;

                while ((request_record = serverState.getReq_history().getFirstPending()) != null) {
                    next_log_entry++;
                    System.out.println("Next log entry to propose: " + next_log_entry + ", current ballot: " + ballot
                            + ", leader_phase1_done: " + proposer.getLeaderPhase1Done() + "\n >> for request id "
                            + request_record.getRequest());

                    if (!this.serverState.isActive()) {
                        System.out.println("Starting phase 1 as leader for ballot " + ballot);
                        continue;
                    }

                    serverState.getReq_history().moveToInProgress(request_record.getId());

                    System.out.println("Starting phase 2 for log entry " + next_log_entry + " with command id "
                            + request_record.getId() + " in ballot " + ballot);
                    int entry_number = next_log_entry;
                    PaxosInstance entry = serverState.getPaxos_log().testAndSetEntry(entry_number, ballot);
                    if (entry.getCommand_id() == 0) {
                        System.out.println("Inside proposer main loop, entry is null or NOOP");
                        final int finalEntryNumber = entry_number;
                        final int finalBallot = ballot;
                        final int finalRequestId = request_record.getId();
                        executor.submit(() -> proposer.startPhaseTwo(finalEntryNumber, finalBallot, finalRequestId));
                    } else {
                        System.out.println("Entry " + entry_number + " already proposed with command id "
                                + entry.getCommand_id() + ", skipping...");
                    }
                }
            }
        }
    }

    private void executeCommand(PaxosInstance entry) {
        System.out.println("Executing command " + entry.getCommand_id());
        RequestRecord requestRecord = serverState.getReq_history().getIfInProgress(entry.getCommand_id());
        if (requestRecord == null) {
            // Try to fetch from pending
            requestRecord = serverState.getReq_history().getIfPending(entry.getCommand_id());
            if (requestRecord != null) {
                // Move to in-progress before execution
                serverState.getReq_history().moveToInProgress(requestRecord.getId());
            } else {
                System.out.println("Request record for command id " + entry.getCommand_id() + " not found, waiting...");
                waitForWork();
                return;
            }
        }
        DidaMeetingsCommand command = requestRecord.getRequest();
        boolean result = processCommand(command);

        // After executing OPEN or ADD, check if there are pending topic assignments to
        // execute
        if (result && (command.action() == DidaMeetingsAction.OPEN ||
                command.action() == DidaMeetingsAction.ADD)) {
            this.serverState.checkAndExecutePendingTopic();
        }

        requestRecord.setResponse(result);
        this.serverState.getReq_history().moveToProcessed(requestRecord.getId());

        System.out.println("Executed command: " + command);
    }

    private boolean processCommand(DidaMeetingsCommand command) {
        DidaMeetingsAction action = command.action();

        return switch (action) {
            case OPEN ->
                    serverState.getMeeting_manager().open(command.getMeetingId(), serverState.getMax_participants());
            case ADD ->
                    serverState.getMeeting_manager().addAndClose(command.getMeetingId(), command.getParticipantId());
            case TOPIC -> serverState.getMeeting_manager().setTopic(command.getMeetingId(), command.getParticipantId(),
                    command.getTopicId());
            case CLOSE -> serverState.getMeeting_manager().close(command.getMeetingId());
            case DUMP -> {
                serverState.getMeeting_manager().dump();
                yield true;
            }
            default -> {
                System.out.println("Unknown action: " + action);
                yield false;
            }
        };

    }

    private boolean isLeader(int ballot) {
        return serverState.getScheduler().leader(ballot) == serverState.getMy_id();
    }

    public synchronized void wakeup() {
        notify();
    }

    private void waitForWork() {
        try {
            wait();
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for work");
        }
    }

}
