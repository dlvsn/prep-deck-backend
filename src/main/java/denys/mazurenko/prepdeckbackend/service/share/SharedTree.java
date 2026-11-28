package denys.mazurenko.prepdeckbackend.service.share;

import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;
import denys.mazurenko.prepdeckbackend.service.topic.TopicForest;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** One owner's tree that is being shared: its root and all the owner's topics around it. */
public record SharedTree(UUID ownerId, Topic root, TopicForest forest) {

    public boolean contains(UUID topicId) {
        return forest.isWithin(topicId, root.getId());
    }

    /** The ids given that lie in this tree, parents before children, siblings in their order. */
    public List<UUID> inTreeOrder(Collection<UUID> topicIds) {
        return forest.inTreeOrder(topicIds.stream().filter(this::contains).toList());
    }
}
