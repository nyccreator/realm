package com.realm.event;

import com.realm.model.NoteLink;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new relationship is created between notes
 */
@Getter
public class RelationshipCreatedEvent extends ApplicationEvent {
    
    private final NoteLink relationship;
    
    public RelationshipCreatedEvent(NoteLink relationship) {
        super(relationship);
        this.relationship = relationship;
    }
    
    @Override
    public String toString() {
        return String.format("RelationshipCreatedEvent{relationshipId=%d, type=%s, timestamp=%s}", 
            relationship.getId(), 
            relationship.getType(), 
            getTimestamp());
    }
}