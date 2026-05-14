package com.passwordvault.app.service.autofill

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.widget.RemoteViews

class VaultAutofillService : AutofillService() {

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val fillContext = request.fillContexts.lastOrNull() ?: run {
            callback.onSuccess(null)
            return
        }

        val usernameView = RemoteViews(packageName, android.R.layout.simple_list_item_1)
        usernameView.setTextViewText(android.R.id.text1, "从密码库填充")

        val response = FillResponse.Builder()
            .addDataset(
                android.service.autofill.Dataset.Builder(usernameView).build()
            )
            .build()

        callback.onSuccess(response)
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }
}
