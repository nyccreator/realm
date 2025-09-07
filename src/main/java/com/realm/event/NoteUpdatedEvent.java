package com.realm.event;

import com.realm.model.Note;
import com.realm.model.NoteVersion;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a note is updated
 */
@Getter
public class NoteUpdatedEvent extends ApplicationEvent {
    
    private final Note updatedNote;
    private final NoteVersion previousVersion;
    
    public NoteUpdatedEvent(Note updatedNote, NoteVersion previousVersion) {
        super(updatedNote);
        this.updatedNote = updatedNote;
        this.previousVersion = previousVersion;
    }
    
    @Override
    public String toString() {
        return String.format("NoteUpdatedEvent{noteId=%d, userId=%d, timestamp=%s}", 
            updatedNote.getId(), 
            updatedNote.getCreatedBy().getId(), 
            getTimestamp());
    }
}