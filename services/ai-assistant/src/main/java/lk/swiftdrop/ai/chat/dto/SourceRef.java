package lk.swiftdrop.ai.chat.dto;

/** A help article passage used for an answer, e.g. {@code prohibited-items} / {@code Liquids}. */
public record SourceRef(String article, String section) {
}
