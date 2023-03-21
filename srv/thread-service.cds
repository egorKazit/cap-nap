using {db} from '../db/nap-vdm';

service ThreadService  {

    entity Thread     as projection on db.Thread order by
        thread desc
    actions {

        action copy() returns Thread;

        @(
            cds.odata.bindingparameter.name: '_it',
            Common.SideEffects             : {TargetProperties: ['_it/status']}
        )
        action publish();

        @protocol: 'none'
        action promoteStatus(status : String);

        @(
            cds.odata.bindingparameter.name: '_it',
            Common.SideEffects             : {TargetProperties: ['_it/status']}
        )
        action withdraw();

        @(
            cds.odata.bindingparameter.name: '_it',
            Common.SideEffects             : {TargetProperties: ['_it/status']}
        )
        action complete();
    };

    entity Note       as projection on db.Note order by
        note asc;

    entity Attachment as projection on db.Attachment;

}
