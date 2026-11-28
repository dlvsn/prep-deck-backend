package denys.mazurenko.prepdeckbackend.service.share;

import denys.mazurenko.prepdeckbackend.dto.share.request.InviteRequest;
import denys.mazurenko.prepdeckbackend.dto.share.request.ShareTopicsRequest;
import denys.mazurenko.prepdeckbackend.dto.share.request.UpdateLinkRequest;
import denys.mazurenko.prepdeckbackend.dto.share.response.SharingResponse;

import java.util.Locale;
import java.util.UUID;

/**
 * The owner's side of sharing: who can see a tree, and the ways in. Every call names any topic of
 * the tree and needs the right to share it (the owner's alone today); every call answers the tree's
 * sharing as it stands afterwards. Topic ids given must lie in that tree ("share.topicNotInTree")
 * and may not be empty ("share.pickOne").
 */
public interface SharingWebService {

    SharingResponse get(UUID userId, UUID topicId);

    /**
     * Saves an invitation and emails it (in the invitee's language when they have an account,
     * else the request's). Replaces a pending invitation to the same address in this tree.
     * Refuses "share.noSuchUser" for an unknown username, "share.invalidEmail",
     * "share.selfInvite", "share.alreadyHasAccess" (change their topics instead) and
     * "share.inviteTooSoon" with the seconds to wait.
     */
    SharingResponse invite(UUID userId, UUID topicId, InviteRequest request, Locale locale);

    /** The letter's link stops working. "share.inviteNotFound" when it is no longer pending. */
    SharingResponse revokeInvite(UUID userId, UUID topicId, UUID inviteId);

    /** Replaces the topics a person may see in this tree. "share.notMember" when they have none. */
    SharingResponse changeTopics(UUID userId, UUID topicId, UUID memberId, ShareTopicsRequest request);

    /**
     * Takes every topic of this tree from the person at once, and stops them coming back through
     * a public link until a personal invitation of the owner's is accepted.
     */
    SharingResponse removeMember(UUID userId, UUID topicId, UUID memberId);

    /** Makes the tree's link on first use; turns it on or off, and sets what it opens. */
    SharingResponse updateLink(UUID userId, UUID topicId, UpdateLinkRequest request);

    /**
     * A new token: the old link stops working at once, people who came through it keep their
     * access. "share.noLink" when the tree has no link yet.
     */
    SharingResponse resetLink(UUID userId, UUID topicId);

    /** {@link #removeMember} for everyone who came through the public link alone. */
    SharingResponse removeLinkMembers(UUID userId, UUID topicId);
}
