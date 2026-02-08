package didameetings.server.core;

import didameetings.DidaMeetingsMain;
import io.grpc.stub.StreamObserver;

public class PendingTopicOperation {

    private final int reqId;
    private final int topicId;
    private final int meetingId;
    private final int participantId;
    private final StreamObserver<DidaMeetingsMain.TopicReply> responseObserver;

    public PendingTopicOperation(int reqId, int topicId, int meetingId, int participantId,
            StreamObserver<DidaMeetingsMain.TopicReply> responseObserver) {
        this.reqId = reqId;
        this.topicId = topicId;
        this.meetingId = meetingId;
        this.participantId = participantId;
        this.responseObserver = responseObserver;
    }

    public int getReqId() {
        return reqId;
    }

    public int getTopicId() {
        return topicId;
    }

    public int getMeetingId() {
        return meetingId;
    }

    public int getParticipantId() {
        return participantId;
    }

    public StreamObserver<DidaMeetingsMain.TopicReply> getResponseObserver() {
        return responseObserver;
    }

}
