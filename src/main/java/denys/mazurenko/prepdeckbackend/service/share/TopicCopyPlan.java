package denys.mazurenko.prepdeckbackend.service.share;

import denys.mazurenko.prepdeckbackend.persistence.image.entity.Image;
import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What a copy will be made of, read in one transaction: the topics of the subtree (parents
 * first), their cards oldest first, and every picture they show. {@code parentId} is where the
 * copy goes among the caller's topics (null: a root).
 */
record TopicCopyPlan(UUID parentId, List<Topic> topics, Map<UUID, List<CardCopy>> cardsByTopic, List<Image> images) {
}
