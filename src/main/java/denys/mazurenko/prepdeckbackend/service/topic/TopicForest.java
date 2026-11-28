package denys.mazurenko.prepdeckbackend.service.topic;

import denys.mazurenko.prepdeckbackend.persistence.topic.entity.Topic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * One owner's topics as trees in memory, built from the flat list the database gives. Topics are
 * read whole per owner anyway, and walking a few hundred rows here beats a query per step.
 */
public final class TopicForest {

    private static final Comparator<Topic> BY_POSITION = Comparator.comparingInt(Topic::getPosition);

    private final Map<UUID, Topic> byId = new HashMap<>();
    private final Map<UUID, List<Topic>> children = new HashMap<>();

    public TopicForest(Collection<Topic> topics) {
        for (Topic topic : topics) {
            byId.put(topic.getId(), topic);
        }
        for (Topic topic : topics) {
            if (topic.getParentId() != null) {
                children.computeIfAbsent(topic.getParentId(), parent -> new ArrayList<>()).add(topic);
            }
        }
        children.values().forEach(list -> list.sort(BY_POSITION));
    }

    public Optional<Topic> find(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    public boolean contains(UUID id) {
        return byId.containsKey(id);
    }

    /** The top of the tree the topic sits in (the topic itself for a root). */
    public Topic root(UUID id) {
        Topic cursor = require(id);
        while (cursor.getParentId() != null && byId.containsKey(cursor.getParentId())) {
            cursor = byId.get(cursor.getParentId());
        }
        return cursor;
    }

    /** The topic and everything below it, parents before children, siblings in their order. */
    public List<Topic> subtree(UUID id) {
        List<Topic> result = new ArrayList<>();
        Deque<Topic> stack = new ArrayDeque<>();
        stack.push(require(id));
        while (!stack.isEmpty()) {
            Topic topic = stack.pop();
            result.add(topic);
            List<Topic> below = children.getOrDefault(topic.getId(), List.of());
            for (int i = below.size() - 1; i >= 0; i--) {
                stack.push(below.get(i));
            }
        }
        return result;
    }

    /** The topics above this one, from the root down to its parent. */
    public List<Topic> ancestors(UUID id) {
        List<Topic> chain = new ArrayList<>();
        Topic cursor = require(id);
        while (cursor.getParentId() != null && byId.containsKey(cursor.getParentId())) {
            cursor = byId.get(cursor.getParentId());
            chain.addFirst(cursor);
        }
        return chain;
    }

    /** Whether the topic is {@code ancestorId} itself or lies below it. */
    public boolean isWithin(UUID id, UUID ancestorId) {
        Topic cursor = byId.get(id);
        while (cursor != null) {
            if (cursor.getId().equals(ancestorId)) {
                return true;
            }
            cursor = cursor.getParentId() == null ? null : byId.get(cursor.getParentId());
        }
        return false;
    }

    /**
     * The ids given, as the trees show them: roots in their order, each followed by its subtree,
     * parents before children, siblings in their order. Ids not in this forest are left out.
     */
    public List<UUID> inTreeOrder(Collection<UUID> ids) {
        Map<UUID, Integer> order = new HashMap<>();
        List<Topic> roots = byId.values().stream()
                .filter(topic -> topic.getParentId() == null || !byId.containsKey(topic.getParentId()))
                .sorted(BY_POSITION)
                .toList();
        for (Topic root : roots) {
            for (Topic topic : subtree(root.getId())) {
                order.put(topic.getId(), order.size());
            }
        }
        return ids.stream()
                .filter(order::containsKey)
                .distinct()
                .sorted(Comparator.comparingInt(order::get))
                .toList();
    }

    public boolean hasChildren(UUID id) {
        return !children.getOrDefault(id, List.of()).isEmpty();
    }

    /**
     * The ids that no other id of the set lies above: a topic already covers its subtree, so a
     * subtopic picked along with it adds nothing. Order of the input is kept.
     */
    public Set<UUID> outermost(Collection<UUID> ids) {
        Set<UUID> all = new LinkedHashSet<>(ids);
        Set<UUID> result = new LinkedHashSet<>();
        for (UUID id : all) {
            boolean covered = ancestors(id).stream().anyMatch(above -> all.contains(above.getId()));
            if (!covered) {
                result.add(id);
            }
        }
        return result;
    }

    private Topic require(UUID id) {
        Topic topic = byId.get(id);
        if (topic == null) {
            throw new IllegalArgumentException("Topic " + id + " is not in this forest");
        }
        return topic;
    }
}
