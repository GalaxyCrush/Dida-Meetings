package didameetings.server.grpc;


import didameetings.DidaMeetingsPaxos;
import didameetings.DidaMeetingsPaxosServiceGrpc;
import didameetings.server.core.DidaMeetingsServerState;
import didameetings.server.paxos.Acceptor;
import didameetings.server.paxos.Learner;
import didameetings.server.paxos.messages.InstanceInfo;
import didameetings.server.paxos.messages.PhaseOneReply;
import didameetings.server.paxos.messages.PhaseTwoReply;
import didameetings.util.CollectorStreamObserver;
import didameetings.util.GenericResponseCollector;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Paxos service implementation for DidaMeetings.
 * Each server runs all 3 roles: Proposer, Acceptor, Learner.
 */
public class DidaMeetingsPaxosServiceImpl extends DidaMeetingsPaxosServiceGrpc.DidaMeetingsPaxosServiceImplBase {

    private final Learner learner;
    private final Acceptor acceptor;
    DidaMeetingsServerState serverState;

    public DidaMeetingsPaxosServiceImpl(DidaMeetingsServerState state) {
        this.serverState = state;
        this.learner = new Learner(state);
        this.acceptor = new Acceptor(state);
    }

    @Override
    public void phaseone(DidaMeetingsPaxos.PhaseOneRequest request, StreamObserver<DidaMeetingsPaxos.PhaseOneReply> responseObserver) {

        PhaseOneReply reply = this.acceptor.handlePhaseOne(request.getInstance(), request.getRequestballot());

        DidaMeetingsPaxos.PhaseOneReply.Builder responseBuilder = DidaMeetingsPaxos.PhaseOneReply.newBuilder();
        responseBuilder.setServerid(this.serverState.getMy_id());
        responseBuilder.setRequestballot(request.getRequestballot());
        responseBuilder.setAccepted(reply.isAccepted());
        responseBuilder.setMaxballot(this.serverState.getCurrentBallot());

        for (Map.Entry<Integer, InstanceInfo> entry : reply.getInstanceMap().entrySet()) {
            DidaMeetingsPaxos.InstanceState.Builder instanceStateBuilder =
                    DidaMeetingsPaxos.InstanceState.newBuilder();
            instanceStateBuilder.setInstance(entry.getKey());
            instanceStateBuilder.setValue(entry.getValue().value);
            instanceStateBuilder.setValballot(entry.getValue().valBallot);
            responseBuilder.addInstances(instanceStateBuilder);
        }

        DidaMeetingsPaxos.PhaseOneReply responses = responseBuilder.build();

        responseObserver.onNext(responses);
        responseObserver.onCompleted();
    }

    @Override
    public void phasetwo(DidaMeetingsPaxos.PhaseTwoRequest request, StreamObserver<DidaMeetingsPaxos.PhaseTwoReply> responseObserver) {

        PhaseTwoReply reply = this.acceptor.handlePhaseTwo(request.getInstance(), request.getRequestballot(), request.getValue());

        DidaMeetingsPaxos.PhaseTwoReply response = DidaMeetingsPaxos.PhaseTwoReply.newBuilder()
                .setAccepted(reply.isAccepted())
                .setInstance(reply.getInstance())
                .setServerid(this.serverState.getMy_id())
                .setRequestballot(reply.getBallot())
                .setMaxballot(reply.getMaxballot())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
        if (reply.isAccepted()) {
            notifyLearners(request.getInstance(), request.getRequestballot(), request.getValue());
        }
    }

    private void notifyLearners(int instance, int ballot, int value) {
        Context ctx = Context.current().fork();
        ctx.run(() -> {
            List<Integer> learners = this.serverState.getScheduler().learners(ballot);
            int n_targets = learners.size();

            DidaMeetingsPaxos.LearnRequest learnRequest = DidaMeetingsPaxos.LearnRequest.newBuilder()
                    .setInstance(instance)
                    .setValue(value)
                    .setBallot(ballot)
                    .build();


            ArrayList<DidaMeetingsPaxos.LearnReply> learnResponses = new ArrayList<>();
            GenericResponseCollector<DidaMeetingsPaxos.LearnReply> collector = new GenericResponseCollector<>(learnResponses, n_targets);

            for (int learnerId : learners) {
                CollectorStreamObserver<DidaMeetingsPaxos.LearnReply> observer =
                        new CollectorStreamObserver<>(collector);
                this.serverState.getAsync_stubs()[learnerId].learn(learnRequest, observer);

            }
        });
    }

    @Override
    public void learn(DidaMeetingsPaxos.LearnRequest request, StreamObserver<DidaMeetingsPaxos.LearnReply> responseObserver) {
        this.learner.learn(request.getInstance(), request.getBallot(), request.getValue());

        DidaMeetingsPaxos.LearnReply response = DidaMeetingsPaxos.LearnReply.newBuilder()
                .setInstance(request.getInstance())
                .setBallot(request.getBallot())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
