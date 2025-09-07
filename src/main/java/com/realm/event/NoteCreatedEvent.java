package com.realm.event;

import com.realm.model.Note;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a new note is created
 */
@Getter
public class NoteCreatedEvent extends ApplicationEvent {
    
    private final Note note;
    
    public NoteCreatedEvent(Note note) {
        super(note);
        this.note = note;
    }
    
    @Override
    public String toString() {
        return String.format("NoteCreatedEvent{noteId=%d, userId=%d, timestamp=%s}", 
            note.getId(), 
            note.getCreatedBy().getId(), 
            getTimestamp());
    }
}