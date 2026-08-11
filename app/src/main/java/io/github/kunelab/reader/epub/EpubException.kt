package io.github.kunelab.reader.epub

/**
 * A structural problem with an EPUB file.
 *
 * Deliberately carries no user-facing text: the parser is plain Kotlin with no access
 * to Android resources, so callers decide how to phrase these for the user.
 */
sealed class EpubException(message: String) : Exception(message) {

    /** The archive is not an EPUB, or its structure could not be followed. */
    class NotAnEpub(detail: String) : EpubException("Not a valid EPUB: $detail")

    /** The EPUB declares content that is not present in the archive. */
    class MissingContent(detail: String) : EpubException("EPUB is missing content: $detail")
}
