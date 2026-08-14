package org.example.model;

public record BasicInfo(
        String completedDateTime,
        String createdDateTime,
        boolean deleted,
        String distributionType,
        int emailCoverWordCount,
        boolean external,
        boolean hidden,
        String language,
        boolean legacy,
        int listenTime,
        boolean pendingRelease,
        String pubDate,
        String pubId,
        String pubSchemaVersion,
        int pubVersion,
        int pubWordCount,
        int readingTime,
        String releasedDateTime,
        String source,
        String sourceId,
        boolean test,
        String updatedDateTime,
        boolean useNewReleasedTS
) {

    public BasicInfo withPubId(String pubId) {
        return new BasicInfo(completedDateTime, createdDateTime, deleted, distributionType, emailCoverWordCount,
                external, hidden, language, legacy, listenTime, pendingRelease, pubDate, pubId, pubSchemaVersion,
                pubVersion, pubWordCount, readingTime, releasedDateTime, source, sourceId, test, updatedDateTime,
                useNewReleasedTS);
    }
}
