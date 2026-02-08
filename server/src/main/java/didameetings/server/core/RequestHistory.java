package didameetings.server.core;

import java.util.Enumeration;
import java.util.Hashtable;

public class RequestHistory {

    private final Hashtable<Integer, RequestRecord> pending;
    private final Hashtable<Integer, RequestRecord> inProgress;
    private final Hashtable<Integer, RequestRecord> processed;

    public RequestHistory() {
        this.pending = new Hashtable<Integer, RequestRecord>();
        this.inProgress = new Hashtable<Integer, RequestRecord>();
        this.processed = new Hashtable<Integer, RequestRecord>();
    }

    public synchronized RequestRecord getIfPending(int requestid) {
        Integer id = requestid;
        return this.pending.get(id);
    }

    public synchronized void moveToInProgress(int requestid) {
        RequestRecord record = this.pending.remove(requestid);
        if (record != null) {
            this.inProgress.put(requestid, record);
        }
    }

    public synchronized RequestRecord moveToProcessed(int requestid) {
        RequestRecord record = this.inProgress.remove(requestid);
        if (record == null) {
            record = this.pending.remove(requestid);
        }
        this.processed.put(requestid, record);
        return record;
    }

    public synchronized RequestRecord getFirstPending() {
        Enumeration<Integer> pendingids = this.pending.keys();
        if (pendingids.hasMoreElements())
            return this.pending.get(pendingids.nextElement());
        else
            return null;
    }

    public synchronized RequestRecord getIfProcessed(int requestid) {
        Integer id = requestid;
        return this.processed.get(id);
    }

    public synchronized RequestRecord getIfExists(int requestid) {
        RequestRecord record;
        Integer id = requestid;

        record = this.pending.get(id);
        if (record == null)
            record = this.processed.get(id);
        return record;
    }

    public synchronized void addToPending(int requestid, RequestRecord record) {
        Integer id = requestid;
        this.pending.put(id, record);
    }

    public synchronized RequestRecord getIfInProgress(int requestid) {
        return this.inProgress.get(requestid);
    }

    public synchronized RequestRecord rollbackInProgress(int requestid) {
        RequestRecord record = this.inProgress.remove(requestid);
        if (record != null) {
            this.pending.put(requestid, record);
        }
        return record;
    }

}
