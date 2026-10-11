package com.voxd31.editor

data class MinecraftWorldBlock(val x: Int, val y: Int, val z: Int, val materialId: String)

data class MinecraftWorldChunk(val x: Int, val z: Int, val blocks: List<MinecraftWorldBlock>)

interface MinecraftConnectionListener {
    fun connected(serverAddress: String)
    fun playerPosition(x: Double, y: Double, z: Double)
    fun chunkReceived(chunk: MinecraftWorldChunk)
    fun blockUpdated(block: MinecraftWorldBlock)
    fun message(text: String)
    fun disconnected(message: String)
}

interface MinecraftConnectionService {
    fun connect(address: String, username: String, listener: MinecraftConnectionListener)
    fun updateViewPosition(x: Double, y: Double, z: Double)
    fun sendCommand(command: String)
    fun close()
}
