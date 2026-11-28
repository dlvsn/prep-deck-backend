package denys.mazurenko.prepdeckbackend.dto.share.response;

import denys.mazurenko.prepdeckbackend.dto.auth.response.UserResponse;

import java.util.List;
import java.util.UUID;

/**
 * Who can see a tree and how (client type `TopicSharing`): the people with access, the
 * invitations still waiting, and the public link. {@code rootTopicId} is the top of the tree,
 * whichever of its topics the call was made on; every topic id here lies inside it.
 */
public record SharingResponse(UUID rootTopicId, UserResponse owner, List<ShareMemberResponse> people,
                              List<ShareInviteResponse> invites, ShareLinkResponse link) {
}
