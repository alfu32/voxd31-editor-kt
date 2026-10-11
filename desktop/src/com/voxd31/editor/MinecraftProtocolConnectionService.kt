package com.voxd31.editor

import com.voxd31.editor.MinecraftConnectionListener
import com.voxd31.editor.MinecraftConnectionService
import com.voxd31.editor.MinecraftWorldBlock
import com.voxd31.editor.MinecraftWorldChunk
import io.netty.buffer.Unpooled
import org.geysermc.mcprotocollib.network.ClientSession
import org.geysermc.mcprotocollib.network.Session
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundChunkBatchFinishedPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundForgetLevelChunkPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetChunkCacheCenterPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundChunkBatchReceivedPacket
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.HandPreference
import org.geysermc.mcprotocollib.protocol.data.game.setting.ChatVisibility
import org.geysermc.mcprotocollib.protocol.data.game.setting.ParticleStatus
import org.geysermc.mcprotocollib.protocol.data.game.setting.SkinPart
import kotlin.math.floor
import java.util.concurrent.atomic.AtomicReference

class MinecraftProtocolConnectionService : MinecraftConnectionService {
    private val activeSession = AtomicReference<ClientSession?>()
    @Volatile private var listener: MinecraftConnectionListener? = null
    @Volatile private var loggedIn = false
    @Volatile private var centerX = 0
    @Volatile private var centerY = 64.0
    @Volatile private var centerZ = 0
    @Volatile private var minSectionY = -4
    @Volatile private var sectionCount = 24
    @Volatile private var lastRequestedChunkX = Int.MIN_VALUE
    @Volatile private var lastRequestedChunkZ = Int.MIN_VALUE

    override fun connect(address: String, username: String, listener: MinecraftConnectionListener) {
        close()
        this.listener = listener
        loggedIn = false
        val (host, port) = parseAddress(address)
        val session = ClientNetworkSessionFactory.factory()
            .setAddress(host, port)
            .setProtocol(MinecraftProtocol(username))
            .create()
        activeSession.set(session)
        session.addListener(object : SessionAdapter() {
            override fun packetReceived(session: Session, packet: org.geysermc.mcprotocollib.network.packet.Packet) {
                when (packet) {
                    is ClientboundLoginPacket -> {
                        updateDimension(packet.commonPlayerSpawnInfo.worldName.toString())
                        loggedIn = true
                        listener.connected("$host:$port")
                        session.send(
                            ServerboundClientInformationPacket(
                                "en_us",
                                SERVER_RENDER_DISTANCE,
                                ChatVisibility.FULL,
                                true,
                                SkinPart.VALUES.toList(),
                                HandPreference.RIGHT_HAND,
                                false,
                                true,
                                ParticleStatus.MINIMAL
                            )
                        )
                        session.send(ServerboundPlayerLoadedPacket.INSTANCE)
                    }
                    is ClientboundRespawnPacket -> updateDimension(packet.commonPlayerSpawnInfo.worldName.toString())
                    is ClientboundPlayerPositionPacket -> {
                        val position = packet.position
                        session.send(ServerboundAcceptTeleportationPacket(
                            packet.id,
                            position.x,
                            position.y,
                            position.z,
                            packet.yRot,
                            packet.xRot
                        ))
                        session.send(ServerboundMovePlayerPosPacket(true, false, position.x, position.y, position.z))
                        centerY = position.y
                        lastRequestedChunkX = floor(position.x).toInt() shr 4
                        lastRequestedChunkZ = floor(position.z).toInt() shr 4
                        listener.playerPosition(position.x, position.y, position.z)
                    }
                    is ClientboundSetChunkCacheCenterPacket -> {
                        centerX = packet.chunkX
                        centerZ = packet.chunkZ
                    }
                    is ClientboundLevelChunkWithLightPacket -> {
                        if (kotlin.math.abs(packet.x - centerX) <= MAX_CHUNK_RADIUS && kotlin.math.abs(packet.z - centerZ) <= MAX_CHUNK_RADIUS) {
                            listener.chunkReceived(decodeChunk(packet))
                        }
                    }
                    is ClientboundForgetLevelChunkPacket -> listener.chunkReceived(MinecraftWorldChunk(packet.x, packet.z, emptyList()))
                    is ClientboundBlockUpdatePacket -> {
                        val entry = packet.entry
                        listener.blockUpdated(MinecraftWorldBlock(
                            entry.position.x,
                            entry.position.y,
                            entry.position.z,
                            if (entry.block == 0) "minecraft:air" else MinecraftMaterials.default.id
                        ))
                    }
                    is ClientboundChunkBatchFinishedPacket -> session.send(ServerboundChunkBatchReceivedPacket(8f))
                    is ClientboundSystemChatPacket -> listener.message(packet.content.toString())
                }
            }

            override fun disconnected(event: DisconnectedEvent) {
                val reason = event.reason.toString().ifBlank { event.cause?.message ?: "Connection closed." }
                listener.disconnected(reason)
            }
        })
        try {
            session.connect()
        } catch (t: Throwable) {
            activeSession.compareAndSet(session, null)
            throw IllegalStateException(t.message ?: t.javaClass.simpleName, t)
        }
    }

    override fun sendCommand(command: String) {
        val session = activeSession.get() ?: return
        if (loggedIn && session.isConnected) {
            session.send(ServerboundChatCommandPacket(command.removePrefix("/")))
        }
    }

    override fun updateViewPosition(x: Double, y: Double, z: Double) {
        val session = activeSession.get() ?: return
        if (!loggedIn || !session.isConnected) return
        val chunkX = Math.floorDiv(floor(x).toInt(), 16)
        val chunkZ = Math.floorDiv(floor(z).toInt(), 16)
        if (chunkX == lastRequestedChunkX && chunkZ == lastRequestedChunkZ) return
        lastRequestedChunkX = chunkX
        lastRequestedChunkZ = chunkZ
        centerY = y
        session.send(ServerboundMovePlayerPosPacket(false, false, x, y, z))
    }

    override fun close() {
        loggedIn = false
        activeSession.getAndSet(null)?.disconnect("Disconnected by Voxcraft")
    }

    private fun decodeChunk(packet: ClientboundLevelChunkWithLightPacket): MinecraftWorldChunk {
        val input = Unpooled.wrappedBuffer(packet.chunkData)
        val blocks = mutableListOf<MinecraftWorldBlock>()
        try {
            repeat(sectionCount) { sectionIndex ->
                val section: ChunkSection = MinecraftTypes.readChunkSection(
                    input,
                    BLOCK_STATE_REGISTRY_SIZE,
                    BIOME_REGISTRY_SIZE
                )
                val sectionY = minSectionY + sectionIndex
                val sectionBottom = sectionY * 16
                if (sectionBottom + 15 < centerY - VERTICAL_BLOCK_RADIUS ||
                    sectionBottom > centerY + VERTICAL_BLOCK_RADIUS
                ) {
                    return@repeat
                }
                for (y in 0 until 16) {
                    for (z in 0 until 16) {
                        for (x in 0 until 16) {
                            val blockY = sectionBottom + y
                            if (kotlin.math.abs(blockY - centerY) <= VERTICAL_BLOCK_RADIUS && section.getBlock(x, y, z) != 0) {
                                blocks += MinecraftWorldBlock(
                                    packet.x * 16 + x,
                                    blockY,
                                    packet.z * 16 + z,
                                    MinecraftMaterials.default.id
                                )
                            }
                        }
                    }
                }
            }
        } finally {
            input.release()
        }
        return MinecraftWorldChunk(packet.x, packet.z, blocks)
    }

    private fun parseAddress(address: String): Pair<String, Int> {
        val value = address.trim()
        require(value.isNotEmpty()) { "Enter a server hostname or IP address." }
        if (value.startsWith("[")) {
            val close = value.indexOf(']')
            require(close > 1) { "Invalid IPv6 address." }
            val host = normalizeHost(value.substring(1, close))
            val port = value.substring(close + 1).removePrefix(":").takeIf { it.isNotBlank() }?.toInt() ?: DEFAULT_PORT
            require(port in 1..65535) { "Port must be between 1 and 65535." }
            return host to port
        }
        val separator = value.lastIndexOf(':')
        if (separator > 0 && value.indexOf(':') == separator) {
            val host = normalizeHost(value.substring(0, separator))
            val port = value.substring(separator + 1).toIntOrNull() ?: throw IllegalArgumentException("Invalid server port.")
            require(port in 1..65535) { "Port must be between 1 and 65535." }
            return host to port
        }
        return normalizeHost(value) to DEFAULT_PORT
    }

    private fun normalizeHost(host: String): String = when (host) {
        "0.0.0.0" -> "127.0.0.1"
        "::" -> "::1"
        else -> host
    }

    private fun updateDimension(worldName: String) {
        if (worldName == "minecraft:overworld") {
            minSectionY = -4
            sectionCount = 24
        } else {
            minSectionY = 0
            sectionCount = 16
        }
    }

    companion object {
        private const val DEFAULT_PORT = 25565
        private const val BLOCK_STATE_REGISTRY_SIZE = 32768
        private const val BIOME_REGISTRY_SIZE = 64
        private const val SERVER_RENDER_DISTANCE = 2
        private const val MAX_CHUNK_RADIUS = 1
        private const val VERTICAL_BLOCK_RADIUS = 16
    }
}
