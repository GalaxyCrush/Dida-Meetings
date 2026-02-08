package didameetings.server.paxos;

import java.util.concurrent.ConcurrentHashMap;


public class PaxosLog {

    private final ConcurrentHashMap<Integer, PaxosInstance> log;

    public PaxosLog() {
        this.log = new ConcurrentHashMap<>();
    }

    public synchronized int length() {
        return this.log.size();
    }

    public synchronized PaxosInstance getEntry(int position) {
        return this.log.get(position);
    }

    public synchronized PaxosInstance testAndSetEntry(int position) {
        PaxosInstance entry = this.log.get(position);

        if (entry == null) {
            entry = new PaxosInstance(position);
            this.log.put(position, entry);
        }
        return entry;
    }


    public synchronized PaxosInstance testAndSetEntry(int position, int ballot) {
        PaxosInstance entry = this.log.get(position);

        if (entry == null) {
            entry = new PaxosInstance(position, ballot);
            this.log.put(position, entry);
        }
        return entry;
    }

    public ConcurrentHashMap<Integer, PaxosInstance> getLog() {
        return this.log;
    }
}
