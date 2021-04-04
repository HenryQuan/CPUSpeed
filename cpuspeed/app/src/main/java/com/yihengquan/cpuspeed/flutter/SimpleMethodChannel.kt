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
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "toast" -> toast(context, call)
                "about" -> showAbout(context)
                "feedback" -> sendFeedback(context)
                "share" -> shareApp(context)
                else -> throw Error("Method not found")
            }
        }
    }

    private fun toast(context: Context, call: MethodCall) {
        val msg = call.argument<String>("message")
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showAbout(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("CPUSpeed")
            .setMessage("It aims to help you set CPUSpeed easily for rooted android devices. Please visit my Github repository for more info.") // Specifying a listener allows you to take an action before dismissing the dialog.
            // The dialog is automatically dismissed when a dialog button is clicked.
            .setPositiveButton("Github") { _, _ -> openLink(context, githubRepoLink) }
            .setNeutralButton("Privacy Policy") { _, _ -> openLink(context, policyLink) }
            .setCancelable(true)
            .show()
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