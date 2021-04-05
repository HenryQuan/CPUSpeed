package com.yihengquan.cpuspeed.flutter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlin.Error

class SimpleMethodChannel(context: Context) : BaseMethodChannel(context) {
    override val name: String = "ui"
    private val channel: MethodChannel by lazy(this::setupChannel)

    private val policyLink = "https://github.com/HenryQuan/CPUSpeed/blob/master/Privacy%20Policy.md"
    private val playStoreLink =
        "https://play.google.com/store/apps/details?id=com.yihengquan.cpuspeed"
    private val githubIssueLink = "https://github.com/HenryQuan/CPUSpeed/issues/new"
    private val githubRepoLink = "https://github.com/HenryQuan/CPUSpeed"

    init {
        channel.setMethodCallHandler { call, _ ->
            when (call.method) {
                "toast" -> toast(context, call.argument<String>("message")!!)
                "feedback" -> sendFeedback(context)
                "share" -> shareApp(context)
                "openUrl" -> openLink(context, call.argument<String>("url")!!)
                else -> throw Error("Method not found")
            }
        }
    }

    private fun toast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    private fun shareApp(context: Context) {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "text/plain"
        share.putExtra(Intent.EXTRA_TEXT, playStoreLink)
        startActivity(context, Intent.createChooser(share, "Share CPUSpeed"), null)
    }

    private fun sendFeedback(context: Context) {
        openLink(context, githubIssueLink)
    }

    private fun openLink(context: Context, url: String) {
        try {
            val browser = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(context, browser, null)
        } catch (e: Exception) {
            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }
}