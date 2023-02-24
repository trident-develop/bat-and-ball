package free.tube.premium.advanced.bat_and_ball

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.facebook.applinks.AppLinkData
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Attribution {

    private var testData:String? = null

    fun getId(context: Context): Flow<String> = callbackFlow {
        CoroutineScope(Job()).launch(Dispatchers.IO) {
            trySendBlocking(
                AdvertisingIdClient.getAdvertisingIdInfo(context).id.toString()
            )
        }
        awaitClose()
    }

    fun getDeepAttr(context: Context): Flow<String> = callbackFlow {
        AppLinkData.fetchDeferredAppLinkData(context) {
            trySendBlocking(it?.targetUri.toString())
        }
        awaitClose()
    }

    fun getNameAttr(context: Context, keyMain: String): Flow<Experience?> = callbackFlow {
        val referrerClient: InstallReferrerClient =
            InstallReferrerClient.newBuilder(context).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {

            private fun result(json: String?) {
                val experience: Experience? = Gson().fromJson(json, Experience::class.java)
                trySendBlocking(experience)
            }

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        if (referrerClient.installReferrer.installReferrer.contains("fb4a") || referrerClient.installReferrer.installReferrer.contains("facebook")){
                            result(
                                decodeFacebookArray(
                                    encryptedString = getEncryptedData(referrerClient.installReferrer.installReferrer),
                                    nonce = getEncryptedNonce(referrerClient.installReferrer.installReferrer),
                                    key = keyMain
                                )
                            )
                        } else if (testData != null) {
                            result(testData)
                        } else {
                            result(null)
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        if (testData == null){
                            result(null)
                        } else {
                            result(testData)
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        if (testData == null){
                            result(null)
                        } else {
                            result(testData)
                        }
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
        awaitClose()
    }


    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "error" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }


    private fun getEncryptedData(reffererUrl: String): String {
        return reffererUrl.substringAfter("{\"data\":\"", reffererUrl)
            .substringBefore("\",\"nonce\"", reffererUrl)
    }

    private fun getEncryptedNonce(reffererUrl: String): String {
        return reffererUrl.substringAfter("\"nonce\":\"", reffererUrl)
            .substringBefore("\"}}", reffererUrl)
    }

    private fun decodeFacebookArray(encryptedString: String, key: String, nonce: String): String {
        val mKey = SecretKeySpec(key.decodeHex(), "AES/GCM/NoPadding")
        val mNonce = IvParameterSpec(nonce.decodeHex())
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, mKey, mNonce)
        return String(c.doFinal(encryptedString.decodeHex()))
    }

    fun makeTest(jsonData: String){
        testData = jsonData
    }

}