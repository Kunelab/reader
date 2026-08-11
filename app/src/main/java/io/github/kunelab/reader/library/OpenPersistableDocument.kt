package io.github.kunelab.reader.library

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

/**
 * [ActivityResultContracts.OpenDocument] with a grant that outlives the process.
 *
 * The stock contract yields a read permission scoped to the current task, so a book
 * reopened from the library after a restart fails with a SecurityException. Asking for
 * a persistable grant lets the caller hold on to it via takePersistableUriPermission.
 */
class OpenPersistableDocument : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        return super.createIntent(context, input).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
    }
}
