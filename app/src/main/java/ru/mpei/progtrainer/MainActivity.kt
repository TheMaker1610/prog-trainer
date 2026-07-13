package ru.mpei.progtrainer

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#0e0f16"))
            overScrollMode = WebView.OVER_SCROLL_NEVER
        }
        setContentView(web)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true        // ← localStorage: статистика сохраняется между запусками
            allowFileAccess = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            textZoom = 100                  // игнорируем системный масштаб шрифта — вёрстка не поедет
            loadWithOverviewMode = false
            useWideViewPort = false
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
        }

        // тёмная тема внутри WebView (Android 13+), чтобы не мигало белым
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            web.settings.isAlgorithmicDarkeningAllowed = false // страница уже тёмная, затемнять не надо
        }

        web.webViewClient = WebViewClient()      // все ссылки открываются внутри
        web.webChromeClient = WebChromeClient()  // нужен для window.confirm() при выходе из теста

        if (savedInstanceState == null) {
            web.loadUrl("file:///android_asset/index.html")
        } else {
            web.restoreState(savedInstanceState)
        }

        // Аппаратная «Назад»: сначала внутренняя навигация приложения, и только потом выход
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript(BACK_JS) { result ->
                    // "exit" — мы уже на главном экране, выходим из приложения
                    if (result != null && result.contains("exit")) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onPause() {
        super.onPause()
        web.onPause()
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }

    private companion object {
        /**
         * Возвращает "exit", если открыт главный экран (тогда закрываем приложение).
         * Из теста/теории/результатов — уводим на главный экран, приложение не закрывается.
         */
        const val BACK_JS = """
            (function () {
              try {
                var home = document.getElementById('home');
                if (home && !home.classList.contains('hidden')) return 'exit';
                if (typeof refreshHome === 'function') refreshHome();
                if (typeof show === 'function') show('home');
                return 'handled';
              } catch (e) { return 'exit'; }
            })();
        """
    }
}
