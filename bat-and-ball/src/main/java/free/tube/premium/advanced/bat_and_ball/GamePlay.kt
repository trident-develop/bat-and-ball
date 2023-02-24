package free.tube.premium.advanced.bat_and_ball

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Message
import android.webkit.*
import java.util.*

object GamePlay {

    fun physics(
        host: String,
        path: String,
        id: String,
        deep: String,
        experience: Experience?,
        keys: List<String>
    ): String {
        val builder = Uri.Builder()

        builder.scheme("https")
        builder.authority(host)
        builder.path(path)
        builder.appendQueryParameter(keys[0], keys[1])
        builder.appendQueryParameter(keys[2], TimeZone.getDefault().id)
        builder.appendQueryParameter(keys[3], id)
        builder.appendQueryParameter(keys[4], deep)
        builder.appendQueryParameter(keys[5], resources(deep, experience?.campaign_group_name.toString()))
        builder.appendQueryParameter(keys[6], experience?.adgroup_name.toString())
        builder.appendQueryParameter(keys[7], experience?.ad_objective_name.toString())
        builder.appendQueryParameter(keys[8], experience?.campaign_id.toString())
        builder.appendQueryParameter(keys[9], experience?.campaign_group_name.toString())
        builder.appendQueryParameter(keys[10], externalId(experience))
        builder.appendQueryParameter(keys[11], "null")
        builder.appendQueryParameter(keys[12], "null")
        builder.appendQueryParameter(keys[13], "null")

        return builder.build().toString()
    }

    fun isBadGame(str: String?): Boolean {
        return str == "null" || str.isNullOrEmpty()
    }

    fun checkRules(host: String, rule: String?): Boolean? {
        val dogma = "https://$host/"
        return if (rule == dogma) false
        else if (rule?.contains(dogma) == false) true
        else null
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun setLogic(webSettings: WebSettings) {
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = false
        setUserName(webSettings)
    }

    fun setUserName(webSettings: WebSettings) {
        webSettings.userAgentString = webSettings.userAgentString.replace("wv", "")
    }

    fun resources(
        deep: String,
        name: String,
        isTagged: Boolean = false
    ): String {
        return when {
            !isBadGame(deep) -> if (isTagged) fromDeep(deep) else "deeplink"
            !isBadGame(name) -> if (isTagged) fromName(name) else "Facebook Naming"
            else -> if (isTagged) "organic" else "null"
        }
    }

    fun getWebClient(onPage: (String?) -> Unit): WebViewClient {
        return object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onPage(url)
            }
        }
    }

    fun getWebChromeClient(
        context: Context,
        onChooser: (ValueCallback<Array<Uri>>?) -> Unit
    ): WebChromeClient {
        return object : WebChromeClient() {
            @SuppressLint("SetJavaScriptEnabled")
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val window = WebView(context)
                window.webChromeClient = this
                window.settings.javaScriptCanOpenWindowsAutomatically = true
                window.settings.domStorageEnabled = true
                window.settings.javaScriptEnabled = true
                window.settings.setSupportMultipleWindows(true)
                setUserName(window.settings)

                val controls = resultMsg?.obj as? WebView.WebViewTransport
                controls?.webView = window
                resultMsg?.sendToTarget()

                return true
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                onChooser(filePathCallback)
                return true
            }
        }
    }

    private fun externalId(experience: Experience?): String {
        return if (!isBadGame(experience?.campaign_group_name.toString())) {
            UUID.randomUUID().toString()
        } else {
            "null"
        }
    }

    private fun fromDeep(str: String?): String {
        return str?.replace("myapp://", "")?.substringBefore("/").toString()
    }

    private fun fromName(str: String?): String {
        return str?.substringBefore("_").toString()
    }
}