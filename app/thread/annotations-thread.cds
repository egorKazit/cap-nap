using ThreadService as service from '../../srv/thread-service';

annotate ThreadService.Thread with @odata.draft.enabled;
annotate ThreadService.Thread with @fiori.draft.enabled;

annotate ThreadService.Thread with @title: 'Thread' {
    thread  @title                       : 'Thread ID'  @(Common: {ValueList: {
        Label         : 'Thread ID',
        CollectionPath: 'Thread',
        Parameters    : [
            {
                $Type            : 'Common.ValueListParameterInOut',
                ValueListProperty: 'thread',
                LocalDataProperty: thread
            },
            {
                $Type            : 'Common.ValueListParameterOut',
                ValueListProperty: 'name',
                LocalDataProperty: name,
            },
        ]
    }});
    name    @title                       : 'Name';
}

annotate ThreadService.Thread with @UI: {

    SelectionFields                                : [
        thread,
        name,
    ],

    LineItem                                       : [

        {
            ![@UI.Hidden],
            Value: ID
        },

        {
            $Type: 'UI.DataField',
            Value: thread
        },

        {
            $Type: 'UI.DataField',
            Value: name
        },

        {
            $Type: 'UI.DataField',
            Value: status
        },
    ],

    HeaderInfo                                     : {
        TypeName      : 'Thread',
        TypeNamePlural: 'New',
        Title         : {Value: thread},
        Description   : {Value: status},
    },

    Facets                                         : [
        {
            $Type : 'UI.ReferenceFacet',
            Label : 'Main Info',
            Target: '@UI.FieldGroup#MainInfo'
        },

        {
            $Type : 'UI.ReferenceFacet',
            Label : 'Note',
            Target: 'note/@UI.LineItem'
        },

    ],

    FieldGroup #MainInfo                           : {
        Data         : [{
            $Type                  : 'UI.DataField',
            Value                  : name,
            ![@Common.FieldControl]: {$edmJson: {$If: [
                {$Eq: [
                    {$Path: 'HasActiveEntity'},
                    false
                ]},
                3,
                1
            ]}}
        },

        ],

        ![@UI.Hidden]: {$edmJson: {$Eq: [
            {$Path: 'HasActiveEntity'},
            false
        ]}}

    },


    PresentationVariant #DefaultPresentationVariant: {
        Text          : 'Default',
        SortOrder     : [{
            $Type     : 'Common.SortOrderType',
            Property  : note.note,
            Descending: true
        }],
        Visualizations: ['@UI.LineItem#Default']
    },

    Identification                                 : [
        {
            $Type        : 'UI.DataFieldForAction',
            Label        : 'Publish',
            Action       : 'ThreadService.publish',
            ![@UI.Hidden]: {$edmJson: {$Or: [
                {$Ne: [
                    {$Path: 'status'},
                    'Initial'
                ]},
                {$Ne: [
                    {$Path: 'IsActiveEntity'},
                    true
                ]},
            ]}}
        },
        {
            $Type        : 'UI.DataFieldForAction',
            Label        : 'Withdraw',
            Action       : 'ThreadService.withdraw',
            ![@UI.Hidden]: {$edmJson: {$Or: [
                {$Ne: [
                    {$Path: 'status'},
                    'Published'
                ]},
                {$Ne: [
                    {$Path: 'IsActiveEntity'},
                    true
                ]},
            ]}}
        },
        {
            $Type        : 'UI.DataFieldForAction',
            Label        : 'Complete',
            Action       : 'ThreadService.complete',
            ![@UI.Hidden]: {$edmJson: {$Or: [
                {$Ne: [
                    {$Path: 'status'},
                    'Published'
                ]},
                {$Ne: [
                    {$Path: 'IsActiveEntity'},
                    true
                ]},
            ]}}
        }
    ]

};

annotate ThreadService.Thread with @(

    UI.UpdateHidden: {$edmJson: {$Not: {$Eq: [
        {$Path: 'status'},
        'Initial'
    ]}}},


    UI.DeleteHidden: {$edmJson: {$Not: {$Eq: [
        {$Path: 'status'},
        'Initial'
    ]}}}
);

annotate ThreadService.Thread with @(Capabilities: {
    UpdateRestrictions: {
        $Type    : 'Capabilities.UpdateRestrictionsType',
        Updatable: {$edmJson: {$Eq: [
            {$Path: 'status'},
            'Initial'
        ]}}
    },
    DeleteRestrictions: {
        $Type    : 'Capabilities.DeleteRestrictionsType',
        Deletable: {$edmJson: {$Eq: [
            {$Path: 'status'},
            'Initial'
        ]}}
    },
});
