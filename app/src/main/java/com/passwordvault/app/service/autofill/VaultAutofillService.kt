package com.passwordvault.app.service.autofill

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.passwordvault.app.data.repository.AccountRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VaultAutofillService : AutofillService() {

    @Inject lateinit var accountRepository: AccountRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal, callback: FillCallback) {
        val fillContext = request.fillContexts.lastOrNull() ?: run {
            callback.onSuccess(null)
            return
        }

        val structure = fillContext.structure
        val activityComponent = structure.activityComponent ?: run {
            callback.onSuccess(null)
            return
        }

        val callingPackage = activityComponent.packageName
        if (callingPackage == this.packageName) {
            callback.onSuccess(null)
            return
        }

        val usernameIds = mutableListOf<android.view.autofill.AutofillId>()
        val passwordIds = mutableListOf<android.view.autofill.AutofillId>()

        for (i in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            traverseNodes(windowNode.rootViewNode) { node ->
                val id = node.autofillId ?: return@traverseNodes
                val hints = node.autofillHints?.joinToString(" ") { it.lowercase() } ?: ""
                val entry = id.toString().lowercase()

                if (hints.contains("password") || entry.contains("password")) {
                    passwordIds.add(id)
                }
                if (hints.contains("username") || hints.contains("email") || hints.contains("login") ||
                    hints.contains("account") || entry.contains("username") || entry.contains("email")) {
                    usernameIds.add(id)
                }
            }
        }

        if (passwordIds.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        scope.launch {
            try {
                val matched = accountRepository.findAccountsByUrl(callingPackage)
                val candidates = if (matched.isNotEmpty()) matched else accountRepository.getAllAccountsSync()

                if (candidates.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val builder = FillResponse.Builder()

                for (account in candidates.take(5)) {
                    val view = RemoteViews(packageName, android.R.layout.simple_list_item_2)
                    view.setTextViewText(android.R.id.text1, account.username)
                    view.setTextViewText(android.R.id.text2, account.title)

                    val dataset = Dataset.Builder()
                    if (usernameIds.isNotEmpty()) {
                        dataset.setValue(usernameIds[0], AutofillValue.forText(account.username))
                    }
                    dataset.setValue(passwordIds[0], AutofillValue.forText(account.password))
                    builder.addDataset(dataset.build())
                }

                val allIds = usernameIds + passwordIds
                if (allIds.isNotEmpty()) {
                    builder.setSaveInfo(
                        SaveInfo.Builder(
                            SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                            allIds.toTypedArray()
                        ).build()
                    )
                }

                callback.onSuccess(builder.build())
            } catch (e: Exception) {
                callback.onSuccess(null)
            }
        }
    }

    private fun traverseNodes(node: AssistStructure.ViewNode, action: (AssistStructure.ViewNode) -> Unit) {
        action(node)
        for (i in 0 until node.childCount) {
            traverseNodes(node.getChildAt(i), action)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
