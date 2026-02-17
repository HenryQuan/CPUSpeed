package com.yihengquan.cpuspeed

import android.util.Log
import java.io.*

/**
 * Root command executor with enhanced error handling and logging
 */
class RootManager {
    companion object {
        private const val TAG = "RootManager"
        
        private val ROOT_PATHS = arrayOf(
            "/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
            "/data/local/bin/", "/system/sd/xbin/",
            "/system/bin/failsafe/", "/data/local/"
        )
        
        private val ROOT_BINARIES = arrayOf("su", "busybox", "magisk")
    }
    
    /**
     * Check if device has root access
     */
    fun isRootAvailable(): Boolean {
        for (binary in ROOT_BINARIES) {
            if (findBinary(binary)) {
                Log.d(TAG, "Found root binary: $binary")
                return true
            }
        }
        Log.w(TAG, "No root binaries found")
        return false
    }
    
    /**
     * Execute a single command with root privileges
     */
    fun executeCommand(command: String): Result<String> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                Log.e(TAG, "Command failed with exit code $exitCode: $error")
                Result.failure(Exception("Command failed: $error"))
            } else {
                Log.d(TAG, "Command executed successfully: ${command.take(50)}...")
                Result.success(output)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: $command", e)
            Result.failure(e)
        }
    }
    
    /**
     * Execute multiple commands in a single root session
     */
    fun executeCommands(commands: List<String>): Result<Unit> {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val writer = DataOutputStream(process.outputStream)
            
            commands.forEach { command ->
                writer.writeBytes("$command\n")
                writer.flush()
            }
            
            writer.writeBytes("exit\n")
            writer.flush()
            writer.close()
            
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().use { it.readText() }
                Log.e(TAG, "Commands failed with exit code $exitCode: $error")
                Result.failure(Exception("Commands failed: $error"))
            } else {
                Log.d(TAG, "All commands executed successfully (${commands.size} commands)")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing commands", e)
            Result.failure(e)
        }
    }
    
    /**
     * Read file content with root privileges
     */
    fun readFile(path: String): Result<String> {
        return executeCommand("cat $path")
    }
    
    /**
     * Write content to file with root privileges
     */
    fun writeFile(path: String, content: String): Result<Unit> {
        val escapedContent = content.replace("'", "'\\''")
        return executeCommand("echo '$escapedContent' > $path").map { }
    }
    
    /**
     * Check if file exists with root privileges
     */
    fun fileExists(path: String): Boolean {
        return executeCommand("[ -f $path ] && echo 'exists' || echo 'not found'")
            .getOrNull()?.trim() == "exists"
    }
    
    /**
     * Set file permissions with root privileges
     */
    fun setPermissions(path: String, permissions: String): Result<Unit> {
        return executeCommand("chmod $permissions $path").map { }
    }
    
    private fun findBinary(binaryName: String): Boolean {
        for (path in ROOT_PATHS) {
            if (File(path + binaryName).exists()) {
                return true
            }
        }
        return false
    }
}
