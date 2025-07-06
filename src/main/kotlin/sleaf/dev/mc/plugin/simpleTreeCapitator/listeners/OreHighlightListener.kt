package sleaf.dev.mc.plugin.simpleTreeCapitator.listeners

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import org.bukkit.GameMode
import java.util.*

class OreHighlightListener(private val plugin: Plugin) : Listener {

    // Хранит активные задачи подсветки для каждого игрока
    private val highlightingPlayers = mutableMapOf<UUID, BukkitRunnable>()

    // Проверяет, является ли блок рудой
    private fun isOreBlock(material: Material): Boolean {
        return material.name.endsWith("_ORE")
    }

    // Проверяет наличие метки OreBreaker в lore предмета
    private fun hasOreBreakerMeta(item: ItemStack): Boolean {
        if (!item.hasItemMeta()) return false
        val meta = item.itemMeta
        return meta?.lore?.any { it.contains("OreBreaker") } ?: false
    }

    // Создает обводку блока с помощью частиц
    private fun createBlockOutline(player: Player, block: Block) {
        val loc = block.location
        val whiteParticle = Particle.END_ROD
        val redParticle = Particle.REDSTONE

        // Получаем цвет частиц в зависимости от типа руды
        val dustOptions = getOreColor(block.type)

        // Создаем точки для обводки блока
        val edgePoints = mutableListOf<Vector>()
        
        // Нижняя грань (края)
        for (x in 0..1) {
            for (z in 0..1) {
                edgePoints.add(Vector(x.toDouble(), 0.0, z.toDouble()))
            }
        }
        
        // Верхняя грань (края)
        for (x in 0..1) {
            for (z in 0..1) {
                edgePoints.add(Vector(x.toDouble(), 1.0, z.toDouble()))
            }
        }
        
        // Вертикальные края
        for (x in 0..1) {
            for (z in 0..1) {
                for (y in 0..1) {
                    edgePoints.add(Vector(x.toDouble(), y.toDouble(), z.toDouble()))
                }
            }
        }

        // Отображаем частицы для краев
        for (point in edgePoints) {
            val particleLoc = loc.clone().add(point)
            player.spawnParticle(whiteParticle, particleLoc, 1, 0.0, 0.0, 0.0, 0.0)
        }

        // Отображаем частицы для граней
        val facePoints = mutableListOf<Vector>()
        
        // Нижняя грань
        for (i in 1..3) {
            facePoints.add(Vector(0.5, 0.0, i * 0.25)) // Передняя грань
            facePoints.add(Vector(0.5, 0.0, 1.0 - i * 0.25)) // Задняя грань
            facePoints.add(Vector(i * 0.25, 0.0, 0.5)) // Левая грань
            facePoints.add(Vector(1.0 - i * 0.25, 0.0, 0.5)) // Правая грань
        }

        // Верхняя грань
        for (i in 1..3) {
            facePoints.add(Vector(0.5, 1.0, i * 0.25)) // Передняя грань
            facePoints.add(Vector(0.5, 1.0, 1.0 - i * 0.25)) // Задняя грань
            facePoints.add(Vector(i * 0.25, 1.0, 0.5)) // Левая грань
            facePoints.add(Vector(1.0 - i * 0.25, 1.0, 0.5)) // Правая грань
        }

        // Боковые грани
        for (i in 1..3) {
            facePoints.add(Vector(0.0, i * 0.25, 0.5)) // Левая грань
            facePoints.add(Vector(1.0, i * 0.25, 0.5)) // Правая грань
            facePoints.add(Vector(0.5, i * 0.25, 0.0)) // Передняя грань
            facePoints.add(Vector(0.5, i * 0.25, 1.0)) // Задняя грань
        }

        // Отображаем частицы для граней
        for (point in facePoints) {
            val particleLoc = loc.clone().add(point)
            player.spawnParticle(redParticle, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
        }
    }

    // Получает цвет частиц в зависимости от типа руды
    private fun getOreColor(material: Material): org.bukkit.Particle.DustOptions {
        val name = material.name
        return when {
            name.contains("REDSTONE") -> org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.0f)
            name.contains("DIAMOND") -> org.bukkit.Particle.DustOptions(org.bukkit.Color.AQUA, 1.0f)
            name.contains("LAPIS") -> org.bukkit.Particle.DustOptions(org.bukkit.Color.BLUE, 1.0f)
            name.contains("GOLD") -> org.bukkit.Particle.DustOptions(org.bukkit.Color.YELLOW, 1.0f)
            name.contains("IRON") -> org.bukkit.Particle.DustOptions(org.bukkit.Color.GRAY, 1.0f)
            name.contains("COAL") -> org.bukkit.Particle.DustOptions(org.bukkit.Color.BLACK, 1.0f)
            name.contains("EMERALD") -> org.bukkit.Particle.DustOptions(org.bukkit.Color.GREEN, 1.0f)
            name.contains("COPPER") -> org.bukkit.Particle.DustOptions(org.bukkit.Color.ORANGE, 1.0f)
            name.contains("NETHERITE") -> org.bukkit.Particle.DustOptions(org.bukkit.Color.PURPLE, 1.0f)
            name.contains("NETHER_GOLD") -> org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(255, 165, 0), 1.0f) // Оранжево-золотой
            name.contains("QUARTZ") -> org.bukkit.Particle.DustOptions(org.bukkit.Color.WHITE, 1.0f)
            name.contains("ANCIENT_DEBRIS") -> org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(139, 69, 19), 1.0f) // Коричневый
            else -> org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.0f) // По умолчанию красный
        }
    }

    @EventHandler
    fun onBlockDamage(event: BlockDamageEvent) {
        val player = event.player
        val block = event.block
        val tool = player.inventory.itemInMainHand

        if (isOreBlock(block.type) && hasOreBreakerMeta(tool) && player.gameMode == GameMode.SURVIVAL) {
            // Отменяем существующую подсветку для этого игрока
            highlightingPlayers[player.uniqueId]?.cancel()
            
            // Создаем новую задачу подсветки
            val task = object : BukkitRunnable() {
                override fun run() {
                    // Проверяем, что игрок все еще онлайн и выполняет условия
                    if (!player.isOnline || !hasOreBreakerMeta(player.inventory.itemInMainHand) || player.gameMode != GameMode.SURVIVAL) {
                        cancel()
                        highlightingPlayers.remove(player.uniqueId)
                        return
                    }

                    // Находим все связанные блоки руды
                    val connectedBlocks = findConnectedOreBlocks(block, 100)
                    
                    // Подсвечиваем все найденные блоки
                    for (connectedBlock in connectedBlocks) {
                        createBlockOutline(player, connectedBlock)
                    }
                }
            }

            // Запускаем задачу каждые 5 тиков (1/4 секунды)
            task.runTaskTimer(plugin, 0L, 5L)
            highlightingPlayers[player.uniqueId] = task
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        // Отменяем подсветку для игрока, если она активна
        highlightingPlayers[player.uniqueId]?.cancel()
        highlightingPlayers.remove(player.uniqueId)
    }

    // Находит все связанные блоки руды
    private fun findConnectedOreBlocks(startBlock: Block, maxBlocks: Int): Set<Block> {
        val visited = mutableSetOf<Block>()
        val queue = LinkedList<Block>()
        queue.add(startBlock)
        visited.add(startBlock)

        // Определяем тип руды из начального блока
        val oreType = getOreType(startBlock.type)
        if (oreType == null) return visited

        while (queue.isNotEmpty() && visited.size < maxBlocks) {
            val current = queue.poll()
            
            // Добавляем все возможные направления, включая диагонали
            val directions = listOf(
                // Оси
                Triple(1, 0, 0), Triple(-1, 0, 0),
                Triple(0, 1, 0), Triple(0, -1, 0),
                Triple(0, 0, 1), Triple(0, 0, -1),
                // Диагонали в плоскости XY
                Triple(1, 1, 0), Triple(-1, 1, 0),
                Triple(1, -1, 0), Triple(-1, -1, 0),
                // Диагонали в плоскости XZ
                Triple(1, 0, 1), Triple(-1, 0, 1),
                Triple(1, 0, -1), Triple(-1, 0, -1),
                // Диагонали в плоскости YZ
                Triple(0, 1, 1), Triple(0, -1, 1),
                Triple(0, 1, -1), Triple(0, -1, -1),
                // Диагонали в пространстве
                Triple(1, 1, 1), Triple(-1, 1, 1),
                Triple(1, -1, 1), Triple(-1, -1, 1),
                Triple(1, 1, -1), Triple(-1, 1, -1),
                Triple(1, -1, -1), Triple(-1, -1, -1)
            )

            for ((x, y, z) in directions) {
                val adjacent = current.world.getBlockAt(
                    current.x + x,
                    current.y + y,
                    current.z + z
                )

                if (isOreBlock(adjacent.type) && !visited.contains(adjacent)) {
                    // Проверяем, что тип руды совпадает
                    if (getOreType(adjacent.type) == oreType) {
                        visited.add(adjacent)
                        queue.add(adjacent)
                    }
                }
            }
        }

        return visited
    }

    // Получает тип руды из материала
    private fun getOreType(material: Material): String? {
        val name = material.name
        return when {
            name.contains("COAL") -> "COAL"
            name.contains("IRON") -> "IRON"
            name.contains("GOLD") -> "GOLD"
            name.contains("DIAMOND") -> "DIAMOND"
            name.contains("EMERALD") -> "EMERALD"
            name.contains("LAPIS") -> "LAPIS"
            name.contains("REDSTONE") -> "REDSTONE"
            name.contains("COPPER") -> "COPPER"
            name.contains("NETHERITE") -> "NETHERITE"
            name.contains("NETHER_GOLD") -> "NETHER_GOLD"
            name.contains("QUARTZ") -> "QUARTZ"
            name.contains("ANCIENT_DEBRIS") -> "ANCIENT_DEBRIS"
            else -> null
        }
    }
} 