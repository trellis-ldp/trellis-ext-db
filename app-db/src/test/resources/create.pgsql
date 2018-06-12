CREATE TABLE IF NOT EXISTS metadata (
    id VARCHAR(1024) NOT NULL PRIMARY KEY,
    interactionModel VARCHAR(255) NOT NULL,
    modified BIGINT NOT NULL,
    isPartOf VARCHAR(1024),
    isDeleted BOOLEAN default FALSE,
    hasAcl BOOLEAN default FALSE
);

CREATE TABLE IF NOT EXISTS resource (
    id VARCHAR(1024) NOT NULL,
    subject VARCHAR(1024) NOT NULL,
    predicate VARCHAR(1024) NOT NULL,
    object TEXT NOT NULL,
    lang VARCHAR(10),
    datatype VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS resource_idx ON resource (id);

CREATE TABLE IF NOT EXISTS log (
    id VARCHAR(1024) NOT NULL,
    subject VARCHAR(1024) NOT NULL,
    predicate VARCHAR(1024) NOT NULL,
    object TEXT NOT NULL,
    lang VARCHAR(10),
    datatype VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS log_idx ON log (id);

CREATE TABLE IF NOT EXISTS acl (
    id VARCHAR(1024) NOT NULL,
    subject VARCHAR(1024) NOT NULL,
    predicate VARCHAR(1024) NOT NULL,
    object TEXT NOT NULL,
    lang VARCHAR(10),
    datatype VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS acl_idx ON acl (id);

CREATE TABLE IF NOT EXISTS ldp (
    id VARCHAR(1024) NOT NULL PRIMARY KEY,
    member VARCHAR(1024) NOT NULL,
    membershipResource VARCHAR(1024) NOT NULL,
    hasMemberRelation VARCHAR(255),
    isMemberOfRelation VARCHAR(255),
    insertedContentRelation VARCHAR(255) default 'http://www.w3.org/ns/ldp#MemberSubject'
);

CREATE INDEX IF NOT EXISTS ldp_idx ON ldp (member);

CREATE TABLE IF NOT EXISTS extra (
    subject VARCHAR(1024) NOT NULL,
    predicate VARCHAR(1024) NOT NULL,
    object VARCHAR(1024) NOT NULL
);

CREATE INDEX IF NOT EXISTS extra_idx ON extra (subject);

CREATE TABLE IF NOT EXISTS nonrdf (
    id VARCHAR(1024) NOT NULL PRIMARY KEY,
    location VARCHAR(1024) NOT NULL,
    modified BIGINT NOT NULL,
    format VARCHAR(255),
    size BIGINT
);

