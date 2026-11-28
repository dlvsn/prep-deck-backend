package denys.mazurenko.prepdeckbackend.service.share;

import denys.mazurenko.prepdeckbackend.dto.share.request.AcceptShareRequest;
import denys.mazurenko.prepdeckbackend.dto.topic.response.TopicResponse;

import java.util.List;
import java.util.UUID;

/** The side of the people a topic is shared with: Shared with me, coming in, leaving. */
public interface SharedTopicWebService {

    /**
     * Every topic shared with the user, flat, from every owner: each granted topic with its whole
     * subtree. See {@link TopicResponse} for how the top of each shared tree is told apart.
     */
    List<TopicResponse> listShared(UUID userId);

    /**
     * Opens a public link or an invitation link and answers the first topic it opened. Refuses
     * "share.linkInvalid" for a link that does not exist or is turned off, "share.ownTopic" for
     * the caller's own link, "share.accessRevoked" for someone the owner took access from (only a
     * personal invitation lets them back), "share.inviteExpired" for an invitation already used,
     * revoked or out of date, and "share.inviteForAnotherEmail" when the caller's email is not
     * the one invited.
     */
    TopicResponse accept(UUID userId, AcceptShareRequest request);

    /** The share the topic came through is no longer new. Nothing happens on one's own topic. */
    void markSeen(UUID userId, UUID topicId);

    /**
     * Gives up the share the topic came through: its grant goes, with every topic below it.
     * "share.ownTopic" on one's own topic.
     */
    void leave(UUID userId, UUID topicId);
}
