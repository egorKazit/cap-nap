using ThreadService as service from '../../srv/thread-service';
using from './annotations-thread';

annotate ThreadService.Note with {
    ID                @UI.Hidden;
    text              @UI.MultiLineText;
    thread            @UI.Hidden;
    replicationStatus @UI.Hidden;
}

annotate ThreadService.Note with @UI: {

    HeaderInfo          : {
        TypeName      : 'Note',
        TypeNamePlural: 'New',
        Title         : {Value: note},
        Description   : {Value: replicationStatus},
    },

    LineItem            : [

        {
            ![@UI.Hidden],
            Value: ID
        },


        {
            ![@UI.Hidden],
            Value: thread_ID
        },

        {
            ![@UI.Hidden],
            Value: text
        },

        {
            $Type: 'UI.DataField',
            Label: 'Note',
            Value: note
        },

        {
            $Type: 'UI.DataField',
            Label: 'Replication Status',
            Value: replicationStatus
        },

    ],

    Facets              : [{
        $Type : 'UI.ReferenceFacet',
        Label : 'Note Text',
        Target: '@UI.FieldGroup#MainInfo',
    },

    ],

    FieldGroup #MainInfo: {Data: [{
        $Type: 'UI.DataField',
        Label: 'Text',
        Value: text
    }, ],

    }
};

annotate service.Thread with @(UI.Facets: [
    {
        $Type : 'UI.ReferenceFacet',
        ID    : 'MainInfoId',
        Label : 'Main Info',
        Target: '@UI.FieldGroup#MainInfo',
    },
    {
        $Type : 'UI.ReferenceFacet',
        ID    : 'NoteId',
        Label : 'Note',
        Target: 'note/@UI.LineItem',
    },
]);

annotate service.Attachment with @(UI.Facets: []);
