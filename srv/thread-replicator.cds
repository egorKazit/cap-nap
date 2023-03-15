@protocol: 'none'
service ThreadReplicationService {

    action process(threadId: UUID) returns String;

    action revert(threadId: UUID);

}
