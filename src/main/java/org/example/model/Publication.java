package org.example.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record Publication(
        BasicInfo basicInfo,
        List<JsonNode> multimedia,
        List<PubSeriesInfo> pubSeriesInfo,
        PubUrlInfo pubUrlInfo,
        List<Summary> summaries,
        List<Tag> tags,
        List<Title> titles
) {

    public Publication withBasicInfo(BasicInfo basicInfo) {
        return new Publication(basicInfo, multimedia, pubSeriesInfo, pubUrlInfo, summaries, tags, titles);
    }

    public Publication withTitles(List<Title> titles) {
        return new Publication(basicInfo, multimedia, pubSeriesInfo, pubUrlInfo, summaries, tags, titles);
    }
}
