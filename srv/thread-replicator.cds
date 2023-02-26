@protocol: 'none'
service ThreadReplicationService {

    action process(threadId: UUID);

    action revert(threadId: UUID);

}
