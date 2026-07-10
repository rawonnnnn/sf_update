package com.sofaflix.kmp

import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
actual fun WatchPlayer(
    movie: Movie,
    episode: Episode,
    hasNext: Boolean,
    onPlayNextEpisode: () -> Unit,
    onUpdateProgress: (Double, Double) -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: (Boolean) -> Unit,
    modifier: Modifier
) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val savedProgressSec = remember(movie.slug, episode.name) {
        StorageHelpers.getEpisodeProgress(movie.slug, episode.name)
    }

    val htmlContent = remember(movie, episode, hasNext, savedProgressSec) {
        val embed = episode.embedUrl
        val stream = episode.streamUrl
        if (stream.isNotBlank()) {
            val absoluteStreamUrl = if (stream.startsWith("http")) stream else "${getBaseUrl()}${if (stream.startsWith("/")) "" else "/"}$stream"
            val subsJson = getSubtitlesJson(episode)
            generateArtplayerHtml(
                m3u8Url = absoluteStreamUrl,
                title = "${movie.name} - ${episode.name}",
                hasNext = hasNext,
                poster = movie.posterUrl.ifBlank { movie.thumbUrl },
                subtitleTracksJson = subsJson,
                savedTime = savedProgressSec
            )
        } else {
            ""
        }
    }

    var isCustomViewActive by remember { mutableStateOf(false) }
    var customViewRef by remember { mutableStateOf<View?>(null) }
    var customCallbackRef by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var exitFullscreenLambda by remember { mutableStateOf<(() -> Unit)?>(null) }

    if (isCustomViewActive) {
        BackHandler {
            exitFullscreenLambda?.invoke()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            object : WebView(context) {
                override fun dispatchKeyEvent(event: android.view.KeyEvent?): Boolean {
                    if (event?.keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                        if (event.action == android.view.KeyEvent.ACTION_UP) {
                            val activity = context.findActivity() as? androidx.activity.ComponentActivity
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        }
                        return true
                    }
                    return super.dispatchKeyEvent(event)
                }
            }.apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                settings.javaScriptEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                webViewClient = WebViewClient()
                val customChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        val msg = consoleMessage?.message() ?: ""
                        android.util.Log.d("WebViewConsole", msg)
                        return true
                    }

                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                        if (customViewRef != null) {
                            callback?.onCustomViewHidden()
                            return
                        }
                        customViewRef = view
                        customCallbackRef = callback
                        isCustomViewActive = true

                        val activity = context.findActivity() ?: return
                        val root = activity.window.decorView as FrameLayout
                        
                        view?.setBackgroundColor(android.graphics.Color.BLACK)
                        root.addView(view, FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        ))
                        
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

                        // Hide system UI bars
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            activity.window.setDecorFitsSystemWindows(false)
                            activity.window.insetsController?.let { controller ->
                                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            activity.window.decorView.systemUiVisibility = (
                                View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            )
                        }
                    }

                    override fun onHideCustomView() {
                        val cv = customViewRef ?: return
                        val cb = customCallbackRef
                        
                        val activity = context.findActivity() ?: return
                        val root = activity.window.decorView as FrameLayout
                        
                        root.removeView(cv)
                        customViewRef = null
                        customCallbackRef = null
                        isCustomViewActive = false
                        cb?.onCustomViewHidden()
                        
                        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                        // Show system UI bars
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            activity.window.setDecorFitsSystemWindows(true)
                            activity.window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        } else {
                            @Suppress("DEPRECATION")
                            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                        }
                    }
                }
                webChromeClient = customChromeClient
                exitFullscreenLambda = {
                    customChromeClient.onHideCustomView()
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun postMessage(message: String) {
                        post {
                            try {
                                val json = Json.parseToJsonElement(message)
                                val obj = json as? kotlinx.serialization.json.JsonObject
                                val action = obj?.get("action")?.let {
                                    (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                                }
                                if (action == "playNextEpisode") {
                                    onPlayNextEpisode()
                                } else if (action == "updateProgress") {
                                    val data = obj["data"] as? kotlinx.serialization.json.JsonObject
                                    val currentTime = data?.get("currentTime")?.let {
                                        (it as? kotlinx.serialization.json.JsonPrimitive)?.doubleOrNull
                                    } ?: 0.0
                                    val duration = data?.get("duration")?.let {
                                        (it as? kotlinx.serialization.json.JsonPrimitive)?.doubleOrNull
                                    } ?: 0.0
                                    onUpdateProgress(currentTime, duration)
                                } else if (action == "toggleFullscreen") {
                                    val isFS = message.contains("\"isFullscreen\":true")
                                    onToggleFullscreen(isFS)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }, "AndroidApp")

                val stream = episode.streamUrl
                val embed = episode.embedUrl
                val targetUrl = if (stream.isNotBlank()) stream else embed
                tag = targetUrl
                if (stream.isNotBlank()) {
                    loadDataWithBaseURL(getBaseUrl(), htmlContent, "text/html", "utf-8", null)
                } else if (embed.isNotBlank()) {
                    val absoluteEmbedUrl = if (embed.startsWith("http")) embed else "${getBaseUrl()}${if (embed.startsWith("/")) "" else "/"}$embed"
                    loadUrl(absoluteEmbedUrl)
                }
            }
        },
        update = { webView ->
            val stream = episode.streamUrl
            val embed = episode.embedUrl
            val targetUrl = if (stream.isNotBlank()) stream else embed
            val loadedUrl = webView.tag as? String
            if (loadedUrl != targetUrl) {
                webView.tag = targetUrl
                if (stream.isNotBlank()) {
                    webView.loadDataWithBaseURL(getBaseUrl(), htmlContent, "text/html", "utf-8", null)
                } else if (embed.isNotBlank()) {
                    val absoluteEmbedUrl = if (embed.startsWith("http")) embed else "${getBaseUrl()}${if (embed.startsWith("/")) "" else "/"}$embed"
                    if (webView.url != absoluteEmbedUrl) {
                        webView.loadUrl(absoluteEmbedUrl)
                    }
                }
            }
            webView.evaluateJavascript("if (window.art) { window.art.fullscreenWeb = $isFullscreen; }", null)
        }
    )
}

private fun getBaseUrl(): String {
    return AppPreferences.getString("sf:api_domain", "https://sofaflix.baby")
}

private fun getAbsoluteUrl(url: String): String {
    if (url.startsWith("http://") || url.startsWith("https://")) {
        return url
    }
    return getBaseUrl() + if (url.startsWith("/")) url else "/$url"
}

private fun getSubtitlesJson(episode: Episode): String {
    val list = mutableListOf<String>()
    episode.subtitles.forEachIndexed { index, sub ->
        if (sub.url.isNotBlank()) {
            val separator = if (sub.url.contains("?")) "&" else "?"
            val taggedUrl = getAbsoluteUrl(sub.url) + separator + "sf_subtitle=1"
            val label = sub.label.ifBlank { "Phụ đề ${index + 1}" }.replace("\"", "\\\"")
            list.add("""{"id":"track-$index","label":"$label","url":"$taggedUrl"}""")
        }
    }
    if (list.isEmpty() && episode.linkSub.isNotBlank()) {
        val separator = if (episode.linkSub.contains("?")) "&" else "?"
        val taggedUrl = getAbsoluteUrl(episode.linkSub) + separator + "sf_subtitle=1"
        list.add("""{"id":"default","label":"Mặc định (Server)","url":"$taggedUrl"}""")
    }
    return "[" + list.joinToString(",") + "]"
}

private fun generateArtplayerHtml(
    m3u8Url: String,
    title: String,
    hasNext: Boolean,
    poster: String,
    subtitleTracksJson: String,
    savedTime: Float
): String {
    val escapedTitle = title.replace("'", "\\'").replace("\"", "\\\"")
    val escapedPoster = poster.replace("'", "\\'").replace("\"", "\\\"")
    val escapedM3u8Url = m3u8Url.replace("'", "\\'").replace("\"", "\\\"")

    return """
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
      <title>$escapedTitle</title>
      <style>
        @import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@500;600;700;800&display=swap');

        html, body {
          margin: 0; padding: 0; width: 100%; height: 100%;
          background-color: #000; overflow: hidden;
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }
        #artplayer {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;
          box-sizing: border-box;
        }
        .art-video-player {
          background-color: #000 !important;
        }

        .art-fullscreen .art-minibar {
          display: none !important;
        }
        
        .art-controls {
          padding-left: calc(env(safe-area-inset-left, 0px) + 8px) !important;
          padding-right: calc(env(safe-area-inset-right, 0px) + 8px) !important;
        }
        
        .art-control-progress-inner,
        .art-progress-loaded,
        .art-progress-played,
        .art-progress-hover {
          border-radius: 2px !important;
        }

        .art-settings {
          position: absolute !important;
          bottom: calc(env(safe-area-inset-bottom, 0px) + 50px) !important;
          right: calc(env(safe-area-inset-right, 0px) + 8px) !important;
          width: 265px !important;
          min-width: 265px !important;
          max-width: calc(100% - 8px) !important;
          height: auto !important;
          max-height: calc(100% - 60px - env(safe-area-inset-bottom, 0px)) !important;
          padding: 0 !important;
          overflow: hidden !important;
          flex-direction: column !important;
          z-index: 9999 !important;
          border: 1px solid rgba(255, 255, 255, 0.1) !important;
          border-radius: 16px !important;
          background: rgba(10, 10, 16, 0.97) !important;
          box-shadow: rgba(0, 0, 0, 0.75) 0 20px 60px, rgba(0, 0, 0, 0.5) 0 4px 16px !important;
          backdrop-filter: blur(24px) saturate(1.6) !important;
          -webkit-backdrop-filter: blur(24px) saturate(1.6) !important;
          animation: fadeInUp 0.18s ease-out !important;
        }

        .art-settings::before {
          content: "Cài đặt";
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 12px 14px 10px;
          border-bottom: 1px solid rgba(255, 255, 255, 0.07);
          color: rgba(200, 200, 210, 0.85);
          font-size: 11px;
          font-weight: 700;
          letter-spacing: 0.08em;
          text-transform: uppercase;
          cursor: default;
          flex-shrink: 0;
        }

        .art-setting-panel {
          width: 100% !important;
          min-width: 0 !important;
          padding: 4px 0 !important;
          overflow-y: auto !important;
          flex: 1 1 0% !important;
          min-height: 0 !important;
        }

        .art-setting-item {
          display: flex !important;
          align-items: center !important;
          gap: 8px !important;
          min-height: 38px !important;
          height: 38px !important;
          width: 100% !important;
          padding: 0 14px !important;
          border: 0 !important;
          border-radius: 0 !important;
          color: rgba(215, 215, 225, 0.92) !important;
          background: transparent !important;
          cursor: pointer !important;
          user-select: none !important;
          text-shadow: none !important;
          transition: background 0.13s !important;
        }

        .art-setting-item:hover {
          background: rgba(255, 255, 255, 0.07) !important;
        }

        .art-setting-item-left,
        .art-setting-item-right {
          min-width: 0 !important;
          gap: 8px !important;
        }

        .art-setting-item-left {
          flex: 1 1 0% !important;
        }

        .art-setting-item-right {
          flex-shrink: 0 !important;
        }

        .art-setting-item-left-text {
          flex: 1 1 0% !important;
          color: rgba(245, 245, 250, 0.96) !important;
          font-size: 13.5px !important;
          font-weight: 400 !important;
          letter-spacing: 0 !important;
        }

        .art-setting-item-right-tooltip {
          max-width: 92px !important;
          padding-right: 4px !important;
          color: rgba(190, 190, 205, 0.95) !important;
          font-size: 11px !important;
          font-weight: 400 !important;
          overflow: hidden !important;
          text-overflow: ellipsis !important;
          white-space: nowrap !important;
        }

        .art-setting-item-left-icon {
          width: 18px !important;
          height: 18px !important;
          flex-shrink: 0 !important;
          opacity: 1 !important;
        }

        .art-setting-item-left-icon svg {
          width: 18px !important;
          height: 18px !important;
          fill: none !important;
          stroke: rgba(245, 245, 250, 0.95) !important;
        }

        .art-setting-item-right-icon {
          width: 12px !important;
          height: 12px !important;
          flex-shrink: 0 !important;
          opacity: 0.78 !important;
          color: currentColor !important;
        }

        .art-setting-item-right-icon svg {
          width: 12px !important;
          height: 12px !important;
          fill: none !important;
          stroke: currentColor !important;
          stroke-width: 2.5 !important;
          stroke-linecap: round !important;
          stroke-linejoin: round !important;
        }

        .art-settings::-webkit-scrollbar,
        .art-setting-panel::-webkit-scrollbar {
          width: 4px;
        }

        .art-settings::-webkit-scrollbar-thumb,
        .art-setting-panel::-webkit-scrollbar-thumb {
          border-radius: 999px;
          background: rgba(255, 255, 255, 0.18);
        }

        .art-setting-range {
          height: 4px !important;
          border-radius: 999px !important;
          background: rgba(255, 255, 255, 0.18) !important;
        }

        .art-setting-range .art-setting-range-value {
          border-radius: 999px !important;
          background: #1cc749 !important;
        }

        .mobile-center-controls {
          position: absolute;
          top: 50%;
          left: 0;
          width: 100%;
          transform: translateY(-50%);
          display: flex;
          justify-content: center;
          align-items: center;
          gap: 60px;
          z-index: 9999;
          pointer-events: none;
          opacity: 0;
          visibility: hidden;
          transition: opacity 0.3s ease, visibility 0.3s;
        }

        .has-started.art-control-show .mobile-center-controls {
          opacity: 1;
          visibility: visible;
        }

        .has-started .art-state {
          display: none !important;
        }

        .art-settings {
          left: auto !important;
          right: calc(env(safe-area-inset-right, 0px) + 8px) !important;
          bottom: calc(env(safe-area-inset-bottom, 0px) + 50px) !important;
          width: 265px !important;
          min-width: 265px !important;
          max-width: calc(100% - 8px) !important;
          height: auto !important;
          max-height: calc(100% - 60px - env(safe-area-inset-bottom, 0px)) !important;
        }

        .art-setting-panel {
          min-width: 0 !important;
        }

        .art-setting-item {
          min-height: 38px !important;
          height: 38px !important;
        }

        @keyframes fadeInUp {
          from {
            opacity: 0;
            transform: translateY(8px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }

        .mobile-seek-btn {
          width: 45px;
          height: 45px;
          border-radius: 50%;
          background: rgba(0, 0, 0, 0.6);
          backdrop-filter: blur(4px);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          color: rgba(255, 255, 255, 0.9);
          cursor: pointer;
          transition: transform 0.1s;
          pointer-events: auto;
        }

        .mobile-seek-btn:active {
          transform: scale(0.95);
          background: rgba(255, 255, 255, 0.2);
        }

        .sf-subtitle-overlay {
          position: absolute;
          left: 50%;
          bottom: calc(env(safe-area-inset-bottom, 0px) + 10%);
          z-index: 10000;
          width: min(94%, 980px);
          transform: translateX(-50%);
          display: none;
          flex-direction: column;
          align-items: center;
          gap: 3px;
          pointer-events: none;
          text-align: center;
        }

        .sf-subtitle-overlay.visible {
          display: flex;
        }

        .sf-subtitle-line {
          max-width: 100%;
          padding: 2px 10px;
          border-radius: 6px;
          border: 0;
          background: linear-gradient(90deg, transparent 0%, rgba(0, 0, 0, 0.18) 10%, rgba(0, 0, 0, 0.18) 90%, transparent 100%);
          font-family: "Be Vietnam Pro", "Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
          line-height: 1.32;
          white-space: pre-wrap;
          word-break: break-word;
          letter-spacing: 0;
          text-shadow:
            0 1px 0 rgba(0, 0, 0, 0.95),
            1px 0 0 rgba(0, 0, 0, 0.95),
            -1px 0 0 rgba(0, 0, 0, 0.95),
            0 -1px 0 rgba(0, 0, 0, 0.95),
            0 2px 8px rgba(0, 0, 0, 0.95),
            0 4px 18px rgba(0, 0, 0, 0.9);
          -webkit-text-stroke: 0.25px rgba(0, 0, 0, 0.75);
          paint-order: stroke fill;
          box-decoration-break: clone;
          -webkit-box-decoration-break: clone;
        }

        .sf-subtitle-primary {
          color: #fff;
          font-size: clamp(15px, 4.2vw, 24px);
          font-weight: 800;
        }

        .sf-subtitle-secondary {
          color: #ffe96a;
          font-size: clamp(12px, 3.2vw, 18px);
          font-weight: 700;
          opacity: 0.96;
        }

        .art-control-show .sf-subtitle-overlay {
          bottom: calc(env(safe-area-inset-bottom, 0px) + 18%);
        }

        @media (orientation: landscape) {
          .sf-subtitle-overlay {
            bottom: calc(env(safe-area-inset-bottom, 0px) + 8%);
            width: min(86%, 980px);
          }

          .art-control-show .sf-subtitle-overlay {
            bottom: calc(env(safe-area-inset-bottom, 0px) + 17%);
          }

          .sf-subtitle-primary {
            font-size: clamp(16px, 2.7vw, 26px);
          }

          .sf-subtitle-secondary {
            font-size: clamp(13px, 2.1vw, 19px);
          }
        }
      </style>
      <script src="https://cdn.jsdelivr.net/npm/hls.js@1.6.15"></script>
      <script src="https://cdn.jsdelivr.net/npm/artplayer@5.4.0/dist/artplayer.js"></script>
    </head>
    <body>
      <div id="artplayer"></div>
      <script>
        var art = null;
        var hlsInstance = null;
        var subtitleTracks = $subtitleTracksJson;
        var subtitleMode = subtitleTracks.length > 1 ? 'bilingual' : (subtitleTracks.length ? 'on' : 'off');
        var primarySubtitleId = subtitleTracks[0] ? subtitleTracks[0].id : null;
        var secondarySubtitleId = subtitleTracks[1] ? subtitleTracks[1].id : null;
        var primarySubtitleCues = [];
        var secondarySubtitleCues = [];
        var subtitleLoadToken = { primary: 0, secondary: 0 };

        const ICON_SPEED = '<img src="https://sf-static.onflixcdn.pics/images/svg/1773140562_gauge.svg" style="width: 18px; height: 18px; object-fit: contain; flex-shrink: 0; opacity: 0.85;">';
        const ICON_TV = '<img src="https://sf-static.onflixcdn.pics/images/svg/1773140512_tv-minimal-play.svg" style="width: 18px; height: 18px; object-fit: contain; flex-shrink: 0; opacity: 0.85;">';
        const ICON_CC = '<span style="display:inline-flex;align-items:center;justify-content:center;width:18px;height:18px;border:1.8px solid currentColor;border-radius:4px;font-size:9px;font-weight:800;line-height:1;">CC</span>';

        function postMessageToApp(action, data) {
            var msg = JSON.stringify({ action: action, data: data });
            if (window.AndroidApp && typeof window.AndroidApp.postMessage === 'function') {
                window.AndroidApp.postMessage(msg);
            } else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.App) {
                window.webkit.messageHandlers.App.postMessage(msg);
            }
        }

        function decodeHtml(text) {
          var div = document.createElement('div');
          div.innerHTML = text || '';
          return div.textContent || div.innerText || '';
        }

        function parseSubtitleTime(value) {
          var text = String(value || '').trim().replace(',', '.');
          var parts = text.split(':');
          var h = 0, m = 0, s = 0, ms = 0;

          if (parts.length === 3) {
            h = parseInt(parts[0], 10) || 0;
            m = parseInt(parts[1], 10) || 0;
            var sec3 = parts[2].split('.');
            s = parseInt(sec3[0], 10) || 0;
            ms = parseInt((sec3[1] || '0').padEnd(3, '0').slice(0, 3), 10) || 0;
          } else if (parts.length === 2) {
            m = parseInt(parts[0], 10) || 0;
            var sec2 = parts[1].split('.');
            s = parseInt(sec2[0], 10) || 0;
            ms = parseInt((sec2[1] || '0').padEnd(3, '0').slice(0, 3), 10) || 0;
          } else {
            return 0;
          }

          return h * 3600 + m * 60 + s + ms / 1000;
        }

        function parseSubtitle(text) {
          if (!text) return [];
          if (text.charCodeAt(0) === 0xFEFF) {
            text = text.slice(1);
          }
          var trimmed = text.trim();
          if (/^<(?:!doctype|html|script)/i.test(trimmed)) return [];

          var lines = text.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n');
          var cues = [];
          var current = null;

          for (var i = 0; i < lines.length; i++) {
            var raw = lines[i];
            var line = raw.trim();

            if (!line || line.indexOf('WEBVTT') === 0 || line.indexOf('NOTE') === 0 || line.indexOf('STYLE') === 0) {
              if (!line && current) {
                cues.push(current);
                current = null;
              }
              continue;
            }

            if (line.indexOf('-->') >= 0) {
              var parts = line.split('-->');
              var endText = (parts[1] || '').trim().split(/\s+/)[0];
              if (current) cues.push(current);
              current = {
                start: parseSubtitleTime(parts[0]),
                end: parseSubtitleTime(endText),
                textLines: []
              };
            } else if (current) {
              if (current.textLines.length === 0 && /^\d+$/.test(line)) continue;
              current.textLines.push(raw);
            }
          }

          if (current) cues.push(current);

          return cues.map(function (cue) {
            return {
              start: cue.start,
              end: cue.end,
              text: decodeHtml(cue.textLines.join('\n').replace(/<[^>]+>/g, '').trim())
            };
          }).filter(function (cue) {
            return cue.text && cue.end > cue.start;
          });
        }

        function getSubtitleOverlay() {
          return document.getElementById('sf-subtitle-overlay');
        }

        function cleanCompareSubtitle(text) {
          return String(text || '')
            .replace(/<[^>]+>/g, '')
            .toLowerCase()
            .replace(/[\s\r\n\t]+/g, ' ')
            .replace(/[^\wÀ-ỹ\s]/g, '')
            .trim();
        }

        function sameSubtitleText(a, b) {
          if (!a || !b) return false;
          return cleanCompareSubtitle(a) === cleanCompareSubtitle(b);
        }

        function appendSubtitleLine(overlay, text, className) {
          if (!text) return;
          var line = document.createElement('div');
          line.className = 'sf-subtitle-line ' + className;
          line.textContent = text;
          overlay.appendChild(line);
        }

        function findCueAt(cues, time) {
          return (cues || []).find(function (item) {
            return time >= item.start && time <= item.end;
          }) || null;
        }

        // Overlapping cue finder
        function findOverlappingCue(cues, cue) {
          if (!cue) return null;
          return (cues || []).find(function (item) {
            return Math.max(item.start, cue.start) < Math.min(item.end, cue.end);
          }) || null;
        }

        function renderSubtitle() {
          var overlay = getSubtitleOverlay();
          if (!overlay || !art || subtitleMode === 'off') {
            if (overlay) overlay.classList.remove('visible');
            return;
          }

          var time = Number((art.video && art.video.currentTime) || art.currentTime || 0);
          var primaryCue = findCueAt(primarySubtitleCues, time);
          var secondaryCue = null;

          if (subtitleMode === 'bilingual') {
            secondaryCue = findCueAt(secondarySubtitleCues, time);
            if (!primaryCue && secondaryCue) {
              primaryCue = findOverlappingCue(primarySubtitleCues, secondaryCue);
            }
            if (!secondaryCue && primaryCue) {
              secondaryCue = findOverlappingCue(secondarySubtitleCues, primaryCue);
            }
            if (primaryCue && secondaryCue && sameSubtitleText(primaryCue.text, secondaryCue.text)) {
              secondaryCue = null;
            }
          }

          overlay.innerHTML = '';
          appendSubtitleLine(overlay, primaryCue && primaryCue.text, 'sf-subtitle-primary');
          appendSubtitleLine(overlay, secondaryCue && secondaryCue.text, 'sf-subtitle-secondary');

          if (overlay.childNodes.length) {
            overlay.classList.add('visible');
          } else {
            overlay.classList.remove('visible');
          }
        }

        async function loadSubtitleTrack(trackId, slot) {
          var targetSlot = slot || 'primary';
          if (targetSlot === 'primary') {
            primarySubtitleId = trackId;
            primarySubtitleCues = [];
          } else {
            secondarySubtitleId = trackId;
            secondarySubtitleCues = [];
          }
          renderSubtitle();

          var token = ++subtitleLoadToken[targetSlot];
          if (!trackId || trackId === 'off') return;

          var track = subtitleTracks.find(function (item) { return item.id === trackId; });
          if (!track || !track.url) return;

          try {
            var response = await fetch(track.url);
            if (!response.ok) throw new Error('HTTP ' + response.status);
            var contentType = response.headers.get('content-type') || '';
            if (contentType.indexOf('text/html') >= 0) throw new Error('Subtitle returned HTML');
            var text = await response.text();
            if (token !== subtitleLoadToken[targetSlot]) return;
            if (targetSlot === 'primary') {
              primarySubtitleCues = parseSubtitle(text);
            } else {
              secondarySubtitleCues = parseSubtitle(text);
            }
            renderSubtitle();
            if (art) art.notice.show = 'Đã tải phụ đề';
          } catch (error) {
            console.warn('Subtitle load error:', error);
            if (token === subtitleLoadToken[targetSlot]) {
              if (targetSlot === 'primary') primarySubtitleCues = [];
              else secondarySubtitleCues = [];
            }
          }
        }

        function trackLabel(trackId) {
          var track = subtitleTracks.find(function (item) { return item.id === trackId; });
          return track ? track.label : 'Chọn';
        }

        function trackSelector(selectedId, includeOff) {
          var items = includeOff ? [{ value: 'off', html: 'Tắt', default: selectedId === 'off' }] : [];
          return items.concat(subtitleTracks.map(function (track) {
            return {
              value: track.id,
              html: track.label,
              default: selectedId === track.id
            };
          }));
        }

        function subtitleSettingItem() {
          if (!subtitleTracks.length) return null;
          return {
            width: 290,
            name: 'subtitle',
            html: 'Phụ đề',
            icon: ICON_CC,
            tooltip: subtitleMode === 'off' ? 'Tắt' : (subtitleMode === 'bilingual' ? 'Song ngữ' : trackLabel(primarySubtitleId)),
            selector: [
              {
                width: 290,
                name: 'subtitle-mode',
                html: 'Chế độ',
                tooltip: subtitleMode === 'off' ? 'Tắt' : (subtitleMode === 'bilingual' ? 'Song ngữ' : 'Bật'),
                selector: [
                  { value: 'off', html: 'Tắt', default: subtitleMode === 'off' },
                  { value: 'on', html: 'Bật', default: subtitleMode === 'on' },
                  { value: 'bilingual', html: 'Song ngữ', default: subtitleMode === 'bilingual' }
                ],
                onSelect: function (item) {
                  subtitleMode = item.value;
                  if (subtitleMode !== 'off' && primarySubtitleId && !primarySubtitleCues.length) {
                    loadSubtitleTrack(primarySubtitleId, 'primary');
                  }
                  if (subtitleMode === 'bilingual' && secondarySubtitleId && !secondarySubtitleCues.length) {
                    loadSubtitleTrack(secondarySubtitleId, 'secondary');
                  }
                  renderSubtitle();
                  return item.html;
                }
              },
              {
                width: 290,
                name: 'subtitle-primary',
                html: 'Phụ đề chính',
                tooltip: trackLabel(primarySubtitleId),
                selector: trackSelector(primarySubtitleId, false),
                onSelect: function (item) {
                  subtitleMode = subtitleMode === 'off' ? 'on' : subtitleMode;
                  loadSubtitleTrack(item.value, 'primary');
                  return item.html;
                }
              },
              {
                width: 290,
                name: 'subtitle-secondary',
                html: 'Phụ đề phụ',
                tooltip: secondarySubtitleId ? trackLabel(secondarySubtitleId) : 'Chọn',
                selector: trackSelector(secondarySubtitleId, false).filter(function (item) {
                  return item.value !== primarySubtitleId || subtitleTracks.length === 1;
                }),
                onSelect: function (item) {
                  subtitleMode = 'bilingual';
                  loadSubtitleTrack(item.value, 'secondary');
                  return item.html;
                }
              }
            ]
          };
        }

        // player settings
        function playerSettings() {
          var settings = [
            {
              width: 265,
              name: 'playback-rate',
              html: 'Tốc độ phát',
              icon: ICON_SPEED,
              tooltip: 'Bình thường',
              selector: [
                { value: 0.5, name: 'playback-rate-0.5', html: '0.5x' },
                { value: 0.75, name: 'playback-rate-0.75', html: '0.75x' },
                { value: 1, name: 'playback-rate-1', html: 'Bình thường', default: true },
                { value: 1.25, name: 'playback-rate-1.25', html: '1.25x' },
                { value: 1.5, name: 'playback-rate-1.5', html: '1.5x' },
                { value: 2, name: 'playback-rate-2', html: '2x' },
              ],
              onSelect(item) {
                this.playbackRate = Number(item.value);
                return item.html;
              }
            },
            {
              width: 265,
              name: 'aspect-ratio',
              html: 'Tỉ lệ khung hình',
              icon: ICON_TV,
              tooltip: 'Mặc định',
              selector: [
                { value: 'default', name: 'aspect-ratio-default', html: 'Mặc định', default: true },
                { value: '4:3', name: 'aspect-ratio-4:3', html: '4:3' },
                { value: '16:9', name: 'aspect-ratio-16:9', html: '16:9' },
              ],
              onSelect(item) {
                this.aspectRatio = item.value;
                return item.html;
              }
            },
          ];
          var subtitleItem = subtitleSettingItem();
          if (subtitleItem) settings.splice(1, 0, subtitleItem);
          return settings;
        }

        function initPlayer() {
          if (typeof Artplayer === 'undefined' || typeof Hls === 'undefined') {
            setTimeout(initPlayer, 50);
            return;
          }

          art = new Artplayer({
            container: '#artplayer',
            url: '$escapedM3u8Url',
            type: 'm3u8',
            poster: '$escapedPoster',
            autoplay: true,
            muted: false,
            volume: 1,
            isLive: false,
            autoSize: false,
            autoMini: false,
            loop: false,
            flip: false,
            playbackRate: false,
            aspectRatio: false,
            setting: true,
            hotkey: true,
            pip: false,
            mutex: true,
            fullscreen: false,
            fullscreenWeb: true,
            subtitleOffset: false,
            miniProgressBar: false,
            settings: playerSettings(),
            autoPlayback: true,
            theme: '#1cc749',
            cssVar: {
              '--art-settings-max-height': '360px',
              '--art-selector-max-height': '360px',
              '--art-widget-background': 'rgba(15, 15, 18, 0.92)',
              '--art-border-radius': '12px',
            },
            icons: {
              loading: '<img style="pointer-events: none;" src="https://sf-static.onflixcdn.pics/images/gif/1760901453_ploading.gif">',
              state: '<img style="pointer-events: none; width: 150px; height: 150px;" src="https://sf-static.onflixcdn.pics/images/pic/1760900790_state.svg">',
              indicator: '<img style="pointer-events: none; width: 16px; height: 16px;" src="https://sf-static.onflixcdn.pics/images/gif/1760901512_indicator.svg">',
              play: '<img style="pointer-events: none; width: 40px; height: 40px;" class="sm:w-16 sm:h-16" src="https://sf-static.onflixcdn.pics/images/svg/1760902371_play-circle-svgrepo-com.svg">',
              pause: '<img style="pointer-events: none; width: 40px; height: 40px;" class="sm:w-16 sm:h-16" src="https://sf-static.onflixcdn.pics/images/svg/1760902631_pause-circle-svgrepo-com.svg">',
              setting: '<img src="https://sf-static.onflixcdn.pics/images/svg/1773141205_setting_flix.svg?v=2" style="width:24px;height:24px;object-fit:contain;pointer-events:none;">',
            },
            customType: {
              m3u8: function (video, url, artInstance) {
                if (Hls.isSupported()) {
                  if (hlsInstance) hlsInstance.destroy();

                  class CustomLoader extends Hls.DefaultConfig.loader {
                    constructor(config) {
                      super(config);
                      const load = this.load.bind(this);
                      this.load = function (context, config, callbacks) {
                        const onSuccess = callbacks.onSuccess;
                        callbacks.onSuccess = function (response, stats, context) {
                          if (response.data && typeof response.data === 'string' && response.data.includes('#EXTM3U')) {
                            const lines = response.data.split(/\r?\n/);
                            let newLines = [];
                            let buffer = [];

                            for (let i = 0; i < lines.length; i++) {
                              const line = lines[i];
                              const trimLine = line.trim();
                              if (!trimLine) continue;

                              if (trimLine.startsWith('#EXTINF')) {
                                buffer.push(line);
                              } else if (trimLine.startsWith('#')) {
                                if (buffer.length > 0) {
                                  buffer.push(line);
                                } else {
                                  newLines.push(line);
                                }
                              } else {
                                if (trimLine.includes('segment_') && trimLine.includes('.ts')) {
                                  buffer = [];
                                } else {
                                  newLines.push(...buffer);
                                  newLines.push(line);
                                  buffer = [];
                                }
                              }
                            }
                            response.data = newLines.join(String.fromCharCode(10));
                          }
                          onSuccess(response, stats, context);
                        };
                        load(context, config, callbacks);
                      };
                    }
                  }

                  hlsInstance = new Hls({
                    enableWorker: true,
                    maxBufferLength: 30,
                    pLoader: CustomLoader
                  });
                  hlsInstance.loadSource(url);
                  hlsInstance.attachMedia(video);

                  hlsInstance.on(Hls.Events.MANIFEST_PARSED, () => {
                    video.play().catch((e) => console.warn('Browser prevented autoplay:', e));
                  });

                  hlsInstance.on(Hls.Events.ERROR, (_, data) => {
                    if (data.fatal) {
                      switch (data.type) {
                        case Hls.ErrorTypes.NETWORK_ERROR:
                          hlsInstance.startLoad();
                          break;
                        case Hls.ErrorTypes.MEDIA_ERROR:
                          hlsInstance.recoverMediaError();
                          break;
                        default:
                          hlsInstance.destroy();
                          break;
                      }
                    }
                  });

                  artInstance.hls = hlsInstance;
                  artInstance.once('destroy', () => hlsInstance.destroy());
                } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                  video.src = url;
                }
              }
            },
            controls: [
              {
                position: 'left',
                index: 11,
                html: '<div style="display: flex; align-items: center; justify-content: center; width: 30px; height: 30px;"><img style="pointer-events: none; width: 20px; height: 20px; object-fit: contain;" src="https://sf-static.onflixcdn.pics/images/svg/1756189000_-10.svg"></div>',
                tooltip: 'Backward 10s',
                click: function () {
                  art.backward = 10;
                },
              },
              {
                position: 'left',
                index: 12,
                html: '<div style="display: flex; align-items: center; justify-content: center; width: 30px; height: 30px;"><img style="pointer-events: none; width: 20px; height: 20px; object-fit: contain;" src="https://sf-static.onflixcdn.pics/images/svg/1756189026_+10.svg"></div>',
                tooltip: 'Forward 10s',
                click: function () {
                  art.forward = 10;
                },
              },
              {
                position: 'right',
                index: 15,
                name: 'next',
                html: '<div style="display: flex; align-items: center; justify-content: center; width: 30px; height: 30px;"><img src="https://sf-static.onflixcdn.pics/images/svg/1772478616_next-svgrepo-com_flix.svg" style="width: 22px; height: 22px;"></div>',
                tooltip: 'Tập tiếp theo',
                style: {
                  display: ${if (hasNext) "'flex'" else "'none'"},
                },
                click: function () {
                  postMessageToApp('playNextEpisode');
                },
              }
            ],
            layers: [
              {
                html: '<div id="sf-subtitle-overlay" class="sf-subtitle-overlay"></div>',
                name: 'sofaFlixSubtitles',
              },
              {
                html: `<div class="mobile-center-controls">
                        <div class="mobile-seek-btn" id="mobile-rewind">
                            <img class="w-6 h-6" src="https://sf-static.onflixcdn.pics/images/svg/1756189000_-10.svg">
                        </div>
                        <div class="mobile-seek-btn" id="mobile-playpause" style="width: 55px; height: 55px;">
                            <img id="mobile-playpause-icon" class="w-8 h-8" src="https://sf-static.onflixcdn.pics/images/svg/1760902631_pause-circle-svgrepo-com.svg">
                        </div>
                        <div class="mobile-seek-btn" id="mobile-forward">
                            <img class="w-6 h-6" src="https://sf-static.onflixcdn.pics/images/svg/1756189026_+10.svg">
                        </div>
                    </div>`,
                name: 'mobileControls',
              }
            ]
          });

          art.on('ready', () => {
            var savedTime = $savedTime;
            if (savedTime > 0) {
              art.currentTime = savedTime;
            }
            if (subtitleMode !== 'off' && primarySubtitleId) {
              loadSubtitleTrack(primarySubtitleId, 'primary');
            }
            if (subtitleMode === 'bilingual' && secondarySubtitleId) {
              loadSubtitleTrack(secondarySubtitleId, 'secondary');
            }

            const rewindBtn = art.template.player.querySelector('#mobile-rewind');
            const forwardBtn = art.template.player.querySelector('#mobile-forward');
            const playPauseBtn = art.template.player.querySelector('#mobile-playpause');
            const playPauseIcon = art.template.player.querySelector('#mobile-playpause-icon');

            if (rewindBtn) rewindBtn.onclick = (e) => { e.stopPropagation(); art.backward = 10; };
            if (forwardBtn) forwardBtn.onclick = (e) => { e.stopPropagation(); art.forward = 10; };
            if (playPauseBtn) playPauseBtn.onclick = (e) => { e.stopPropagation(); art.toggle(); };

            art.on('play', () => {
              if (playPauseIcon) playPauseIcon.src = 'https://sf-static.onflixcdn.pics/images/svg/1760902631_pause-circle-svgrepo-com.svg';
            });
            art.on('pause', () => {
              if (playPauseIcon) playPauseIcon.src = 'https://sf-static.onflixcdn.pics/images/svg/1760902371_play-circle-svgrepo-com.svg';
            });

            art.once('play', () => {
              if (art.template.player) {
                art.template.player.classList.add('has-started');
              }
            });
          });

          art.on('video:ended', () => {
            postMessageToApp('playNextEpisode');
          });

          art.on('video:timeupdate', () => {
            renderSubtitle();
            if (art.duration) {
              postMessageToApp('updateProgress', { currentTime: art.currentTime, duration: art.duration });
            }
          });

          art.on('fullscreenWeb', (state) => {
            postMessageToApp('toggleFullscreen', { isFullscreen: state });
          });
        }

        window.updatePlayerState = function (hasNext, newSubtitleTracks) {
          subtitleTracks = newSubtitleTracks;
          subtitleMode = subtitleTracks.length > 1 ? 'bilingual' : (subtitleTracks.length ? 'on' : 'off');
          primarySubtitleId = subtitleTracks[0] ? subtitleTracks[0].id : null;
          secondarySubtitleId = subtitleTracks[1] ? subtitleTracks[1].id : null;
          
          if (art) {
            try {
              art.setting.update(playerSettings());
            } catch(e) {}
            
            var nextBtn = art.template.player.querySelector('[name="next"]') || art.template.player.querySelector('.art-control-next');
            if (nextBtn) {
              nextBtn.style.display = hasNext ? 'flex' : 'none';
            }
            
            if (subtitleMode !== 'off' && primarySubtitleId) {
              loadSubtitleTrack(primarySubtitleId, 'primary');
            }
          }
        }

        // Prevent notification bar pull-down from triggering player seek/gestures
        document.addEventListener('touchstart', function(e) {
          if (e.touches && e.touches[0] && e.touches[0].clientY < 65) {
            e.stopPropagation();
          }
        }, { capture: true, passive: true });

        document.addEventListener('touchmove', function(e) {
          if (e.touches && e.touches[0] && e.touches[0].clientY < 65) {
            e.stopPropagation();
          }
        }, { capture: true, passive: true });

        if (document.readyState === 'complete' || document.readyState === 'interactive') {
          initPlayer();
        } else {
          document.addEventListener('DOMContentLoaded', initPlayer);
        }
      </script>
    </body>
    </html>
    """.trimIndent()
}
