namespace db;


using {
    cuid,
    managed
} from '@sap/cds/common';

entity Thread : cuid, managed {
    thread     : String  @assert.notNull  @readonly;
    name       : String  @assert.notNull;
    status     : String  @assert.range enum {
        Initial;
        Publishing;
        Published;
        Completed;
    } default 'Initial';
    note       : Composition of many Note
                     on note.thread = $self;
    attachment : Composition of many Attachment
                     on attachment.thread = $self;
}

entity Note : cuid, managed {
    note              : String @readonly;
    text              : String;
    replicationStatus : String @assert.range enum {
        Initial;
        UnderReplication;
        Replicated;
        Failed;
        UnderDereplication;
    } default 'Initial';
    thread            : Association to one Thread not null;
}

entity Attachment : cuid, managed {

    content           : LargeBinary @Core.MediaType  : mediaType;
    mediaType         : String      @Core.IsMediaType: true;
    fileName          : String;
    size              : Integer;
    url               : String;
    internalQualifier : String;
    thread            : Association to one Thread not null;
}
