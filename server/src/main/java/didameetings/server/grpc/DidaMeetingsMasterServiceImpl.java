package didameetings.server.grpc;

import didameetings.DidaMeetingsMaster;
import didameetings.DidaMeetingsMasterServiceGrpc;
import didameetings.server.core.DidaMeetingsServerState;
import io.grpc.stub.StreamObserver;

public class DidaMeetingsMasterServiceImpl extends DidaMeetingsMasterServiceGrpc.DidaMeetingsMasterServiceImplBase {
    DidaMeetingsServerState server_state;


    public DidaMeetingsMasterServiceImpl(DidaMeetingsServerState state) {
        this.server_state = state;
    }

    @Override
    public void newballot(DidaMeetingsMaster.NewBallotRequest request, StreamObserver<DidaMeetingsMaster.NewBallotReply> responseObserver) {
        System.out.println(request);

        int request_id = request.getReqid();
        int new_ballot = request.getNewballot();
        int completed_ballot = request.getCompletedballot();

        // for debug purposes
        System.out.println("Current ballot = " + this.server_state.getCurrentBallot() + " new ballot = " + new_ballot + " completed ballot = " + completed_ballot);

        this.server_state.setCompletedBallot(completed_ballot);

        if (new_ballot > this.server_state.getCurrentBallot()) {
            this.server_state.setCurrentBallot(new_ballot);

            this.server_state.setActive(false);

            this.server_state.getMain_loop().wakeup();

            completed_ballot = this.server_state.waitForCompletedBallot(new_ballot);
        } else {
            completed_ballot = this.server_state.getCompletedBallot();
        }

        System.out.println("Responding to NEWBALLOT with completed ballot = " + completed_ballot);
        DidaMeetingsMaster.NewBallotReply.Builder response_builder = DidaMeetingsMaster.NewBallotReply.newBuilder();
        response_builder.setReqid(request_id);
        response_builder.setCompletedballot(completed_ballot);

        DidaMeetingsMaster.NewBallotReply response = response_builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void activate(DidaMeetingsMaster.ActivateRequest request,
                         StreamObserver<DidaMeetingsMaster.ActivateReply> responseObserver) {

        int request_id = request.getReqid();
        int ballot = request.getBallot();

        System.out.println("[MasterRPC] Received ACTIVATE for ballot " + ballot);

        // Only activate if it's the current ballot
        if (ballot == this.server_state.getCurrentBallot()) {
            this.server_state.setActive(true);
            this.server_state.getMain_loop().wakeup();
            System.out.println("[MasterRPC] Server is now ACTIVE for ballot " + ballot);
        } else {
            System.out.println("[MasterRPC] Ignored ACTIVATE (outdated ballot)");
        }

        DidaMeetingsMaster.ActivateReply reply = DidaMeetingsMaster.ActivateReply.newBuilder()
                .setReqid(request_id)
                .setAck(true)
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void setdebug(DidaMeetingsMaster.SetDebugRequest request, StreamObserver<DidaMeetingsMaster.SetDebugReply> responseObserver) {
        // for debug purposes
        System.out.println(request);

        boolean response_value = true;

        int request_id = request.getReqid();
        this.server_state.setDebugMode(request.getMode());

        // for debug purposes
        System.out.println("Setting debug mode to = " + this.server_state.getDebugMode());

        DidaMeetingsMaster.SetDebugReply.Builder response_builder = DidaMeetingsMaster.SetDebugReply.newBuilder();
        response_builder.setReqid(request_id);
        response_builder.setAck(response_value);

        DidaMeetingsMaster.SetDebugReply response = response_builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
