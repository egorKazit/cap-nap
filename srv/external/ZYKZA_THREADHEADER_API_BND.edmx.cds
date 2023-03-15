/* checksum : 85ae1c11046580e3c25044826667f63d */
@cds.external : true
@cds.persistence.skip : true
@Capabilities.NavigationRestrictions : { RestrictedProperties: [ {
  NavigationProperty: _ThreadItemTP,
  InsertRestrictions: { Insertable: true }
} ] }
@Capabilities.SearchRestrictions : { Searchable: false }
@Capabilities.FilterRestrictions : {
  Filterable: true,
  FilterExpressionRestrictions: [ {
    Property: Thread,
    AllowedExpressions: 'SearchExpression'
  }, {
    Property: Name,
    AllowedExpressions: 'SearchExpression'
  } ]
}
@Capabilities.SortRestrictions : { NonSortableProperties: [ 'Thread', 'Name' ] }
@Capabilities.InsertRestrictions : { Insertable: false }
@Capabilities.DeleteRestrictions : { Deletable: false }
@Capabilities.UpdateRestrictions : {
  Updatable: false,
  QueryOptions: { SelectSupported: true }
}
@Core.OptimisticConcurrency : [  ]
entity com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.ZYKZA_ThreadHeader {
  @Core.Computed : true
  @Common.IsUpperCase : true
  @Common.Label : 'UUID'
  @Common.QuickInfo : '16-byte UID in 32 chars (hexadecimal)'
  key UUID : UUID not null;
  @Common.IsUpperCase : true
  @Common.Label : 'UUID'
  @Common.QuickInfo : '16-byte UID in 32 chars (hexadecimal)'
  SourceUUID : UUID;
  @Core.Computed : true
  Thread : LargeString not null;
  @Core.Computed : true
  @Common.Label : 'Cap Processor Any Name'
  Name : String(256) not null;
  @Core.Computed : true
  @Common.IsUpperCase : true
  @Common.Label : 'Thread Status'
  @Common.QuickInfo : 'Cap Processor Status'
  Status : String(1) not null;
  @Core.Computed : true
  @Common.IsUpperCase : true
  @Common.Label : 'Truth Value'
  @Common.QuickInfo : 'Truth Value: True/False'
  ProcessedFlag : Boolean not null;
  SAP__Messages : many com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.SAP__Message not null;
  @cds.ambiguous : 'missing on condition?'
  @Common.Composition : true
  _ThreadItemTP : Composition of many com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.ZYKZA_ThreadItem;
} actions {
  action Abandon() returns com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.ZYKZA_ThreadHeader not null;
  action CreateEntityFromSource(
    SourceUUID : UUID,
    Thread : LargeString not null,
    Name : String(256) not null,
    _Items : many com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.ZYKZD_ThreadItem not null
  ) returns com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.ZYKZA_ThreadHeader not null;
  action Reject() returns com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.ZYKZA_ThreadHeader not null;
  action SubmitForApproval() returns com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.ZYKZA_ThreadHeader not null;
  action Complete() returns com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.ZYKZA_ThreadHeader not null;
  action Approve() returns com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.ZYKZA_ThreadHeader not null;
};

@cds.external : true
@cds.persistence.skip : true
@Capabilities.SearchRestrictions : { Searchable: false }
@Capabilities.FilterRestrictions : {
  Filterable: true,
  FilterExpressionRestrictions: [ {
    Property: NameOrContent,
    AllowedExpressions: 'SearchExpression'
  } ]
}
@Capabilities.SortRestrictions : { NonSortableProperties: [ 'NameOrContent' ] }
@Capabilities.InsertRestrictions : { Insertable: false }
@Capabilities.DeleteRestrictions : { Deletable: false }
@Capabilities.UpdateRestrictions : {
  Updatable: false,
  QueryOptions: { SelectSupported: true }
}
@Core.OptimisticConcurrency : [  ]
entity com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.ZYKZA_ThreadItem {
  @Core.Computed : true
  @Common.IsUpperCase : true
  @Common.Label : 'UUID'
  @Common.QuickInfo : '16-byte UID in 32 chars (hexadecimal)'
  key UUID : UUID not null;
  @Core.Computed : true
  @Common.IsUpperCase : true
  @Common.Label : 'UUID'
  @Common.QuickInfo : '16-byte UID in 32 chars (hexadecimal)'
  ThreadUUID : UUID not null;
  @Core.Computed : true
  Item : Integer not null;
  @Core.Computed : true
  @Common.IsUpperCase : true
  @Common.Label : 'Item Type'
  @Common.QuickInfo : 'Cap Processor Item Type'
  Type : String(1) not null;
  @Core.Computed : true
  NameOrContent : LargeString not null;
  @cds.ambiguous : 'missing on condition?'
  _ThreadHeaderTP : Association to one com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.ZYKZA_ThreadHeader on _ThreadHeaderTP.UUID = ThreadUUID;
};

@cds.external : true
type com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.ZYKZD_ThreadItem {
  ItemUUID : UUID;
  Item : Integer not null;
  Type : String(1) not null;
  NameOrContent : LargeString not null;
};

@cds.external : true
type com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.SAP__Message {
  code : LargeString not null;
  message : LargeString not null;
  target : LargeString;
  additionalTargets : many LargeString not null;
  transition : Boolean not null;
  @odata.Type : 'Edm.Byte'
  numericSeverity : Integer not null;
  longtextUrl : LargeString;
};

@cds.external : true
@Aggregation.ApplySupported : {
  Transformations: [ 'aggregate', 'groupby', 'filter' ],
  Rollup: #None
}
@Common.ApplyMultiUnitBehaviorForSortingAndFiltering : true
@Capabilities.FilterFunctions : [ 'eq', 'ne', 'gt', 'ge', 'lt', 'le', 'and', 'or', 'contains', 'startswith', 'endswith', 'any', 'all' ]
@Capabilities.SupportedFormats : [ 'application/json', 'application/pdf' ]
@Capabilities.KeyAsSegmentSupported : true
@Capabilities.AsynchronousRequestsSupported : true
service com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001 {};

