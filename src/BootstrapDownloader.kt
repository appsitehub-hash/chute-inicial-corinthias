// BootstrapDownloader.kt
// Simple Kotlin bootstrapper to download a Windows installer and run it with elevation.
// Intended to be packaged into an exe via jpackage on Windows.

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

fun main(args: Array<String>) {
    val url = args.getOrNull(0) ?: "REPLACE_WITH_INSTALLER_URL"
    if (url.startsWith("REPLACE_WITH")) {
        println("Bootstrap downloader: no installer URL provided. Run the exe with the installer URL as first argument.")
        println("Example: BootstrapDownloader.exe https://example.com/AppCorinthians-setup.exe")
        return
    }

    if (!isWindows()) {
        println("This bootstrapper is intended to run on Windows.")
        println("Installer URL: $url")
        return
    }

    try {
        println("Downloading installer from: $url")
        val tmpDir = Files.createTempDirectory("appcorinthians_boot_")
        val installerFile = tmpDir.resolve("AppCorinthians-setup.exe").toFile()
        downloadToFile(url, installerFile)
        println("Downloaded to: ${installerFile.absolutePath}")

        println("Running installer (will prompt for elevation).")
        val exit = runInstallerWithPowerShell(installerFile)
        println("Installer process exited with code: $exit")

        println("Attempting to create shortcuts (desktop + Start Menu)")
        createShortcutsViaPowerShell()

        println("Done.")
    } catch (e: Exception) {
        System.err.println("Bootstrap failed: ${e.message}")
        e.printStackTrace()
        kotlin.system.exitProcess(1)
    }
}

fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")

fun downloadToFile(urlString: String, outFile: File) {
    URL(urlString).openStream().use { input ->
        Files.copy(input, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}

fun runInstallerWithPowerShell(installer: File): Int {
    // Use PowerShell to start installer with elevation and wait
    val psCmd = listOf(
        "powershell.exe",
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-Command",
        "Start-Process -FilePath '${installer.absolutePath.replace("'", "''")}' -Verb RunAs -Wait"
    )
    val pb = ProcessBuilder(psCmd)
    pb.inheritIO()
    val p = pb.start()
    return p.waitFor()
}

fun createShortcutsViaPowerShell() {
    // Attempt to create Desktop and Start Menu shortcuts pointing to common Program Files location
    // This will not fail the bootstrap if the shortcuts cannot be created.
    val psScript = """
1    ${'$'}w = New-Object -ComObject WScript.Shell
    try {
      ${'$'}desktop = [Environment]::GetFolderPath('Desktop')
      ${'$'}s = ${'$'}w.CreateShortcut((Join-Path ${'$'}desktop 'AppCorinthians.lnk'))
      ${'$'}s.TargetPath = 'C:\\Program Files\\AppCorinthians\\AppCorinthians.exe'
      ${'$'}s.IconLocation = 'C:\\Program Files\\AppCorinthians\\Corinthians-Simbolo-Png.ico'
      ${'$'}s.Save()
    } catch {}

    try {
      ${'$'}startPath = Join-Path ${'$'}env:ProgramData 'Microsoft\\Windows\\Start Menu\\Programs'
      ${'$'}s2 = ${'$'}w.CreateShortcut((Join-Path ${'$'}startPath 'AppCorinthians.lnk'))
      ${'$'}s2.TargetPath = 'C:\\Program Files\\AppCorinthians\\AppCorinthians.exe'
      ${'$'}s2.IconLocation = 'C:\\Program Files\\AppCorinthians\\Corinthians-Simbolo-Png.ico'
      ${'$'}s2.Save()
    } catch {}
    """.trimIndent()

    val tmp = Files.createTempFile("create_shortcuts", ".ps1")
    Files.writeString(tmp, psScript)
    val psCmd = listOf("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", tmp.toAbsolutePath().toString())
    try {
        val p = ProcessBuilder(psCmd).inheritIO().start()
        p.waitFor()
    } catch (e: Exception) {
        // ignore - best effort
        println("Failed to run PowerShell to create shortcuts: ${'$'}{e.message}")
    } finally {
        try { Files.deleteIfExists(tmp) } catch (_: Exception) {}
    }
}
