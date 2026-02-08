package didameetings.util;

import java.util.ArrayList;

public class GenericResponseCollector<T> {
    
    private final GenericResponseProcessor<T> processor;
    private final ArrayList<T> collected_responses;
    private int received = 0;
    private int pending;
    private boolean done = false;

    public GenericResponseCollector(ArrayList<T> responses, int maxresponses) {
        this(responses, maxresponses, null);
    }

    public GenericResponseCollector(ArrayList<T> responses, int maxresponses, GenericResponseProcessor<T> p) {
        this.processor = p;
        this.collected_responses = responses;
        this.pending = maxresponses;
    }

    public synchronized void addResponse(T resp) {
        if (!this.done) {
            collected_responses.add(resp);
            if (this.processor != null)
                this.done = this.processor.onNext(this.collected_responses, resp);
            // true -> maioria
            // true -> NACK
            //
        }
        this.received++;
        this.pending--;
        if (this.pending == 0)
            this.done = true;
        notifyAll();
    }

    public synchronized void addNoResponse() {
        this.pending--;
        if (this.pending == 0)
            this.done = true;
        notifyAll();
    }

    public synchronized void waitForQuorum(int quorum) {
        while ((!this.done) && (this.received < quorum)) {
            try {
                wait();
            } catch (InterruptedException _) {
                System.err.println("Interrupted while waiting for quorum");
            }
        }
        this.done = true;
    }


    public synchronized void waitUntilDone() {
        while (!this.done) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.err.println("Interrupted while waiting for responses until done");
            }
        }
    }
}
