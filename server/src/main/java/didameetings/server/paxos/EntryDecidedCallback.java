package didameetings.server.paxos;

public interface EntryDecidedCallback {
    void onEntryDecided(int entryNumber);
}
