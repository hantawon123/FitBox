package com.ssafy.fitbox.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.ssafy.fitbox.databinding.FragmentAddressSearchBinding

class AddressSearchFragment : Fragment() {
    private var _binding: FragmentAddressSearchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddressSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.webViewAddressSearch.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            addJavascriptInterface(AddressBridge(), "FitBoxAddress")
            loadDataWithBaseURL(
                "https://postcode.map.daum.net/",
                POSTCODE_HTML,
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    private inner class AddressBridge {
        @JavascriptInterface
        fun selectAddress(zoneCode: String, roadAddress: String, jibunAddress: String) {
            val address = roadAddress.ifBlank { jibunAddress }
            val activity = activity ?: return
            activity.runOnUiThread {
                if (!isAdded || _binding == null) return@runOnUiThread
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(
                        KEY_ZONE_CODE to zoneCode,
                        KEY_ADDRESS to address
                    )
                )
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        binding.webViewAddressSearch.apply {
            removeJavascriptInterface("FitBoxAddress")
            destroy()
        }
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_KEY = "address_search_result"
        const val KEY_ZONE_CODE = "zone_code"
        const val KEY_ADDRESS = "address"

        private val POSTCODE_HTML = """
            <!doctype html>
            <html lang="ko">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <script src="https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>
              <style>html,body,#wrap{width:100%;height:100%;margin:0;padding:0;}</style>
            </head>
            <body>
              <div id="wrap"></div>
              <script>
                new daum.Postcode({
                  oncomplete: function(data) {
                    FitBoxAddress.selectAddress(
                      data.zonecode || '',
                      data.roadAddress || '',
                      data.jibunAddress || ''
                    );
                  },
                  width: '100%',
                  height: '100%'
                }).embed(document.getElementById('wrap'));
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
