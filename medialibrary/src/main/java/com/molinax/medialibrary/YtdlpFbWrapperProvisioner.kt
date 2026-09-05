package com.molinax.medialibrary

import java.io.File

/**
 * Menyediakan wrapper 'yt-dlp-fb' -- pengganti binary yt-dlp khusus untuk
 * domain Facebook, yang meng-override resolusi DNS lewat DNS-over-HTTPS
 * (Cloudflare 1.1.1.1) supaya tidak kena filter DNS plain UDP:53 di
 * jaringan ISP. Transparan untuk domain lain (YouTube, TikTok, dll),
 * karena hanya mengintervensi hostname yang cocok suffix Facebook.
 *
 * Source Python-nya disimpan di sini (bukan sebagai file terpisah di
 * $PREFIX) supaya ikut ter-commit ke repo dan otomatis tersedia lagi
 * kalau app di-reinstall / pindah device.
 */
object YtdlpFbWrapperProvisioner {

    private const val TARGET_PATH = "/data/data/com.termux/files/usr/bin/yt-dlp-fb"

    private val SCRIPT_CONTENT = buildString {
        appendLine("#!/data/data/com.termux/files/usr/bin/python3")
        appendLine("# Wrapper yt-dlp: override resolusi DNS domain Facebook lewat DoH")
        appendLine("# (Cloudflare 1.1.1.1), supaya tidak kena filter DNS plain UDP:53 ISP.")
        appendLine("# Auto-generated oleh YtdlpFbWrapperProvisioner -- jangan diedit manual,")
        appendLine("# perubahan akan hilang saat provisioner jalan ulang.")
        appendLine("import socket")
        appendLine("import json")
        appendLine("import urllib.request")
        appendLine("import sys")
        appendLine()
        appendLine("_orig_getaddrinfo = socket.getaddrinfo")
        appendLine("_DOH_SUFFIXES = (\"facebook.com\", \"fbcdn.net\", \"fbsbx.com\", \"fb.watch\")")
        appendLine("_cache = {}")
        appendLine()
        appendLine()
        appendLine("def _is_target(host):")
        appendLine("    if not host:")
        appendLine("        return False")
        appendLine("    return any(host == s or host.endswith(\".\" + s) for s in _DOH_SUFFIXES)")
        appendLine()
        appendLine()
        appendLine("def _doh_resolve(host):")
        appendLine("    if host in _cache:")
        appendLine("        return _cache[host]")
        appendLine("    url = \"https://1.1.1.1/dns-query?name=%s&type=A\" % host")
        appendLine("    req = urllib.request.Request(url, headers={\"accept\": \"application/dns-json\"})")
        appendLine("    try:")
        appendLine("        with urllib.request.urlopen(req, timeout=5) as resp:")
        appendLine("            data = json.loads(resp.read())")
        appendLine("        ips = [a[\"data\"] for a in data.get(\"Answer\", []) if a.get(\"type\") == 1]")
        appendLine("        if ips:")
        appendLine("            _cache[host] = ips[0]")
        appendLine("            return ips[0]")
        appendLine("    except Exception as e:")
        appendLine("        print(\"DoH resolve gagal untuk %s: %s\" % (host, e), file=sys.stderr)")
        appendLine("    return None")
        appendLine()
        appendLine()
        appendLine("def patched_getaddrinfo(host, *args, **kwargs):")
        appendLine("    if _is_target(host):")
        appendLine("        ip = _doh_resolve(host)")
        appendLine("        if ip:")
        appendLine("            print(\"[doh-wrapper] %s -> %s\" % (host, ip), file=sys.stderr)")
        appendLine("            return _orig_getaddrinfo(ip, *args, **kwargs)")
        appendLine("    return _orig_getaddrinfo(host, *args, **kwargs)")
        appendLine()
        appendLine()
        appendLine("socket.getaddrinfo = patched_getaddrinfo")
        appendLine()
        appendLine("from yt_dlp import main")
        appendLine()
        appendLine("if __name__ == \"__main__\":")
        appendLine("    main()")
    }

    /**
     * Pastikan wrapper ada di [TARGET_PATH] dan up-to-date & executable.
     * Aman dipanggil berkali-kali (idempotent) -- hanya menulis ulang
     * kalau file belum ada atau isinya beda dari SCRIPT_CONTENT saat ini.
     * Return path yang siap dipakai sebagai argumen pertama ProcessBuilder.
     */
    @Synchronized
    fun ensureInstalled(): String {
        val file = File(TARGET_PATH)
        val needsWrite = !file.exists() || file.readText() != SCRIPT_CONTENT
        if (needsWrite) {
            file.parentFile?.mkdirs()
            file.writeText(SCRIPT_CONTENT)
            file.setExecutable(true, false)
            file.setReadable(true, false)
        }
        return TARGET_PATH
    }
}
