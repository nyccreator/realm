package com.realm.event;

import com.realm.model.NoteLink;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a relationship is removed between notes
 */
@Getter
public class RelationshipRemovedEvent extends ApplicationEvent {
    
    private final NoteLink relationship;
    
    public RelationshipRemovedEvent(NoteLink relationship) {
        super(relationship);
        this.relationship = relationship;
    }
    
    @Override
    public String toString() {
        return String.format("RelationshipRemovedEvent{relationshipId=%d, type=%s, timestamp=%s}", 
            relationship.getId(), 
            relationship.getType(), 
            getTimestamp());
    }
}