package didameetings.server.paxos;


public class PaxosInstance {
    private int instance_nb;
    private int command_id;
    private int read_ballot;
    private int write_ballot;
    private int accept_ballot;
    private int n_accepts;
    private boolean decided;
    private boolean value_is_locked;

    public PaxosInstance() {

        this.instance_nb = 0;
        this.command_id = 0;
        this.read_ballot = -1;
        this.write_ballot = -1;
        this.accept_ballot = -1;
        this.n_accepts = 0;
        this.decided = false;
        this.value_is_locked = false;
    }
    
    public PaxosInstance(int id) {
        this.instance_nb = id;
        this.command_id = 0;
        this.read_ballot = -1;
        this.write_ballot = -1;
        this.accept_ballot = -1;
        this.n_accepts = 0;
        this.decided = false;
        this.value_is_locked = false;
    }

    public PaxosInstance(int id, int ballot) {
        this.instance_nb = id;
        this.command_id = 0;
        this.read_ballot = ballot;
        this.write_ballot = -1;
        this.accept_ballot = -1;
        this.n_accepts = 0;
        this.decided = false;
        this.value_is_locked = false;
    }

    // Getter and Setter methods for all fields

    public int getAccept_ballot() {
        return accept_ballot;
    }

    public void setAccept_ballot(int accept_ballot) {
        this.accept_ballot = accept_ballot;
    }

    public int getCommand_id() {
        return command_id;
    }

    public void setCommand_id(int command_id) {
        this.command_id = command_id;
    }

    public int getInstance_nb() {
        return instance_nb;
    }

    public void setInstance_nb(int instance_nb) {
        this.instance_nb = instance_nb;
    }

    public int getN_accepts() {
        return n_accepts;
    }

    public void setN_accepts(int n_accepts) {
        this.n_accepts = n_accepts;
    }

    public int getRead_ballot() {
        return read_ballot;
    }

    public void setRead_ballot(int read_ballot) {
        this.read_ballot = read_ballot;
    }

    public int getWrite_ballot() {
        return write_ballot;
    }

    public void setWrite_ballot(int write_ballot) {
        this.write_ballot = write_ballot;
    }

    public boolean isDecided() {
        return decided;
    }

    public void setDecided(boolean decided) {
        this.decided = decided;
    }

    public boolean isValue_is_locked() {
        return value_is_locked;
    }

    public void setValue_is_locked(boolean value_is_locked) {
        this.value_is_locked = value_is_locked;
    }
}
