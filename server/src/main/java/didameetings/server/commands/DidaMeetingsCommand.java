package didameetings.server.commands;

public record DidaMeetingsCommand(DidaMeetingsAction action, int meeting_id, int participant_id, int topic_id) {
    public DidaMeetingsCommand(DidaMeetingsAction command_type) {
        this(command_type, 0, 0, -1);
    }

    public DidaMeetingsCommand(DidaMeetingsAction command_type, int mid) {
        this(command_type, mid, 0, -1);
    }

    public DidaMeetingsCommand(DidaMeetingsAction command_type, int mid, int pid) {
        this(command_type, mid, pid, -1);
    }

    public int getMeetingId() {
        return this.meeting_id;
    }

    public int getParticipantId() {
        return this.participant_id;
    }

    public int getTopicId() {
        return this.topic_id;
    }
}
