package didameetings.server.core;

import didameetings.DidaMeetingsMain;
import didameetings.DidaMeetingsPaxosServiceGrpc;
import didameetings.configs.ConfigurationScheduler;
import didameetings.core.MeetingManager;
import didameetings.server.paxos.PaxosInstance;
import didameetings.server.paxos.PaxosLog;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DidaMeetingsServerState {

    public static final int DEBUG_NONE = 0;
    public static final int DEBUG_MODE_CRASH = 1;
    public static final int DEBUG_MODE_FREEZE = 2;
    public static final int DEBUG_MODE_UNFREEZE = 3;
    public static final int DEBUG_MODE_SLOW_ON = 4;
    public static final int DEBUG_MODE_SLOW_OFF = 5;
    public static final int DEBUG_MODE_REMOVE_INSTANCE = 6;

    private List<PendingTopicOperation> pendingTopicOperations = new ArrayList<>();

    private int max_participants;
    private MeetingManager meeting_manager;
    private ConfigurationScheduler scheduler;
    private int base_port;
    private int my_id;
    private RequestHistory req_history;
    private PaxosLog paxos_log;
    private List<Integer> all_participants;
    private int n_participants;
    private String[] targets;
    private ManagedChannel[] channels;
    private DidaMeetingsPaxosServiceGrpc.DidaMeetingsPaxosServiceStub[] async_stubs;
    private MainLoop main_loop;
    private Thread main_loop_worker;
    private int current_ballot;
    private int completed_ballot;
    private int debug_mode;

    private boolean active = false;

    public DidaMeetingsServerState(int port, int myself, char schedule, int max) {
        this.max_participants = max;
        this.meeting_manager = new MeetingManager();
        this.scheduler = new ConfigurationScheduler(schedule);
        this.base_port = port;
        this.my_id = myself;
        this.debug_mode = 0;
        this.current_ballot = 0;
        this.completed_ballot = -1;
        this.req_history = new RequestHistory();
        this.paxos_log = new PaxosLog();
        this.main_loop = new MainLoop(this);

        // init comms
        this.all_participants = this.scheduler.allparticipants();
        this.n_participants = all_participants.size();

        this.targets = new String[this.n_participants];
        for (int i = 0; i < this.n_participants; i++) {
            int target_port = this.base_port + all_participants.get(i);
            this.targets[i] = new String();
            this.targets[i] = "localhost:" + target_port;
            System.out.printf("targets[%d] = %s%n", i, targets[i]);
        }

        this.channels = new ManagedChannel[this.n_participants];
        for (int i = 0; i < this.n_participants; i++)
            this.channels[i] = ManagedChannelBuilder.forTarget(this.targets[i]).usePlaintext().build();

        this.async_stubs = new DidaMeetingsPaxosServiceGrpc.DidaMeetingsPaxosServiceStub[this.n_participants];
        for (int i = 0; i < this.n_participants; i++)
            this.async_stubs[i] = DidaMeetingsPaxosServiceGrpc.newStub(this.channels[i]);

        // start worker
        this.main_loop_worker = new Thread(main_loop);
        this.main_loop_worker.start();
    }

    public void addPendingTopicOperation(PendingTopicOperation op) {
        synchronized (pendingTopicOperations) {
            System.out.println("Added topic " + op.getTopicId() + " to pending; User: " + op.getParticipantId() + " on meeting " + op.getMeetingId());
            pendingTopicOperations.add(op);
        }
    }

    public void checkAndExecutePendingTopic() {
        synchronized (pendingTopicOperations) {
            Iterator<PendingTopicOperation> iterator = pendingTopicOperations.iterator();
            while (iterator.hasNext()) {
                PendingTopicOperation op = iterator.next();

                System.out.println("Meeting: " + op.getMeetingId());
                System.out.println("User: " + op.getParticipantId());
                if (this.meeting_manager.getMeeting(op.getMeetingId()) != null
                        && this.meeting_manager.getMeeting(op.getMeetingId())
                        .getParticipant(op.getParticipantId()) != null) {

                    this.meeting_manager.setTopic(
                            op.getMeetingId(),
                            op.getParticipantId(),
                            op.getTopicId());

                    System.out.println("Topic with ID: " + op.getTopicId() + " executed; User: " + op.getParticipantId() + " on meeting " + op.getMeetingId());
                    // Remove the operation from the list
                    iterator.remove();
                    System.out.println("Processed pending topic operation for reqid " + op.getReqId());
                }

            }
        }
    }

    public synchronized boolean isActive() {
        return active;
    }

    public synchronized void setActive(boolean active) {
        this.active = active;
        if (active) {
            notifyAll();
        }
    }

    public synchronized int getCurrentBallot() {
        return this.current_ballot;
    }

    public synchronized void setCurrentBallot(int ballot) {
        if (ballot > this.current_ballot)
            this.current_ballot = ballot;
    }

    public synchronized int getCompletedBallot() {
        return this.completed_ballot;
    }

    public synchronized void setCompletedBallot(int ballot) {
        if (ballot > this.completed_ballot)
            this.completed_ballot = ballot;
        this.notifyAll();
    }

    public int findMaxDecidedBallot() {
        int ballot = -1;
        int length = this.paxos_log.length();

        for (int i = 0; i < length; i++) {
            PaxosInstance entry = this.paxos_log.getEntry(i);
            if (entry == null)
                return ballot;
            if (!entry.isDecided())
                return ballot;
            else if (entry.getAccept_ballot() > ballot)
                ballot = entry.getAccept_ballot();

        }
        return ballot;
    }

    public synchronized void updateCompletedBallot(int ballot) {
        // WARNING: THIS ONLY WORKS FOR CONFIGURATIONS WHERE THERE IS NO NEED FOR
        // STATE-TRANSFER!!!!!
        // NEEDS TO BE UPDATE FOR THE PROJECT
        ballot = this.findMaxDecidedBallot();
        System.out.println("LOG: findMaxDecidedBallot " + ballot);
        if (ballot > this.completed_ballot)
            this.completed_ballot = ballot;
        this.notifyAll();
        System.out.println("LOG: Completed Ballot " + this.completed_ballot);
    }

    public synchronized int waitForCompletedBallot(int ballot) {
        while (this.completed_ballot < ballot) {
            try {
                wait();
            } catch (InterruptedException _) {
            }
        }
        return this.completed_ballot;
    }

    public synchronized int getDebugMode() {
        return this.debug_mode;
    }

    public synchronized void setDebugMode(int mode) {
        this.debug_mode = mode;

        switch (mode) {
            case DEBUG_NONE:
                System.out.println("[DidaMeetingsServerState] Setting debug mode to NONE");
                break;

            case DEBUG_MODE_CRASH:
                System.out.println("[DidaMeetingsServerState] Setting debug mode to CRASH");
                System.exit(0);
                break;

            case DEBUG_MODE_FREEZE:
                System.out.println("[DidaMeetingsServerState] Setting debug mode to FREEZE");
                break;

            case DEBUG_MODE_UNFREEZE:
                System.out.println("[DidaMeetingsServerState] Setting debug mode to UNFREEZE");
                this.debug_mode = DEBUG_NONE;
                break;

            case DEBUG_MODE_SLOW_ON:
                System.out.println("[DidaMeetingsServerState] Setting debug mode to SLOW_ON");
                break;

            case DEBUG_MODE_SLOW_OFF:
                System.out.println("[DidaMeetingsServerState] Setting debug mode to SLOW_OFF");
                this.debug_mode = DEBUG_NONE;
                break;
        }
    }

    /**
     * Apply the current debug behavior.
     * This method should be called at the very start of every server method that
     * handles client requests
     */
    public void applyDebugBehavior() {
        switch (debug_mode) {
            case DEBUG_MODE_REMOVE_INSTANCE:
                int instanceToRemove = 1;
                this.paxos_log.getLog().remove(instanceToRemove);
                System.out
                        .println("[DidaMeetingsServerState] Removed instance " + instanceToRemove + " from Paxos log");
                System.out.println(paxos_log.toString());
                break;
            case DEBUG_MODE_FREEZE:
                synchronized (this) {
                    while (debug_mode == DEBUG_MODE_FREEZE) {
                        try {
                            this.wait(1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                break;
            case DEBUG_MODE_SLOW_ON:
                try {
                    Thread.sleep(300 + new java.util.Random().nextInt(700));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                break;

            case DEBUG_NONE:
            case DEBUG_MODE_CRASH:
            case DEBUG_MODE_UNFREEZE:
            case DEBUG_MODE_SLOW_OFF:
            default:
                break;
        }
    }

    public int getMax_participants() {
        return max_participants;
    }

    public void setMax_participants(int max_participants) {
        this.max_participants = max_participants;
    }

    public MeetingManager getMeeting_manager() {
        return meeting_manager;
    }

    public void setMeeting_manager(MeetingManager meeting_manager) {
        this.meeting_manager = meeting_manager;
    }

    public int getDebug_mode() {
        return debug_mode;
    }

    public void setDebug_mode(int debug_mode) {
        this.debug_mode = debug_mode;
    }

    public Thread getMain_loop_worker() {
        return main_loop_worker;
    }

    public void setMain_loop_worker(Thread main_loop_worker) {
        this.main_loop_worker = main_loop_worker;
    }

    public MainLoop getMain_loop() {
        return main_loop;
    }

    public void setMain_loop(MainLoop main_loop) {
        this.main_loop = main_loop;
    }

    public DidaMeetingsPaxosServiceGrpc.DidaMeetingsPaxosServiceStub[] getAsync_stubs() {
        return async_stubs;
    }

    public void setAsync_stubs(DidaMeetingsPaxosServiceGrpc.DidaMeetingsPaxosServiceStub[] async_stubs) {
        this.async_stubs = async_stubs;
    }

    public ManagedChannel[] getChannels() {
        return channels;
    }

    public void setChannels(ManagedChannel[] channels) {
        this.channels = channels;
    }

    public String[] getTargets() {
        return targets;
    }

    public void setTargets(String[] targets) {
        this.targets = targets;
    }

    public int getN_participants() {
        return n_participants;
    }

    public void setN_participants(int n_participants) {
        this.n_participants = n_participants;
    }

    public List<Integer> getAll_participants() {
        return all_participants;
    }

    public void setAll_participants(List<Integer> all_participants) {
        this.all_participants = all_participants;
    }

    public PaxosLog getPaxos_log() {
        return paxos_log;
    }

    public void setPaxos_log(PaxosLog paxos_log) {
        this.paxos_log = paxos_log;
    }

    public int getHighestDecidedInstance() {
        int highest = -1;
        for (Integer key : this.paxos_log.getLog().keySet()) {
            PaxosInstance instance = this.paxos_log.getLog().get(key);
            if (instance.isDecided() && key > highest) {
                highest = key;
            }
        }
        return highest;
    }

    public RequestHistory getReq_history() {
        return req_history;
    }

    public void setReq_history(RequestHistory req_history) {
        this.req_history = req_history;
    }

    public int getMy_id() {
        return my_id;
    }

    public void setMy_id(int my_id) {
        this.my_id = my_id;
    }

    public int getBase_port() {
        return base_port;
    }

    public void setBase_port(int base_port) {
        this.base_port = base_port;
    }

    public ConfigurationScheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(ConfigurationScheduler scheduler) {
        this.scheduler = scheduler;
    }

}
